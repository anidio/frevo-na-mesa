package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.TipoPagamento; // Mantido por contexto
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// IMPORTS DO STRIPE
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.Customer;
import com.stripe.net.Webhook;
import com.stripe.model.EventDataObjectDeserializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
// import java.util.Collections; // Não usado diretamente
// import java.util.HashMap; // Não usado diretamente

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

    @Lazy
    @Autowired
    private PedidoService pedidoService;

    // --- CONFIGURAÇÕES DO STRIPE ---
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // IDs de Preço (Substitua pelos IDs reais)
    private final Map<String, String> priceIds = Map.of(
            "PAY_PER_USE", "price_1SKMQ2C1ubUDyMdQIs3JhxLP", // Pacote Avulso
            "DELIVERY_PRO_MENSAL", "price_1SKMPEC1ubUDyMdQQkAPocBf", // Delivery Pro Mensal
            "SALAO_PDV_MENSAL", "price_1SKMPTC1ubUDyMdQyuHgkYrO", // Salão PDV Mensal
            "PREMIUM_MENSAL", "price_1SKMPmC1ubUDyMdQBnvegHTd", // Premium Mensal
            "PREMIUM_ANUAL", "price_1SKMQOC1ubUDyMdQHMWKSjMK" // Premium Anual
    );
    // --- FIM CONFIGURAÇÕES DO STRIPE ---

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // --- LÓGICA PAY-PER-USE ---
    public String gerarUrlPagamento(Long restauranteId) throws StripeException {
        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String successUrl = frontendBaseUrl + "/admin/financeiro?payment=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?payment=cancel";

        SessionCreateParams params = SessionCreateParams.builder()
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

        if (restaurante.getPlano().equals("GRATUITO") && !restaurante.isDeliveryPro()) {
            restaurante.setPedidosMesAtual(Math.max(0, restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE));
            restauranteRepository.save(restaurante);
            System.out.println("INFO: Limite compensado para Restaurante ID: " + restauranteId + ". Pedidos atuais: " + restaurante.getPedidosMesAtual());
        } else {
            System.out.println("INFO: Pagamento de pacote recebido (Restaurante ID: " + restauranteId + "), mas o plano não é GRATUITO ou já é PRO. Nenhuma compensação necessária.");
        }
    }
    // --- FIM LÓGICA PAY-PER-USE ---


    // --- LÓGICA DE ASSINATURA ---
    private String gerarUrlUpgradeAssinatura(String planoKey, Long restauranteId) throws StripeException {
        String priceId = priceIds.get(planoKey);
        if (priceId == null) {
            throw new RuntimeException("ID de preço não encontrado para a chave de plano: " + planoKey);
        }
        String frontendBaseUrl = allowedOrigins.split(",")[0];
        String successUrl = frontendBaseUrl + "/admin/financeiro?subscription=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?subscription=cancel";

        String customerEmail = restauranteService.getRestauranteLogado().getEmail();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("restauranteId", restauranteId.toString())
                .putMetadata("planoKey", planoKey)
                .setCustomerEmail(customerEmail)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build())
                .build();

        Session session = Session.create(params);
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

    // --- LÓGICA DE PEDIDO PÚBLICO (AINDA NÃO IMPLEMENTADA) ---
    public String gerarUrlPagamentoPedidoPublico(UUID uuidPedido, BigDecimal totalPedido) throws StripeException {
        System.err.println("AVISO: Tentativa de chamar gerarUrlPagamentoPedidoPublico não implementado.");
        throw new UnsupportedOperationException("A funcionalidade de pagamento público com Stripe ainda não foi implementada.");
    }

    // --- LÓGICA DE WEBHOOK DO STRIPE (COM LOGS DETALHADOS) ---
    @Transactional
    public void processarWebhookStripe(String payload, String sigHeader) throws StripeException {
        // **LOG INICIAL**
        System.out.println("\n>>> WEBHOOK RECEBIDO! Header Stripe-Signature: " + (sigHeader != null && !sigHeader.isEmpty() ? "Presente" : "AUSENTE ou VAZIO"));

        Event event;
        try {
            // Valida a assinatura
            event = Webhook.constructEvent(payload, sigHeader, this.webhookSecret);
            System.out.println("✅ Webhook Verificado: " + event.getType() + " ID: " + event.getId());
        } catch (SignatureVerificationException e) {
            System.err.println("❌ ERRO WEBHOOK: Assinatura inválida! Verifique o 'webhookSecret'.");
            // Não logar payload em produção pode ser mais seguro
            // System.err.println("   Header recebido: " + sigHeader);
            // System.err.println("   Payload recebido: " + payload); // CUIDADO: PODE CONTER DADOS SENSÍVEIS
            throw e; // Retorna 400
        } catch (Exception e) {
            System.err.println("❌ ERRO WEBHOOK: Falha ao construir evento: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar webhook: " + e.getMessage(), e); // Retorna 500
        }

        // Tenta obter o objeto desserializado
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject dataObject = null;
        try {
            if (dataObjectDeserializer.getObject().isPresent()) {
                dataObject = dataObjectDeserializer.getObject().get();
                System.out.println("Webhook INFO: Deserialização automática OK para tipo: " + event.getType());
            } else {
                System.err.println("Webhook AVISO: Desserialização automática falhou para: " + event.getType() + ". Tentando desserialização explícita (unsafe).");
                // Tenta desserialização explícita (pode lançar exceção se o tipo for inesperado)
                dataObject = dataObjectDeserializer.deserializeUnsafe();
                System.out.println("Webhook INFO: Desserialização explícita bem-sucedida para tipo: " + event.getType());
            }
        } catch (Exception e) {
            System.err.println("❌❌ ERRO CRÍTICO NA DESSERIALIZAÇÃO para evento " + event.getType() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha ao desserializar objeto Stripe para evento: " + event.getType(), e); // Força 500
        }

        if (dataObject == null) {
            System.err.println("❌❌ ERRO CRÍTICO: dataObject NULO após tentativa de desserialização para tipo: " + event.getType());
            throw new RuntimeException("dataObject nulo para evento: " + event.getType()); // Força 500
        }
        System.out.println("Webhook INFO: Objeto de dados obtido com sucesso: " + dataObject.getClass().getName());


        // Processa eventos específicos
        try { // Adiciona um try-catch geral para o processamento do evento
            switch (event.getType()) {
                case "checkout.session.completed":
                    if (dataObject instanceof Session) {
                        Session session = (Session) dataObject;
                        System.out.println("Webhook: Chamando processarCheckoutSession para Session ID: " + session.getId());
                        processarCheckoutSession(session); // Lógica principal
                        System.out.println("✅ Webhook: processarCheckoutSession concluído com sucesso para Session ID: " + session.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Session para checkout.session.completed, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                case "customer.subscription.deleted":
                    if (dataObject instanceof Subscription) {
                        Subscription subscriptionDeleted = (Subscription) dataObject;
                        System.out.println("Webhook: Chamando processarCancelamentoAssinatura para Subscription ID: " + subscriptionDeleted.getId());
                        processarCancelamentoAssinatura(subscriptionDeleted); // Lógica de cancelamento
                        System.out.println("✅ Webhook: processarCancelamentoAssinatura concluído com sucesso para Sub ID: " + subscriptionDeleted.getId());
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Subscription para customer.subscription.deleted, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                case "customer.subscription.updated":
                    if (dataObject instanceof Subscription) {
                        Subscription subscriptionUpdated = (Subscription) dataObject;
                        System.out.println("Webhook: Processando customer.subscription.updated para Subscription ID: " + subscriptionUpdated.getId());
                        // Adicionar lógica aqui se necessário (ex: renovação)
                        // processarAtualizacaoAssinatura(subscriptionUpdated);
                    } else {
                        System.err.println("❌ ERRO WEBHOOK: Esperado Subscription para customer.subscription.updated, recebeu: " + dataObject.getClass().getName());
                    }
                    break;

                // Adicione outros eventos aqui se precisar (ex: "invoice.payment_failed")

                default:
                    System.out.println("ℹ️ INFO WEBHOOK: Evento não tratado recebido: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            // Captura qualquer exceção ocorrida DENTRO do processamento do evento
            System.err.println("❌❌ ERRO DURANTE PROCESSAMENTO DO EVENTO " + event.getType() + ": " + e.getMessage());
            e.printStackTrace();
            // Re-lança a exceção para garantir que o Stripe veja o erro 500 e tente reenviar o webhook
            throw e;
        }
        System.out.println("<<< WEBHOOK PROCESSADO COM SUCESSO: " + event.getType() + " ID: " + event.getId() + "\n");
    }

    private void processarCheckoutSession(Session session) throws StripeException {
        System.out.println("--- Iniciando processarCheckoutSession ---");
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("restauranteId")) {
            System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Metadados inválidos ou restauranteId ausente. Metadata: " + metadata + " Session ID: " + session.getId());
            throw new RuntimeException("Metadados inválidos na sessão do Stripe.");
        }
        System.out.println("   Metadados obtidos: " + metadata);

        Long restauranteId = null; // Variável inicial
        try {
            // Atribui o valor à variável não-final
            restauranteId = Long.valueOf(metadata.get("restauranteId"));
        } catch (NumberFormatException e) {
            System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): restauranteId inválido nos metadados: " + metadata.get("restauranteId"));
            throw new RuntimeException("restauranteId inválido nos metadados do Stripe.");
        }

        // **CORREÇÃO AQUI:** Cria uma cópia final da variável para usar na lambda
        final Long finalRestauranteId = restauranteId;

        System.out.println(">>> Tentando buscar Restaurante com ID recebido do webhook: " + finalRestauranteId); // Log usando a variável final

        Restaurante restaurante;
        try {
            // Usa a variável final na busca (opcional, mas consistente)
            restaurante = restauranteRepository.findById(finalRestauranteId)
                    // **CORREÇÃO AQUI:** Usa a variável final DENTRO da lambda
                    .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no webhook com ID: " + finalRestauranteId));
            System.out.println("   Restaurante encontrado: ID " + restaurante.getId() + ", Nome: " + restaurante.getNome());
        } catch (Exception e) {
            // **CORREÇÃO AQUI:** Log de erro usando a variável final
            System.err.println("❌ ERRO AO BUSCAR RESTAURANTE ID " + finalRestauranteId + ": " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lança para o handler principal
        }

        // O resto do método continua igual...
        String tipoProduto = metadata.get("tipoProduto");
        String planoKey = metadata.get("planoKey");
        System.out.println("   TipoProduto: " + tipoProduto + ", PlanoKey: " + planoKey);


        if ("PAY_PER_USE".equals(tipoProduto)) {
            System.out.println("   Processando PAY_PER_USE...");
            try {
                compensarLimite(finalRestauranteId); // Usa a variável final (ou a original, tanto faz aqui fora)
                System.out.println("   Compensação de limite chamada com sucesso.");
            } catch (Exception e) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Erro ao chamar compensarLimite: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

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
            System.out.println("   IDs do Stripe definidos no objeto Restaurante.");

            try {
                ativarPlanoPorChave(restaurante, planoKey); // Atualiza plano, flags E LIMITES
                System.out.println("   ativarPlanoPorChave executado com sucesso.");
            } catch (Exception e) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Erro dentro de ativarPlanoPorChave: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            try {
                restauranteRepository.save(restaurante);
                System.out.println("✅ Restaurante salvo com sucesso após ativação do plano.");
            } catch (Exception e) {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Erro ao salvar restaurante: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

        } else if ("PEDIDO_PUBLICO".equals(tipoProduto)) {
            // ... (lógica pedido público) ...
            System.out.println("   Processando PEDIDO_PUBLICO...");
            String pedidoUuidStr = metadata.get("pedidoUuid");
            if (pedidoUuidStr != null) {
                try {
                    UUID pedidoUuid = UUID.fromString(pedidoUuidStr);
                    System.out.println("   Pedido UUID: " + pedidoUuid);
                    TipoPagamento tipoPagamento = TipoPagamento.CARTAO_CREDITO; // Exemplo
                    pedidoService.finalizarPedidoAprovado(pedidoUuid, tipoPagamento);
                    System.out.println("   Pedido público UUID " + pedidoUuid + " finalizado via PedidoService.");
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): pedidoUuid inválido nos metadados: " + pedidoUuidStr);
                    throw new RuntimeException("pedidoUuid inválido nos metadados do Stripe.");
                } catch (Exception e) {
                    System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): Falha ao finalizar pedido público UUID " + pedidoUuidStr + ": " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else {
                System.err.println("❌ ERRO WEBHOOK (processarCheckoutSession): checkout.session.completed para PEDIDO_PUBLICO sem pedidoUuid. Session ID: " + session.getId());
                throw new RuntimeException("pedidoUuid ausente nos metadados para PEDIDO_PUBLICO.");
            }
        } else {
            System.out.println("ℹ️ INFO WEBHOOK (processarCheckoutSession): Session completada sem tipo conhecido (tipoProduto='" + tipoProduto + "', planoKey='" + planoKey + "').");
        }
        System.out.println("--- Finalizando processarCheckoutSession ---");
    }

    @Transactional
    public void processarCancelamentoAssinatura(Subscription subscription) throws StripeException {
        System.out.println("--- Iniciando processarCancelamentoAssinatura ---");
        String customerId = subscription.getCustomer();
        String subscriptionId = subscription.getId(); // Para logs
        if (customerId == null) {
            System.err.println("❌ ERRO WEBHOOK (processarCancelamento): Customer ID nulo. Subscription ID: " + subscriptionId);
            throw new RuntimeException("Customer ID não encontrado no evento de cancelamento.");
        }
        System.out.println("   Processando cancelamento para Customer ID: " + customerId + ", Subscription ID: " + subscriptionId);

        Restaurante restaurante;
        try {
            // Lembre-se de adicionar findByStripeCustomerId ao RestauranteRepository
            restaurante = restauranteRepository.findByStripeCustomerId(customerId)
                    .orElseThrow(() -> new RuntimeException("Restaurante não encontrado com Customer ID: " + customerId));
            System.out.println("   Restaurante encontrado para cancelamento: ID " + restaurante.getId());
        } catch (Exception e) {
            System.err.println("❌ ERRO WEBHOOK (processarCancelamento): Erro ao buscar restaurante por Customer ID " + customerId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        // Só processa se a assinatura cancelada for a ativa
        if (subscriptionId.equals(restaurante.getStripeSubscriptionId())) {
            System.out.println("   Assinatura cancelada corresponde à ativa no restaurante. Revertendo para GRATUITO...");
            restaurante.setPlano("GRATUITO");
            restaurante.setDeliveryPro(false);
            restaurante.setSalaoPro(false);
            restaurante.setStripeSubscriptionId(null); // Limpa ID cancelado
            // restaurante.setStripeCustomerId(null); // Manter ou limpar? Decida com base na sua lógica de reativação
            restaurante.setDataExpiracaoPlano(LocalDateTime.now()); // Marca expiração
            restaurante.setPedidosMesAtual(0); // Reseta contador
            restaurante.setLimiteMesas(LIMITE_MESAS_GRATUITO); // Restaura limite
            restaurante.setLimiteUsuarios(LIMITE_USUARIOS_GRATUITO); // Restaura limite

            try {
                restauranteRepository.save(restaurante);
                System.out.println("✅ Restaurante ID: " + restaurante.getId() + " revertido para GRATUITO com sucesso.");
            } catch (Exception e) {
                System.err.println("❌ ERRO WEBHOOK (processarCancelamento): Erro ao salvar restaurante após reverter plano: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else {
            System.out.println("⚠️ Webhook AVISO (processarCancelamento): Sub ID cancelada (" + subscriptionId + ") não corresponde à ativa (" + restaurante.getStripeSubscriptionId() + ") do Restaurante ID: " + restaurante.getId() + ". Ignorando.");
        }
        System.out.println("--- Finalizando processarCancelamentoAssinatura ---");
    }

    private void ativarPlanoPorChave(Restaurante restaurante, String planoKey) {
        System.out.println("   --- Iniciando ativarPlanoPorChave ---");
        System.out.println("   Ativando plano " + planoKey + " para restaurante ID: " + restaurante.getId());
        restaurante.setPedidosMesAtual(0); // Reseta contador
        LocalDateTime novaExpiracao = null;
        String nomePlano = "GRATUITO";
        boolean ativaDelivery = false;
        boolean ativaSalao = false;
        int limiteMesas = LIMITE_MESAS_GRATUITO;
        int limiteUsuarios = LIMITE_USUARIOS_GRATUITO;

        if ("DELIVERY_PRO_MENSAL".equals(planoKey)) {
            nomePlano = "DELIVERY_PRO";
            ativaDelivery = true;
            ativaSalao = false; // Garante que salão não seja ativado indevidamente
            novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1); // Margem de 1 dia
        } else if ("SALAO_PDV_MENSAL".equals(planoKey)) {
            nomePlano = "SALÃO_PDV";
            ativaDelivery = false; // Garante que delivery não seja ativado indevidamente
            ativaSalao = true;
            limiteMesas = LIMITE_ILIMITADO;
            limiteUsuarios = LIMITE_ILIMITADO;
            novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
        } else if ("PREMIUM_MENSAL".equals(planoKey)) {
            nomePlano = "PREMIUM";
            ativaDelivery = true;
            ativaSalao = true;
            limiteMesas = LIMITE_ILIMITADO;
            limiteUsuarios = LIMITE_ILIMITADO;
            novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
        } else if ("PREMIUM_ANUAL".equals(planoKey)) {
            nomePlano = "PREMIUM";
            ativaDelivery = true;
            ativaSalao = true;
            limiteMesas = LIMITE_ILIMITADO;
            limiteUsuarios = LIMITE_ILIMITADO;
            novaExpiracao = LocalDateTime.now().plusYears(1).plusDays(1); // Margem de 1 dia
        } else {
            System.err.println("⚠️ AVISO (ativarPlanoPorChave): planoKey desconhecida: " + planoKey + ". Mantendo GRATUITO.");
            // Garante que fique no gratuito se a chave for inválida
            nomePlano = "GRATUITO";
            ativaDelivery = false;
            ativaSalao = false;
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
        // O save() é chamado no método chamador (processarCheckoutSession)
    }

    // Lembrete: Adicionar Optional<Restaurante> findByStripeCustomerId(String stripeCustomerId); ao RestauranteRepository.java
}