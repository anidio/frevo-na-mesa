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
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription; // Importado apenas uma vez
import com.stripe.net.Webhook;
// Importando classes com nomes conflitantes usando nome completo ou alias (não necessário aqui se usarmos nome completo)

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class FinanceiroService {

    // Constantes para limites (sem alterações)
    private static final int LIMITE_MESAS_GRATUITO = 10;
    private static final int LIMITE_USUARIOS_GRATUITO = 4;
    private static final int LIMITE_ILIMITADO = Integer.MAX_VALUE;
    private static final int PEDIDOS_POR_PACOTE = 10;

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Lazy
    @Autowired
    private PedidoService pedidoService;

    // --- CONFIGURAÇÕES DO STRIPE --- (sem alterações)
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // IDs de Preço (sem alterações)
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

    // --- LÓGICA PAY-PER-USE --- (sem alterações)
    public String gerarUrlPagamento(Long restauranteId) throws StripeException {
        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String successUrl = frontendBaseUrl + "/admin/financeiro?payment=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?payment=cancel";

        // Usando o SessionCreateParams do checkout
        com.stripe.param.checkout.SessionCreateParams params =
                com.stripe.param.checkout.SessionCreateParams.builder()
                        .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("restauranteId", restauranteId.toString())
                        .putMetadata("tipoProduto", "PAY_PER_USE")
                        .addLineItem(
                                com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                        .setPrice(priceIds.get("PAY_PER_USE"))
                                        .setQuantity(1L)
                                        .build())
                        .build();

        // Usando o Session do checkout
        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public void compensarLimite(Long restauranteId) { // Sem alterações
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado ao compensar limite. ID: " + restauranteId));

        if (restaurante.getPlano().equals("GRATUITO") && !restaurante.isDeliveryPro()) {
            restaurante.setPedidosMesAtual(Math.max(0, restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE));
            restauranteRepository.save(restaurante);
            System.out.println("INFO: Limite compensado para Restaurante ID: " + restauranteId + ". Pedidos atuais: " + restaurante.getPedidosMesAtual());
        } else {
            System.out.println("INFO: Pagamento de pacote recebido (Restaurante ID: " + restauranteId + "), mas o plano não é GRATUITO ou já é PRO. Nenhuma compensação necessária.");
        }
    }
    // --- FIM LÓGICA PAY-PER-USE ---


    // --- LÓGICA DE ASSINATURA (Checkout) --- (Usando nomes completos onde necessário)
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

        System.out.println("DEBUG: Preparando Checkout. CustomerId existente no BD: " + existingCustomerId + ", SubscriptionId existente no BD: " + existingSubscriptionId);

        // Usando o Builder do SessionCreateParams do checkout
        com.stripe.param.checkout.SessionCreateParams.Builder paramsBuilder =
                com.stripe.param.checkout.SessionCreateParams.builder()
                        .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("restauranteId", restauranteId.toString())
                        .putMetadata("planoKey", planoKey)
                        .setCustomer(existingCustomerId != null ? existingCustomerId : null)
                        .setCustomerEmail(existingCustomerId == null ? customerEmail : null)
                        .addLineItem( // Adiciona o item do novo plano
                                com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build());

        if (existingSubscriptionId != null && !existingSubscriptionId.isEmpty()) {
            System.out.println("INFO: Gerando sessão de Checkout para ATUALIZAR assinatura (via Customer ID: " + existingCustomerId + ").");
        } else {
            System.out.println("INFO: Gerando sessão de Checkout para CRIAR NOVA assinatura para o Customer: " + (existingCustomerId != null ? existingCustomerId : "Novo (email: " + customerEmail + ")"));
        }

        com.stripe.param.checkout.SessionCreateParams finalParams = paramsBuilder.build();
        System.out.println("DEBUG: Parâmetros FINAIS enviados para Session.create: Customer=" + finalParams.getCustomer() + ", CustomerEmail=" + finalParams.getCustomerEmail());

        // Usando o Session do checkout
        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(finalParams);
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


    // --- LÓGICA DO PORTAL DO CLIENTE (Usando nomes completos) ---
    public String criarSessaoPortalCliente() throws StripeException {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        String stripeCustomerId = restaurante.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            System.err.println("ERRO: Tentativa de abrir portal para restaurante sem Stripe Customer ID. Restaurante ID: " + restaurante.getId());
            throw new RuntimeException("Cliente Stripe não encontrado para este restaurante.");
        }

        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String returnUrl = frontendBaseUrl + "/admin/financeiro";

        // Usando o SessionCreateParams do billingportal
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(stripeCustomerId)
                        .setReturnUrl(returnUrl)
                        .build();

        // Usando o Session do billingportal
        com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
        System.out.println("INFO: Sessão do Portal do Cliente criada para Customer ID: " + stripeCustomerId);
        return portalSession.getUrl();
    }
    // --- FIM LÓGICA DO PORTAL DO CLIENTE ---


    // --- LÓGICA DE PEDIDO PÚBLICO --- (Sem alterações)
    public String gerarUrlPagamentoPedidoPublico(UUID uuidPedido, BigDecimal totalPedido) throws StripeException {
        System.err.println("AVISO: Tentativa de chamar gerarUrlPagamentoPedidoPublico não implementado.");
        throw new UnsupportedOperationException("A funcionalidade de pagamento público com Stripe ainda não foi implementada.");
    }

    @Transactional
    public void processarWebhookStripe(String payload, String sigHeader) throws StripeException {
        System.out.println("\n>>> WEBHOOK RECEBIDO! Header Stripe-Signature: " + (sigHeader != null && !sigHeader.isEmpty() ? "Presente" : "AUSENTE ou VAZIO"));

        Event event;
        try {
            // Valida a assinatura do webhook
            event = Webhook.constructEvent(payload, sigHeader, this.webhookSecret);
            System.out.println("✅ Webhook Verificado: " + event.getType() + " ID: " + event.getId());
        } catch (SignatureVerificationException e) {
            System.err.println("❌ ERRO WEBHOOK: Assinatura inválida! Verifique o 'webhookSecret'.");
            throw e; // Retorna 400 Bad Request para o Stripe
        } catch (Exception e) {
            System.err.println("❌ ERRO WEBHOOK: Falha ao construir evento: " + e.getMessage());
            e.printStackTrace();
            // Retorna 500 Internal Server Error para o Stripe tentar reenviar
            throw new RuntimeException("Erro ao processar webhook: " + e.getMessage(), e);
        }

        // Desserializa o objeto do evento
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject dataObject = null;
        try {
            if (dataObjectDeserializer.getObject().isPresent()) {
                dataObject = dataObjectDeserializer.getObject().get();
                System.out.println("Webhook INFO: Deserialização automática OK para tipo: " + event.getType());
            } else {
                // Tenta desserialização explícita se a automática falhar (menos seguro)
                System.err.println("Webhook AVISO: Desserialização automática falhou para: " + event.getType() + ". Tentando desserialização explícita (unsafe).");
                dataObject = dataObjectDeserializer.deserializeUnsafe();
                System.out.println("Webhook INFO: Desserialização explícita bem-sucedida para tipo: " + event.getType());
            }
        } catch (Exception e) {
            System.err.println("❌❌ ERRO CRÍTICO NA DESSERIALIZAÇÃO para evento " + event.getType() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha ao desserializar objeto Stripe para evento: " + event.getType(), e); // Força 500
        }

        // Verifica se a desserialização funcionou
        if (dataObject == null) {
            System.err.println("❌❌ ERRO CRÍTICO: dataObject NULO após tentativa de desserialização para tipo: " + event.getType());
            throw new RuntimeException("dataObject nulo para evento: " + event.getType()); // Força 500
        }
        System.out.println("Webhook INFO: Objeto de dados obtido com sucesso: " + dataObject.getClass().getName());

        // Processa os eventos específicos
        try {
            switch (event.getType()) {
                // Evento disparado quando um Checkout (assinatura ou pagamento) é concluído
                case "checkout.session.completed":
                    // Verifica se o objeto é do tipo correto
                    if (dataObject instanceof com.stripe.model.checkout.Session) {
                        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) dataObject;
                        System.out.println("Webhook: Chamando processarCheckoutSession para Session ID: " + session.getId());
                        // Chama o método para lidar com a conclusão do checkout
                        processarCheckoutSession(session);
                        System.out.println("✅ Webhook: processarCheckoutSession concluído com sucesso para Session ID: " + session.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado checkout.Session para checkout.session.completed, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                // Evento disparado quando uma assinatura é cancelada (pelo cliente no portal ou via API)
                case "customer.subscription.deleted":
                    if (dataObject instanceof Subscription) {
                        Subscription subscriptionDeleted = (Subscription) dataObject;
                        System.out.println("Webhook: Chamando processarCancelamentoAssinatura para Subscription ID: " + subscriptionDeleted.getId());
                        // Chama o método para lidar com o cancelamento
                        processarCancelamentoAssinatura(subscriptionDeleted);
                        System.out.println("✅ Webhook: processarCancelamentoAssinatura concluído com sucesso para Sub ID: " + subscriptionDeleted.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Subscription para customer.subscription.deleted, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                // *** CASO CORRIGIDO PARA ATUALIZAÇÃO VIA PORTAL ***
                case "customer.subscription.updated":
                    if (dataObject instanceof Subscription) {
                        Subscription subscriptionUpdated = (Subscription) dataObject;
                        System.out.println("Webhook: Processando customer.subscription.updated para Subscription ID: " + subscriptionUpdated.getId());

                        // --- LÓGICA DE ATUALIZAÇÃO ADICIONADA ---
                        try {
                            String customerId = subscriptionUpdated.getCustomer();
                            if (customerId == null) {
                                System.err.println("❌ ERRO WEBHOOK (sub.updated): Customer ID nulo na assinatura atualizada. Sub ID: " + subscriptionUpdated.getId());
                                break; // Sai do case, não há como encontrar o restaurante
                            }

                            // Busca o restaurante pelo Customer ID
                            Restaurante restaurante = restauranteRepository.findByStripeCustomerId(customerId)
                                    .orElseThrow(() -> new RuntimeException("Restaurante não encontrado via customer.subscription.updated com Customer ID: " + customerId));
                            System.out.println("   Restaurante encontrado (sub.updated): ID " + restaurante.getId());

                            // Verifica se a assinatura atualizada é a assinatura principal do restaurante
                            // Isso é importante caso, no futuro, um cliente possa ter múltiplas assinaturas (o que não parece ser o caso aqui)
                            if (subscriptionUpdated.getId().equals(restaurante.getStripeSubscriptionId())) {

                                // Pega o Price ID do *primeiro* item da assinatura atualizada
                                // Assumindo que seus planos têm apenas um item principal
                                if (subscriptionUpdated.getItems() != null && !subscriptionUpdated.getItems().getData().isEmpty()) {
                                    // Acessa o objeto Price dentro do Item para pegar o ID
                                    String newPriceId = subscriptionUpdated.getItems().getData().get(0).getPrice().getId();
                                    System.out.println("   Novo Price ID detectado (sub.updated): " + newPriceId);

                                    // Tenta encontrar a chave interna (planoKey) correspondente ao Price ID
                                    String planoKey = priceIds.entrySet().stream()
                                            .filter(entry -> entry.getValue().equals(newPriceId))
                                            .map(Map.Entry::getKey)
                                            .findFirst()
                                            .orElse(null); // Retorna null se não encontrar

                                    if (planoKey != null) {
                                        System.out.println("   PlanoKey correspondente encontrado: " + planoKey);
                                        // Chama a lógica de ativação/atualização de plano no seu sistema
                                        ativarPlanoPorChave(restaurante, planoKey);
                                        // Salva as alterações no banco de dados
                                        restauranteRepository.save(restaurante);
                                        System.out.println("✅ Plano do Restaurante ID " + restaurante.getId() + " atualizado via customer.subscription.updated para: " + planoKey);
                                    } else {
                                        // Loga um erro se o Price ID recebido do Stripe não existe no seu mapa `priceIds`
                                        System.err.println("❌ ERRO WEBHOOK (sub.updated): Price ID '" + newPriceId + "' recebido do Stripe não encontrado no mapa 'priceIds'. Plano não atualizado no banco.");
                                        // Considerar notificar um administrador aqui
                                    }
                                } else {
                                    System.err.println("❌ ERRO WEBHOOK (sub.updated): Assinatura atualizada não contém itens válidos. Sub ID: " + subscriptionUpdated.getId());
                                }
                            } else {
                                // Loga um aviso se a assinatura atualizada não for a principal registrada (caso raro)
                                System.out.println("⚠️ Webhook AVISO (sub.updated): ID da assinatura atualizada (" + subscriptionUpdated.getId() + ") não corresponde à assinatura principal ("+ restaurante.getStripeSubscriptionId() +") do restaurante ID: " + restaurante.getId() + ". Ignorando atualização no banco.");
                            }

                        } catch (Exception e) {
                            // Captura erros durante o processamento específico deste evento
                            System.err.println("❌❌ ERRO AO PROCESSAR customer.subscription.updated para Sub ID " + subscriptionUpdated.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                            // Não relança a exceção aqui para permitir que outros eventos sejam processados,
                            // mas o erro foi logado. O Stripe tentará reenviar este evento específico se retornar erro 500.
                            // Se quiser forçar o reenvio pelo Stripe, descomente a linha abaixo:
                            // throw e;
                        }
                        // --- FIM DA LÓGICA ADICIONADA ---

                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Subscription para customer.subscription.updated, recebeu: " + dataObject.getClass().getName());
                    }
                    break; // Fim do case customer.subscription.updated

                // Outros eventos que você pode querer tratar no futuro
                // case "invoice.payment_failed":
                //     // Lógica para lidar com falha de pagamento de renovação
                //     break;

                default:
                    // Loga eventos que não estamos tratando explicitamente
                    System.out.println("ℹ️ INFO WEBHOOK: Evento não tratado recebido: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            // Captura qualquer erro não tratado dentro do switch
            System.err.println("❌❌ ERRO DURANTE PROCESSAMENTO DO EVENTO " + event.getType() + ": " + e.getMessage());
            e.printStackTrace();
            // Relança a exceção para garantir que o Stripe saiba que houve um erro (retorna 500)
            throw e;
        }
        // Mensagem final de sucesso (se nenhum erro foi lançado)
        System.out.println("<<< WEBHOOK PROCESSADO COM SUCESSO: " + event.getType() + " ID: " + event.getId() + "\n");
    } // Fim do método processarWebhookStripe

    // --- processarCheckoutSession --- (Tipo do parâmetro ajustado para nome completo)
    private void processarCheckoutSession(com.stripe.model.checkout.Session session) throws StripeException {
        System.out.println("--- Iniciando processarCheckoutSession ---");
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("restauranteId")) {
            System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Metadados inválidos ou restauranteId ausente. Metadata: " + metadata + " Session ID: " + session.getId());
            throw new RuntimeException("Metadados inválidos na sessão do Stripe.");
        }
        System.out.println("   Metadados obtidos: " + metadata);

        final Long restauranteId = Long.valueOf(metadata.get("restauranteId"));

        System.out.println(">>> Tentando buscar Restaurante com ID recebido do webhook: " + restauranteId);

        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no webhook com ID: " + restauranteId));
        System.out.println("   Restaurante encontrado: ID " + restaurante.getId() + ", Nome: " + restaurante.getNome());


        String tipoProduto = metadata.get("tipoProduto");
        String planoKey = metadata.get("planoKey");
        System.out.println("   TipoProduto: " + tipoProduto + ", PlanoKey: " + planoKey);


        if ("PAY_PER_USE".equals(tipoProduto)) {
            System.out.println("   Processando PAY_PER_USE...");
            compensarLimite(restauranteId);
            System.out.println("   Compensação de limite chamada com sucesso.");

        } else if (planoKey != null && !planoKey.isEmpty()) {
            System.out.println("   Processando lógica de assinatura...");
            String subscriptionId = session.getSubscription();
            String customerId = session.getCustomer();
            System.out.println("   Subscription ID da Sessão: " + subscriptionId + ", Customer ID da Sessão: " + customerId);

            if (subscriptionId == null || customerId == null) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Subscription ID (" + subscriptionId + ") ou Customer ID (" + customerId + ") NULO na Session.");
                throw new RuntimeException("Dados da assinatura ou cliente ausentes na sessão do Stripe.");
            }

            restaurante.setStripeSubscriptionId(subscriptionId);
            restaurante.setStripeCustomerId(customerId);
            System.out.println("   IDs do Stripe ATUALIZADOS no objeto Restaurante.");

            ativarPlanoPorChave(restaurante, planoKey); // Usa a versão corrigida
            System.out.println("   ativarPlanoPorChave executado com sucesso.");

            restauranteRepository.save(restaurante);
            System.out.println("✅ Restaurante salvo com sucesso após ativação/atualização do plano.");

        } else if ("PEDIDO_PUBLICO".equals(tipoProduto)) {
            // Lógica para pedido público... (sem alterações)
            System.out.println("   Processando PEDIDO_PUBLICO...");
            String pedidoUuidStr = metadata.get("pedidoUuid");
            if (pedidoUuidStr != null) {
                try {
                    UUID pedidoUuid = UUID.fromString(pedidoUuidStr);
                    System.out.println("   Pedido UUID: " + pedidoUuid);
                    TipoPagamento tipoPagamento = TipoPagamento.CARTAO_CREDITO;
                    pedidoService.finalizarPedidoAprovado(pedidoUuid, tipoPagamento);
                    System.out.println("   Pedido público UUID " + pedidoUuid + " finalizado via PedidoService.");
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): pedidoUuid inválido: " + pedidoUuidStr);
                    throw new RuntimeException("pedidoUuid inválido nos metadados do Stripe.");
                }
            } else {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): PEDIDO_PUBLICO sem pedidoUuid. Session ID: " + session.getId());
                throw new RuntimeException("pedidoUuid ausente para PEDIDO_PUBLICO.");
            }
        } else {
            System.out.println("ℹ️ INFO WEBHOOK (processarCheckoutSession): Session completada sem tipo conhecido.");
        }
        System.out.println("--- Finalizando processarCheckoutSession ---");
    }

    // --- processarCancelamentoAssinatura --- (Sem alterações)
    @Transactional
    public void processarCancelamentoAssinatura(Subscription subscription) throws StripeException {
        System.out.println("--- Iniciando processarCancelamentoAssinatura ---");
        String customerId = subscription.getCustomer();
        String subscriptionId = subscription.getId();
        if (customerId == null) {
            System.err.println("❌ ERRO WEBHOOK (processarCancelamento): Customer ID nulo. Subscription ID: " + subscriptionId);
            throw new RuntimeException("Customer ID não encontrado no evento de cancelamento.");
        }
        System.out.println("   Processando cancelamento para Customer ID: " + customerId + ", Subscription ID: " + subscriptionId);

        Restaurante restaurante = restauranteRepository.findByStripeCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado com Customer ID: " + customerId));
        System.out.println("   Restaurante encontrado para cancelamento: ID " + restaurante.getId());


        if (subscriptionId.equals(restaurante.getStripeSubscriptionId())) {
            System.out.println("   Assinatura cancelada corresponde à ativa. Revertendo para GRATUITO...");
            restaurante.setPlano("GRATUITO");
            restaurante.setDeliveryPro(false);
            restaurante.setSalaoPro(false);
            restaurante.setStripeSubscriptionId(null);
            restaurante.setDataExpiracaoPlano(LocalDateTime.now());
            restaurante.setPedidosMesAtual(0);
            restaurante.setLimiteMesas(LIMITE_MESAS_GRATUITO);
            restaurante.setLimiteUsuarios(LIMITE_USUARIOS_GRATUITO);

            restauranteRepository.save(restaurante);
            System.out.println("✅ Restaurante ID: " + restaurante.getId() + " revertido para GRATUITO.");

        } else {
            System.out.println("⚠️ Webhook AVISO (processarCancelamento): Sub ID cancelada (" + subscriptionId + ") não corresponde à ativa (" + restaurante.getStripeSubscriptionId() + "). Ignorando.");
        }
        System.out.println("--- Finalizando processarCancelamentoAssinatura ---");
    }

    // --- ativarPlanoPorChave --- (VERSÃO CORRIGIDA)
    private void ativarPlanoPorChave(Restaurante restaurante, String planoKey) {
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
                ativaSalao = false; // Garante desativação
                novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
                limiteMesas = LIMITE_MESAS_GRATUITO;
                limiteUsuarios = LIMITE_USUARIOS_GRATUITO;
                break;
            case "SALAO_PDV_MENSAL":
                nomePlano = "SALÃO_PDV";
                ativaDelivery = false; // Garante desativação
                ativaSalao = true;
                limiteMesas = LIMITE_ILIMITADO;
                limiteUsuarios = LIMITE_ILIMITADO;
                novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
                break;
            case "PREMIUM_MENSAL":
                nomePlano = "PREMIUM";
                ativaDelivery = true;
                ativaSalao = true;
                limiteMesas = LIMITE_ILIMITADO;
                limiteUsuarios = LIMITE_ILIMITADO;
                novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
                break;
            case "PREMIUM_ANUAL":
                nomePlano = "PREMIUM";
                ativaDelivery = true;
                ativaSalao = true;
                limiteMesas = LIMITE_ILIMITADO;
                limiteUsuarios = LIMITE_ILIMITADO;
                novaExpiracao = LocalDateTime.now().plusYears(1).plusDays(1);
                break;
            default:
                System.err.println("⚠️ AVISO (ativarPlanoPorChave): planoKey desconhecida: " + planoKey + ". Revertendo para GRATUITO.");
                nomePlano = "GRATUITO";
                ativaDelivery = false;
                ativaSalao = false;
                limiteMesas = LIMITE_MESAS_GRATUITO;
                limiteUsuarios = LIMITE_USUARIOS_GRATUITO;
                novaExpiracao = null;
                break;
        }

        restaurante.setPlano(nomePlano);
        restaurante.setDeliveryPro(ativaDelivery);
        restaurante.setSalaoPro(ativaSalao);
        restaurante.setDataExpiracaoPlano(novaExpiracao);
        restaurante.setLimiteMesas(limiteMesas);
        restaurante.setLimiteUsuarios(limiteUsuarios);

        System.out.println("   Plano definido: " + nomePlano + ", DeliveryPro: " + ativaDelivery + ", SalaoPro: " + ativaSalao);
        System.out.println("   Limites definidos: Mesas=" + (limiteMesas == LIMITE_ILIMITADO ? "Ilimitado" : limiteMesas) + ", Usuários=" + (limiteUsuarios == LIMITE_ILIMITADO ? "Ilimitado" : limiteUsuarios));
        System.out.println("   Expiração definida para: " + novaExpiracao);
        System.out.println("   --- Finalizando ativarPlanoPorChave ---");
    }

}