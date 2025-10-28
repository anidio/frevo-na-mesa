// backend/src/main/java/br/com/frevonamesa/frevonamesa/service/FinanceiroService.java

package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.TipoPagamento;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// Imports do Stripe (Corrigidos e Organizados)
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session; // Usar o Session do checkout
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.checkout.SessionCreateParams; // Usaremos SessionCreateParams do checkout
// Import para PaymentMethodOptions (CORRETO para esta versão)
import com.stripe.param.checkout.SessionCreateParams.PaymentMethodOptions;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // Adicionar HashMap para capabilities
import java.util.UUID;
import java.util.Optional; // Adicionar Optional


@Service
public class FinanceiroService {

    // Constantes para limites
    private static final int LIMITE_MESAS_GRATUITO = 10;
    private static final int LIMITE_USUARIOS_GRATUITO = 4;
    private static final int LIMITE_ILIMITADO = Integer.MAX_VALUE;
    private static final int PEDIDOS_POR_PACOTE = 10;

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Lazy // Mantém Lazy para evitar dependência cíclica
    @Autowired
    private PedidoService pedidoService;

    // --- CONFIGURAÇÕES DO STRIPE ---
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins; // Usado para URLs de retorno

    // IDs de Preço (mantidos como antes)
    private final Map<String, String> priceIds = Map.of(
            "PAY_PER_USE", "price_1SKMQ2C1ubUDyMdQIs3JhxLP",
            "DELIVERY_PRO_MENSAL", "price_1SKMPEC1ubUDyMdQQkAPocBf",
            "SALAO_PDV_MENSAL", "price_1SKMPTC1ubUDyMdQyuHgkYrO",
            "PREMIUM_MENSAL", "price_1SKMPmC1ubUDyMdQBnvegHTd",
            "PREMIUM_ANUAL", "price_1SKMQOC1ubUDyMdQHMWKSjMK"
    );
    // --- FIM CONFIGURAÇÕES DO STRIPE ---

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // --- LÓGICA PAY-PER-USE (PARA A PLATAFORMA) ---
    public String gerarUrlPagamento(Long restauranteId) throws StripeException {
        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String successUrl = frontendBaseUrl + "/admin/financeiro?payment=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?payment=cancel";

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("restauranteId", restauranteId.toString())
                        .putMetadata("tipoProduto", "PAY_PER_USE")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(priceIds.get("PAY_PER_USE"))
                                        .setQuantity(1L)
                                        .build())
                        .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public void compensarLimite(Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado ao compensar limite. ID: " + restauranteId));

        boolean aplicaCompensacao = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isDeliveryPro()
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester();

        if (aplicaCompensacao) {
            restaurante.setPedidosMesAtual(Math.max(0, restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE));
            restauranteRepository.save(restaurante);
            System.out.println("INFO: Limite compensado para Restaurante ID: " + restauranteId + ". Pedidos atuais: " + restaurante.getPedidosMesAtual());
        } else {
            System.out.println("INFO: Pagamento de pacote recebido (Restaurante ID: " + restauranteId + "), mas o plano não é GRATUITO ou já é PRO. Nenhuma compensação necessária.");
        }
    }
    // --- FIM LÓGICA PAY-PER-USE ---


    // --- LÓGICA DE ASSINATURA (PARA A PLATAFORMA) ---
    private String gerarUrlUpgradeAssinatura(String planoKey, Long restauranteId) throws StripeException {
        String priceId = priceIds.get(planoKey);
        if (priceId == null) {
            throw new RuntimeException("ID de preço não encontrado para a chave de plano: " + planoKey);
        }
        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String successUrl = frontendBaseUrl + "/admin/financeiro?subscription=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?subscription=cancel";

        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado para gerar URL de upgrade. ID: " + restauranteId));
        String customerEmail = restaurante.getEmail();
        String existingSubscriptionId = restaurante.getStripeSubscriptionId();
        String existingCustomerId = restaurante.getStripeCustomerId();

        System.out.println("DEBUG: Preparando Checkout (Assinatura). CustomerId existente no BD: " + existingCustomerId + ", SubscriptionId existente no BD: " + existingSubscriptionId);

        SessionCreateParams.Builder paramsBuilder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("restauranteId", restauranteId.toString())
                        .putMetadata("planoKey", planoKey)
                        .setCustomer(existingCustomerId != null ? existingCustomerId : null)
                        .setCustomerEmail(existingCustomerId == null ? customerEmail : null)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build());

        if (existingSubscriptionId != null && !existingSubscriptionId.isEmpty()) {
            System.out.println("INFO: Gerando sessão de Checkout para ATUALIZAR assinatura existente: " + existingSubscriptionId);
            throw new RuntimeException("Gerencie sua assinatura existente através do Portal do Cliente.");

        } else {
            System.out.println("INFO: Gerando sessão de Checkout para CRIAR NOVA assinatura para o Customer: " + (existingCustomerId != null ? existingCustomerId : "Novo (email: " + customerEmail + ")"));
        }

        SessionCreateParams finalParams = paramsBuilder.build();
        System.out.println("DEBUG: Parâmetros FINAIS (Assinatura) enviados para Session.create: Customer=" + finalParams.getCustomer() + ", CustomerEmail=" + finalParams.getCustomerEmail());

        Session session = Session.create(finalParams);
        return session.getUrl();
    }

    public String gerarUrlUpgradeDeliveryMensal(Long restauranteId) throws StripeException {
        return gerarUrlUpgradeAssinatura("DELIVERY_PRO_MENSAL", restauranteId);
    }

    public String gerarUrlUpgradeSalaoMensal(Long restauranteId) throws StripeException {
        return gerarUrlUpgradeAssinatura("SALAO_PDV_MENSAL", restauranteId);
    }

    public String gerarUrlUpgradePremiumMensal(Long restauranteId) throws StripeException {
        return gerarUrlUpgradeAssinatura("PREMIUM_MENSAL", restauranteId);
    }

    public String gerarUrlUpgradePremiumAnual(Long restauranteId) throws StripeException {
        return gerarUrlUpgradeAssinatura("PREMIUM_ANUAL", restauranteId);
    }
    // --- FIM LÓGICA DE ASSINATURA ---


    // --- LÓGICA DO PORTAL DO CLIENTE (PARA A PLATAFORMA) ---
    public String criarSessaoPortalCliente() throws StripeException {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        String stripeCustomerId = restaurante.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            System.err.println("ERRO: Tentativa de abrir portal para restaurante sem Stripe Customer ID. Restaurante ID: " + restaurante.getId());
            try {
                Customer customer = Customer.create(Map.of("email", restaurante.getEmail(), "name", restaurante.getNome()));
                stripeCustomerId = customer.getId();
                restaurante.setStripeCustomerId(stripeCustomerId);
                restauranteRepository.save(restaurante);
                System.out.println("INFO: Stripe Customer ID criado dinamicamente: " + stripeCustomerId + " para Restaurante ID: " + restaurante.getId());
            } catch (StripeException e) {
                System.err.println("ERRO CRÍTICO: Falha ao criar Stripe Customer ID para restaurante ID: " + restaurante.getId() + " - " + e.getMessage());
                throw new RuntimeException("Cliente Stripe não encontrado e falha ao criar.");
            }
        }

        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String returnUrl = frontendBaseUrl + "/admin/financeiro";

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(stripeCustomerId)
                        .setReturnUrl(returnUrl)
                        .build();

        com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
        System.out.println("INFO: Sessão do Portal do Cliente criada para Customer ID: " + stripeCustomerId);
        return portalSession.getUrl();
    }
    // --- FIM LÓGICA DO PORTAL DO CLIENTE ---

    // --- >> INÍCIO: NOVOS MÉTODOS STRIPE CONNECT (COM CORREÇÕES) << ---

    @Transactional
    public String createConnectAccountLink(Long restauranteId) throws StripeException {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado para criar link Connect. ID: " + restauranteId));

        String accountId = restaurante.getStripeConnectAccountId();
        Account account;

        if (accountId == null || accountId.isEmpty()) {
            // --- CORREÇÃO 1 APLICADA: Usando put para capabilities ---
            AccountCreateParams.Capabilities capabilities = AccountCreateParams.Capabilities.builder()
                    .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                    .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                    // Adiciona 'pix_payments' como um parâmetro extra com 'requested: true'
                    .putExtraParam("pix_payments", Map.of("requested", true)) // <-- CORREÇÃO AQUI
                    .build();

            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setEmail(restaurante.getEmail())
                    .setCapabilities(capabilities)
                    .build();

            account = Account.create(params);
            accountId = account.getId();
            restaurante.setStripeConnectAccountId(accountId);
            restauranteRepository.save(restaurante);
            System.out.println("INFO: Nova conta Stripe Connect Express criada: " + accountId + " para Restaurante ID: " + restauranteId);
        } else {
            account = Account.retrieve(accountId);
            System.out.println("INFO: Usando conta Stripe Connect Express existente: " + accountId + " para Restaurante ID: " + restauranteId);
        }

        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String returnUrl = frontendBaseUrl + "/admin/financeiro?connect_return=true";
        String refreshUrl = frontendBaseUrl + "/admin/financeiro?connect_refresh=true";

        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .setCollect(AccountLinkCreateParams.Collect.EVENTUALLY_DUE)
                .build();

        AccountLink accountLink = AccountLink.create(params);
        System.out.println("INFO: Link de onboarding gerado para a conta: " + accountId);
        return accountLink.getUrl();
    }

    @Transactional
    public boolean verifyConnectedAccount(Long restauranteId) throws StripeException {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado para verificar conta Connect. ID: " + restauranteId));
        String accountId = restaurante.getStripeConnectAccountId();
        boolean canReceivePayments = false;

        if (accountId != null && !accountId.isEmpty()) {
            Account account = Account.retrieve(accountId);
            canReceivePayments = Boolean.TRUE.equals(account.getChargesEnabled()) && Boolean.TRUE.equals(account.getPayoutsEnabled());
            boolean detailsSubmitted = Boolean.TRUE.equals(account.getDetailsSubmitted());
            System.out.println("INFO: Verificação da conta Connect " + accountId +
                    ": ChargesEnabled=" + account.getChargesEnabled() +
                    ", PayoutsEnabled=" + account.getPayoutsEnabled() +
                    ", DetailsSubmitted=" + detailsSubmitted);
        } else {
            System.out.println("WARN: Tentativa de verificar conta Connect sem ID para Restaurante ID: " + restauranteId);
        }
        return canReceivePayments;
    }

    // --- >> FIM: NOVOS MÉTODOS STRIPE CONNECT << ---


    // --- LÓGICA DE PEDIDO PÚBLICO (MODIFICADA PARA STRIPE CONNECT - COM CORREÇÕES) ---
    public String gerarUrlPagamentoPedidoPublico(UUID uuidPedido, BigDecimal totalPedido, Long restauranteId) throws StripeException {
        System.out.println("--- Iniciando gerarUrlPagamentoPedidoPublico com Stripe Connect ---");
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado para pagamento público. ID: " + restauranteId));

        String connectAccountId = restaurante.getStripeConnectAccountId();

        if (connectAccountId == null || connectAccountId.isEmpty()) {
            System.err.println("❌ ERRO PAGAMENTO: Restaurante ID " + restauranteId + " não possui conta Stripe Connect configurada.");
            throw new RuntimeException("Este restaurante não está configurado para receber pagamentos online no momento.");
        }
        System.out.println("   Usando Conta Connect: " + connectAccountId);

        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String successUrl = frontendBaseUrl + "/cardapio/" + restauranteId + "?payment=success&pedido_uuid=" + uuidPedido.toString();
        String cancelUrl = frontendBaseUrl + "/cardapio/" + restauranteId + "?payment=cancel";

        long amountInCents = totalPedido.multiply(new BigDecimal("100")).longValue();
        if (amountInCents < 50) {
            System.err.println("❌ ERRO PAGAMENTO: Valor do pedido " + totalPedido + " é menor que o mínimo permitido pelo Stripe.");
            throw new RuntimeException("O valor do pedido é muito baixo para processamento online.");
        }

        long applicationFeeAmount = Math.max(1, (long) (amountInCents * 0.05)); // 5%, mínimo 1 centavo
        System.out.println("   Valor total (centavos): " + amountInCents);
        System.out.println("   Taxa da plataforma (centavos): " + applicationFeeAmount);
        if (applicationFeeAmount >= amountInCents) {
            System.err.println("❌ ERRO PAGAMENTO: Taxa da plataforma ("+applicationFeeAmount+") é maior ou igual ao valor total ("+amountInCents+"). Pagamento inviável.");
            throw new RuntimeException("Erro interno na configuração de taxas.");
        }

        // --- CORREÇÃO 2 APLICADA: Configuração do Pix movida para setPaymentMethodOptions ---
        PaymentMethodOptions paymentMethodOptions = PaymentMethodOptions.builder()
                .setPix(PaymentMethodOptions.Pix.builder()
                        .setExpiresAfterSeconds(3600L) // 1 hora
                        .build())
                .build();
        // --- FIM DA CORREÇÃO 2 APLICADA ---


        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PIX)
                .putMetadata("pedidoUuid", uuidPedido.toString())
                .putMetadata("restauranteId", restauranteId.toString())
                .putMetadata("tipoProduto", "PEDIDO_PUBLICO")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("brl")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Pedido " + restaurante.getNome() + " #" + uuidPedido.toString().substring(0, 8))
                                                                .build())
                                                .build())
                                .setQuantity(1L)
                                .build())
                .setPaymentIntentData( // Dados para o PaymentIntent
                        SessionCreateParams.PaymentIntentData.builder()
                                .setTransferData( // Direciona para a conta conectada
                                        SessionCreateParams.PaymentIntentData.TransferData.builder()
                                                .setDestination(connectAccountId)
                                                .build()
                                )
                                .setApplicationFeeAmount(applicationFeeAmount) // Sua taxa
                                // Removido daqui -> .setPaymentMethodOptions(paymentMethodOptions)
                                .build()
                )
                .setPaymentMethodOptions(paymentMethodOptions) // <-- CORREÇÃO 2 APLICADA: Opções do Pix aqui
                .build();

        Session session = Session.create(params);
        System.out.println("✅ Sessão de Checkout criada para pedido público " + uuidPedido + " na conta " + connectAccountId);
        System.out.println("--- Finalizando gerarUrlPagamentoPedidoPublico ---");
        return session.getUrl();
    }
    // --- FIM LÓGICA DE PEDIDO PÚBLICO ---


    // --- LÓGICA DE WEBHOOK ---
    @Transactional
    public void processarWebhookStripe(String payload, String sigHeader) throws StripeException {
        // ... (Validação do webhook - sem alterações) ...
        System.out.println("\n>>> WEBHOOK RECEBIDO! Header Stripe-Signature: " + (sigHeader != null && !sigHeader.isEmpty() ? "Presente" : "AUSENTE ou VAZIO"));

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, this.webhookSecret);
            System.out.println("✅ Webhook Verificado: " + event.getType() + " ID: " + event.getId());
        } catch (SignatureVerificationException e) {
            System.err.println("❌ ERRO WEBHOOK: Assinatura inválida! Verifique o 'webhookSecret'.");
            throw e;
        } catch (Exception e) {
            System.err.println("❌ ERRO WEBHOOK: Falha ao construir evento: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar webhook: " + e.getMessage(), e);
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject dataObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            dataObject = dataObjectDeserializer.getObject().get();
            System.out.println("Webhook INFO: Deserialização automática OK para tipo: " + event.getType());
        } else {
            System.err.println("Webhook AVISO: Desserialização automática falhou para: " + event.getType() + ". Tentando desserialização explícita (unsafe).");
            try {
                dataObject = dataObjectDeserializer.deserializeUnsafe();
                System.out.println("Webhook INFO: Desserialização explícita bem-sucedida para tipo: " + event.getType());
            } catch (Exception e) {
                System.err.println("❌❌ ERRO CRÍTICO NA DESSERIALIZAÇÃO para evento " + event.getType() + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Falha ao desserializar objeto Stripe para evento: " + event.getType(), e);
            }
        }

        if (dataObject == null) {
            System.err.println("❌❌ ERRO CRÍTICO: dataObject NULO após tentativa de desserialização para tipo: " + event.getType());
            throw new RuntimeException("dataObject nulo para evento: " + event.getType());
        }
        System.out.println("Webhook INFO: Objeto de dados obtido com sucesso: " + dataObject.getClass().getName());

        // Processa os eventos específicos
        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    if (dataObject instanceof Session) {
                        Session session = (Session) dataObject;
                        System.out.println("Webhook: Chamando processarCheckoutSession para Session ID: " + session.getId());
                        processarCheckoutSession(session);
                        System.out.println("✅ Webhook: processarCheckoutSession concluído com sucesso para Session ID: " + session.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado checkout.Session para checkout.session.completed, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                case "customer.subscription.deleted":
                    if (dataObject instanceof Subscription) {
                        Subscription subscriptionDeleted = (Subscription) dataObject;
                        System.out.println("Webhook: Chamando processarCancelamentoAssinatura para Subscription ID: " + subscriptionDeleted.getId());
                        processarCancelamentoAssinatura(subscriptionDeleted);
                        System.out.println("✅ Webhook: processarCancelamentoAssinatura concluído com sucesso para Sub ID: " + subscriptionDeleted.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Subscription para customer.subscription.deleted, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                case "customer.subscription.updated":
                    if (dataObject instanceof Subscription) {
                        Subscription subscriptionUpdated = (Subscription) dataObject;
                        System.out.println("Webhook: Processando customer.subscription.updated para Subscription ID: " + subscriptionUpdated.getId());
                        processarAtualizacaoAssinatura(subscriptionUpdated);
                        System.out.println("✅ Webhook: processarAtualizacaoAssinatura concluído com sucesso para Sub ID: " + subscriptionUpdated.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Subscription para customer.subscription.updated, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                case "account.updated":
                    if (dataObject instanceof Account) {
                        Account account = (Account) dataObject;
                        System.out.println("ℹ️ INFO WEBHOOK: Conta Connect atualizada: " + account.getId() +
                                ", ChargesEnabled: " + account.getChargesEnabled() +
                                ", PayoutsEnabled: " + account.getPayoutsEnabled() +
                                ", DetailsSubmitted: " + account.getDetailsSubmitted());
                        // --- CORREÇÃO 2 APLICADA: Usando o método correto do repositório ---
                        Optional<Restaurante> restauranteOpt = restauranteRepository.findByStripeConnectAccountId(account.getId());
                        // --- FIM DA CORREÇÃO 2 APLICADA ---
                        restauranteOpt.ifPresent(restaurante -> {
                            try {
                                System.out.println("   Tentando verificar status interno para Restaurante ID: " + restaurante.getId());
                                verifyConnectedAccount(restaurante.getId());
                            } catch (StripeException e) {
                                System.err.println("   ERRO ao chamar verifyConnectedAccount dentro do webhook account.updated: " + e.getMessage());
                            }
                        });
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Account para account.updated, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                default:
                    System.out.println("ℹ️ INFO WEBHOOK: Evento não tratado recebido: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            System.err.println("❌❌ ERRO DURANTE PROCESSAMENTO DO EVENTO " + event.getType() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        System.out.println("<<< WEBHOOK PROCESSADO COM SUCESSO: " + event.getType() + " ID: " + event.getId() + "\n");
    }

    // --- Métodos Auxiliares para Webhook (já existentes ou modificados) ---
    private void processarCheckoutSession(Session session) throws StripeException {
        // ... (código existente) ...
        System.out.println("--- Iniciando processarCheckoutSession ---");
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null) {
            System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Metadados nulos. Session ID: " + session.getId());
            throw new RuntimeException("Metadados nulos na sessão do Stripe.");
        }
        System.out.println("   Metadados obtidos: " + metadata);

        final String restauranteIdStr = metadata.get("restauranteId");
        if (restauranteIdStr == null) {
            System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): restauranteId ausente nos metadados. Metadata: " + metadata + " Session ID: " + session.getId());
            throw new RuntimeException("restauranteId ausente nos metadados do Stripe.");
        }
        final Long restauranteId = Long.valueOf(restauranteIdStr);

        System.out.println(">>> Tentando buscar Restaurante com ID recebido do webhook: " + restauranteId);
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no webhook com ID: " + restauranteId));
        System.out.println("   Restaurante encontrado: ID " + restaurante.getId() + ", Nome: " + restaurante.getNome());

        String tipoProduto = metadata.get("tipoProduto");
        String planoKey = metadata.get("planoKey");
        String pedidoUuidStr = metadata.get("pedidoUuid");

        System.out.println("   TipoProduto: " + tipoProduto + ", PlanoKey: " + planoKey + ", PedidoUuid: " + pedidoUuidStr);

        if ("PAY_PER_USE".equals(tipoProduto)) {
            System.out.println("   Processando PAY_PER_USE...");
            compensarLimite(restauranteId);
            System.out.println("   Compensação de limite chamada com sucesso.");

        } else if (planoKey != null && !planoKey.isEmpty() && session.getSubscription() != null) {
            System.out.println("   Processando lógica de assinatura...");
            String subscriptionId = session.getSubscription();
            String customerId = session.getCustomer();
            System.out.println("   Subscription ID da Sessão: " + subscriptionId + ", Customer ID da Sessão: " + customerId);

            if (customerId == null) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Customer ID NULO na Session de assinatura.");
                throw new RuntimeException("Customer ID ausente na sessão de assinatura do Stripe.");
            }

            restaurante.setStripeSubscriptionId(subscriptionId);
            restaurante.setStripeCustomerId(customerId);
            System.out.println("   IDs do Stripe ATUALIZADOS no objeto Restaurante.");

            ativarPlanoPorChave(restaurante, planoKey);
            System.out.println("   ativarPlanoPorChave executado com sucesso.");

            try {
                Subscription subscription = Subscription.retrieve(subscriptionId);
                if (subscription.getCurrentPeriodEnd() != null) {
                    restaurante.setDataExpiracaoPlano(LocalDateTime.ofEpochSecond(subscription.getCurrentPeriodEnd(), 0, java.time.ZoneOffset.UTC).plusDays(1));
                    System.out.println("   Data de expiração definida a partir da assinatura: " + restaurante.getDataExpiracaoPlano());
                }
            } catch (StripeException e) {
                System.err.println("   WARN: Não foi possível buscar a assinatura " + subscriptionId + " para definir a data de expiração inicial: " + e.getMessage());
                restaurante.setDataExpiracaoPlano(LocalDateTime.now().plusMonths(1).plusDays(1));
                System.out.println("   Data de expiração definida como fallback (1 mês): " + restaurante.getDataExpiracaoPlano());
            }

            restauranteRepository.save(restaurante);
            System.out.println("✅ Restaurante salvo com sucesso após ativação/atualização do plano.");

        } else if ("PEDIDO_PUBLICO".equals(tipoProduto) && pedidoUuidStr != null) {
            System.out.println("   Processando PEDIDO_PUBLICO...");
            try {
                UUID pedidoUuid = UUID.fromString(pedidoUuidStr);
                System.out.println("   Pedido UUID: " + pedidoUuid);

                TipoPagamento tipoPagamento;
                String paymentMethodType = session.getPaymentMethodTypes().isEmpty() ? null : session.getPaymentMethodTypes().get(0);
                if ("pix".equalsIgnoreCase(paymentMethodType)) {
                    tipoPagamento = TipoPagamento.PIX;
                } else if ("card".equalsIgnoreCase(paymentMethodType)) {
                    tipoPagamento = TipoPagamento.CARTAO_CREDITO;
                } else {
                    tipoPagamento = TipoPagamento.CARTAO_CREDITO;
                    System.out.println("WARN: Tipo de pagamento não identificado claramente na sessão: " + paymentMethodType + ". Usando CARTAO_CREDITO como default.");
                }
                System.out.println("   Tipo de Pagamento identificado: " + tipoPagamento);


                pedidoService.finalizarPedidoAprovado(pedidoUuid, tipoPagamento);
                System.out.println("   Pedido público UUID " + pedidoUuid + " finalizado via PedidoService.");
            } catch (IllegalArgumentException e) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): pedidoUuid inválido: " + pedidoUuidStr);
                throw new RuntimeException("pedidoUuid inválido nos metadados do Stripe.");
            } catch (Exception e) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Erro ao finalizar pedido " + pedidoUuidStr + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Erro ao finalizar pedido pós-pagamento: " + e.getMessage(), e);
            }
        } else {
            System.out.println("ℹ️ INFO WEBHOOK (processarCheckoutSession): Session completada, mas sem tipoProduto/planoKey/pedidoUuid esperado. Modo: " + session.getMode() + ", Status Pagamento: " + session.getPaymentStatus());
        }
        System.out.println("--- Finalizando processarCheckoutSession ---");
    }

    @Transactional
    public void processarCancelamentoAssinatura(Subscription subscription) throws StripeException {
        // ... (código existente) ...
        System.out.println("--- Iniciando processarCancelamentoAssinatura ---");
        String customerId = subscription.getCustomer();
        String subscriptionId = subscription.getId();
        if (customerId == null) {
            System.err.println("❌ ERRO WEBHOOK (processarCancelamento): Customer ID nulo. Subscription ID: " + subscriptionId);
            return;
        }
        System.out.println("   Processando cancelamento para Customer ID: " + customerId + ", Subscription ID: " + subscriptionId);

        Optional<Restaurante> restauranteOpt = restauranteRepository.findByStripeCustomerId(customerId);
        if (restauranteOpt.isEmpty()) {
            System.err.println("⚠️ AVISO WEBHOOK (processarCancelamento): Restaurante não encontrado com Customer ID: " + customerId + ". Ignorando evento.");
            return;
        }
        Restaurante restaurante = restauranteOpt.get();
        System.out.println("   Restaurante encontrado para cancelamento: ID " + restaurante.getId());

        if (subscriptionId.equals(restaurante.getStripeSubscriptionId())) {
            System.out.println("   Assinatura cancelada corresponde à ativa. Revertendo para GRATUITO...");
            restaurante.setPlano("GRATUITO");
            restaurante.setDeliveryPro(false);
            restaurante.setSalaoPro(false);
            restaurante.setStripeSubscriptionId(null);
            restaurante.setDataExpiracaoPlano(null);
            restaurante.setPedidosMesAtual(0);
            restaurante.setLimiteMesas(LIMITE_MESAS_GRATUITO);
            restaurante.setLimiteUsuarios(LIMITE_USUARIOS_GRATUITO);

            restauranteRepository.save(restaurante);
            System.out.println("✅ Restaurante ID: " + restaurante.getId() + " revertido para GRATUITO.");

        } else {
            System.out.println("⚠️ Webhook AVISO (processarCancelamento): Sub ID cancelada (" + subscriptionId + ") não corresponde à ativa (" + restaurante.getStripeSubscriptionId() + ") do Restaurante ID " + restaurante.getId() + ". Ignorando reversão de plano.");
        }
        System.out.println("--- Finalizando processarCancelamentoAssinatura ---");
    }

    @Transactional
    private void processarAtualizacaoAssinatura(Subscription subscriptionUpdated) throws StripeException {
        // ... (código existente) ...
        System.out.println("--- Iniciando processarAtualizacaoAssinatura ---");
        String customerId = subscriptionUpdated.getCustomer();
        String subscriptionId = subscriptionUpdated.getId();

        if (customerId == null) {
            System.err.println("❌ ERRO WEBHOOK (sub.updated): Customer ID nulo na assinatura atualizada. Sub ID: " + subscriptionId);
            return;
        }

        Restaurante restaurante = restauranteRepository.findByStripeCustomerId(customerId).orElse(null);

        if (restaurante == null) {
            System.err.println("⚠️ AVISO WEBHOOK (sub.updated): Restaurante não encontrado com Customer ID: " + customerId + ". Ignorando evento.");
            return;
        }
        System.out.println("   Restaurante encontrado (sub.updated): ID " + restaurante.getId());

        if (subscriptionId.equals(restaurante.getStripeSubscriptionId())) {
            if (subscriptionUpdated.getItems() != null && !subscriptionUpdated.getItems().getData().isEmpty() && subscriptionUpdated.getItems().getData().get(0).getPrice() != null) {
                String newPriceId = subscriptionUpdated.getItems().getData().get(0).getPrice().getId();
                System.out.println("   Novo Price ID detectado (sub.updated): " + newPriceId);

                String planoKey = priceIds.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(newPriceId))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (planoKey != null) {
                    System.out.println("   PlanoKey correspondente encontrado: " + planoKey);
                    ativarPlanoPorChave(restaurante, planoKey);
                    if (subscriptionUpdated.getCurrentPeriodEnd() != null) {
                        restaurante.setDataExpiracaoPlano(LocalDateTime.ofEpochSecond(subscriptionUpdated.getCurrentPeriodEnd(), 0, java.time.ZoneOffset.UTC).plusDays(1));
                        System.out.println("   Data de expiração atualizada para: " + restaurante.getDataExpiracaoPlano());
                    } else {
                        System.out.println("   WARN: Não foi possível obter data de expiração da assinatura atualizada. Mantendo a anterior ou nula.");
                    }
                    restauranteRepository.save(restaurante);
                    System.out.println("✅ Plano do Restaurante ID " + restaurante.getId() + " atualizado via customer.subscription.updated para: " + planoKey);
                } else {
                    System.err.println("❌ ERRO WEBHOOK (sub.updated): Price ID '" + newPriceId + "' recebido do Stripe não encontrado no mapa 'priceIds'. Plano não atualizado no banco.");
                }
            } else {
                System.err.println("❌ ERRO WEBHOOK (sub.updated): Assinatura atualizada não contém itens válidos ou Price ID. Sub ID: " + subscriptionId);
            }
        } else {
            System.out.println("⚠️ Webhook AVISO (sub.updated): ID da assinatura atualizada (" + subscriptionId + ") não corresponde à assinatura principal ("+ restaurante.getStripeSubscriptionId() +") do restaurante ID: " + restaurante.getId() + ". Ignorando atualização no banco.");
        }
        System.out.println("--- Finalizando processarAtualizacaoAssinatura ---");
    }


    private void ativarPlanoPorChave(Restaurante restaurante, String planoKey) {
        // ... (código existente) ...
        System.out.println("   --- Iniciando ativarPlanoPorChave ---");
        System.out.println("   Ativando plano " + planoKey + " para restaurante ID: " + restaurante.getId());
        restaurante.setPedidosMesAtual(0);
        LocalDateTime novaExpiracao = null;
        String nomePlano = "GRATUITO";
        boolean ativaDelivery = false;
        boolean ativaSalao = false;
        int limiteMesas = LIMITE_MESAS_GRATUITO;
        int limiteUsuarios = LIMITE_USUARIOS_GRATUITO;

        switch (planoKey) {
            case "DELIVERY_PRO_MENSAL":
                nomePlano = "DELIVERY_PRO";
                ativaDelivery = true;
                ativaSalao = restaurante.isSalaoPro();
                limiteMesas = restaurante.isSalaoPro() ? LIMITE_ILIMITADO : LIMITE_MESAS_GRATUITO;
                limiteUsuarios = restaurante.isSalaoPro() ? LIMITE_ILIMITADO : LIMITE_USUARIOS_GRATUITO;
                break;
            case "SALAO_PDV_MENSAL":
                nomePlano = "SALÃO_PDV";
                ativaDelivery = restaurante.isDeliveryPro();
                ativaSalao = true;
                limiteMesas = LIMITE_ILIMITADO;
                limiteUsuarios = LIMITE_ILIMITADO;
                break;
            case "PREMIUM_MENSAL":
            case "PREMIUM_ANUAL":
                nomePlano = "PREMIUM";
                ativaDelivery = true;
                ativaSalao = true;
                limiteMesas = LIMITE_ILIMITADO;
                limiteUsuarios = LIMITE_ILIMITADO;
                break;
            default:
                System.err.println("⚠️ AVISO (ativarPlanoPorChave): planoKey desconhecida: " + planoKey + ". Mantendo plano anterior ou revertendo para GRATUITO se indefinido.");
                if (restaurante.getPlano() == null || restaurante.getPlano().isEmpty()) {
                    nomePlano = "GRATUITO";
                    ativaDelivery = false;
                    ativaSalao = false;
                    limiteMesas = LIMITE_MESAS_GRATUITO;
                    limiteUsuarios = LIMITE_USUARIOS_GRATUITO;
                    novaExpiracao = null;
                    restaurante.setStripeSubscriptionId(null);
                } else {
                    return;
                }
                break;
        }

        restaurante.setPlano(nomePlano);
        restaurante.setDeliveryPro(ativaDelivery);
        restaurante.setSalaoPro(ativaSalao);
        // Data de expiração definida pelos webhooks
        // restaurante.setDataExpiracaoPlano(novaExpiracao);
        restaurante.setLimiteMesas(limiteMesas);
        restaurante.setLimiteUsuarios(limiteUsuarios);

        System.out.println("   Plano definido para: " + nomePlano + ", DeliveryPro: " + ativaDelivery + ", SalaoPro: " + ativaSalao);
        System.out.println("   Limites definidos: Mesas=" + (limiteMesas == LIMITE_ILIMITADO ? "Ilimitado" : limiteMesas) + ", Usuários=" + (limiteUsuarios == LIMITE_ILIMITADO ? "Ilimitado" : limiteUsuarios));
        System.out.println("   --- Finalizando ativarPlanoPorChave ---");
    }

}