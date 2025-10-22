package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.TipoPagamento; // Embora não usado diretamente aqui, mantido por contexto
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// NOVOS IMPORTS DO STRIPE
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
import java.util.Collections; // Embora não usado diretamente aqui, mantido por contexto
import java.util.HashMap; // Embora não usado diretamente aqui, mantido por contexto

@Service
public class FinanceiroService {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Lazy
    @Autowired
    private PedidoService pedidoService; // Mantido, embora não usado diretamente no código fornecido

    // --- CONFIGURAÇÕES DO STRIPE ---
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}") // Chave secreta para validação de webhook
    private String webhookSecret;

    // URL base do seu Frontend (usada para sucesso/cancelamento)
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // Mapeamento dos IDs de Preço do Stripe (Usando os IDs que você forneceu)
    private final Map<String, String> priceIds = Map.of(
            "PAY_PER_USE", "price_1SKMQ2C1ubUDyMdQIs3JhxLP", // R$ 14,90 (Avulso) - ID de TESTE CORRIGIDO
            "DELIVERY_PRO_MENSAL", "price_1SKMPEC1ubUDyMdQQkAPocBf", // R$ 29,90 (Mensal)
            "SALAO_PDV_MENSAL", "price_1SKMPTC1ubUDyMdQyuHgkYrO", // R$ 39,90 (Mensal)
            "PREMIUM_MENSAL", "price_1SKMPmC1ubUDyMdQBnvegHTd", // R$ 49,90 (Mensal)
            "PREMIUM_ANUAL", "price_1SKMQOC1ubUDyMdQHMWKSjMK" // R$ 499,00 (Anual)
    );
    // --- FIM CONFIGURAÇÕES DO STRIPE ---


    // CONSTANTES ANTIGAS MANTIDAS PARA LÓGICA INTERNA
    private static final int PEDIDOS_POR_PACOTE = 10;
    private static final BigDecimal CUSTO_PACOTE = new BigDecimal("14.90"); // Mantido para lógica de compensação

    @PostConstruct
    public void init() {
        // Inicializa o SDK do Stripe com a chave secreta
        Stripe.apiKey = stripeSecretKey;
    }

    // --- LÓGICA PAY-PER-USE (MODO PAYMENT) ---
    public String gerarUrlPagamento(Long restauranteId) throws StripeException {
        // Pega a primeira URL permitida (idealmente a do frontend)
        String frontendBaseUrl = allowedOrigins.split(",")[0];

        String successUrl = frontendBaseUrl + "/admin/financeiro?payment=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?payment=cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("restauranteId", restauranteId.toString())
                .putMetadata("tipoProduto", "PAY_PER_USE") // Metadado para identificar o tipo
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

        if (!restaurante.getPlano().equals("GRATUITO")) {
            System.out.println("INFO: Pagamento de pacote recebido (Restaurante ID: " + restauranteId + "), mas o restaurante já está no plano " + restaurante.getPlano());
            return; // Não faz nada se o plano não for gratuito
        }

        // Reduz o contador de pedidos do mês atual pelo número de pedidos no pacote
        // Math.max garante que o contador não fique negativo
        restaurante.setPedidosMesAtual(Math.max(0, restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE));
        restauranteRepository.save(restaurante);
        System.out.println("INFO: Limite compensado para Restaurante ID: " + restauranteId + ". Pedidos atuais: " + restaurante.getPedidosMesAtual());
    }
    // --- FIM LÓGICA PAY-PER-USE ---


    // --- LÓGICA DE ASSINATURA (MODO SUBSCRIPTION) ---
    private String gerarUrlUpgradeAssinatura(String planoKey, Long restauranteId) throws StripeException {
        String priceId = priceIds.get(planoKey);
        if (priceId == null) {
            throw new RuntimeException("ID de preço não encontrado para a chave de plano: " + planoKey);
        }
        String frontendBaseUrl = allowedOrigins.split(",")[0];

        String successUrl = frontendBaseUrl + "/admin/financeiro?subscription=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?subscription=cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("restauranteId", restauranteId.toString())
                .putMetadata("planoKey", planoKey) // Metadado correto para assinatura
                .setCustomerEmail(restauranteService.getRestauranteLogado().getEmail()) // Pré-preenche o email
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    // Métodos públicos para iniciar cada tipo de assinatura
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

    // --- LÓGICA DE PEDIDO PÚBLICO (NÃO IMPLEMENTADA PARA STRIPE AINDA) ---
    public String gerarUrlPagamentoPedidoPublico(UUID uuidPedido, BigDecimal totalPedido) {
        // Implementação futura necessária aqui para criar uma Session no modo PAYMENT
        // passando detalhes do pedido e o UUID nos metadados.
        throw new UnsupportedOperationException("A funcionalidade de pagamento público com Stripe ainda não foi implementada.");
    }

    // --- LÓGICA DE WEBHOOK DO STRIPE ---

    @Transactional
    public void processarWebhookStripe(String payload, String sigHeader) throws StripeException {
        Event event = null;
        String webhookSecretToUse = this.webhookSecret; // Use a variável de instância

        try {
            // Valida a assinatura do webhook usando o segredo configurado
            event = Webhook.constructEvent(payload, sigHeader, webhookSecretToUse);
            System.out.println("Webhook Recebido: " + event.getType() + " ID: " + event.getId());

        } catch (SignatureVerificationException e) {
            System.err.println("❌ ERRO: Falha na verificação da assinatura do Webhook do Stripe. Verifique o 'webhookSecret'. Header: " + sigHeader);
            // Log do payload pode expor dados sensíveis, use com cautela em produção: System.err.println("Payload: " + payload);
            throw new SignatureVerificationException("Assinatura inválida.", sigHeader); // Re-lança para retornar 400 Bad Request
        } catch (Exception e) {
            System.err.println("❌ ERRO inesperado ao construir evento webhook: " + e.getMessage());
            throw new RuntimeException("Erro ao processar webhook: " + e.getMessage(), e);
        }

        // Tenta obter o objeto desserializado
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject dataObject = null;

        if (dataObjectDeserializer.getObject().isPresent()) {
            dataObject = dataObjectDeserializer.getObject().get();
            System.out.println("Webhook INFO: Deserialização automática bem-sucedida para tipo: " + event.getType());
        } else {
            // Loga o JSON bruto se a desserialização automática falhar
            System.err.println("Webhook AVISO: Desserialização automática inicial falhou para o tipo de evento: " + event.getType() + ". Tentando desserialização explícita. Dados brutos: " + event.getData().toJson());

            // Tenta a desserialização explícita (unsafe) para tipos conhecidos
            if ("checkout.session.completed".equals(event.getType())) {
                try {
                    // Tenta forçar a desserialização como um objeto Session
                    dataObject = dataObjectDeserializer.deserializeUnsafe();
                    System.out.println("Webhook INFO: Sessão desserializada com sucesso usando método explícito.");
                } catch (Exception e) {
                    System.err.println("Webhook ERRO: Falha ao desserializar Session mesmo com método explícito: " + e.getMessage());
                    // Decide como lidar com esse erro crítico - talvez lançar exceção?
                    throw new RuntimeException("Falha ao desserializar objeto Session do Stripe", e);
                }
            } else if ("customer.subscription.deleted".equals(event.getType()) || "customer.subscription.updated".equals(event.getType())) {
                // Tipo de objeto esperado para eventos de assinatura
                try {
                    dataObject = dataObjectDeserializer.deserializeUnsafe();
                    System.out.println("Webhook INFO: Assinatura desserializada com sucesso usando método explícito.");
                } catch (Exception e) {
                    System.err.println("Webhook ERRO: Falha ao desserializar Subscription mesmo com método explícito: " + e.getMessage());
                    throw new RuntimeException("Falha ao desserializar objeto Subscription do Stripe", e);
                }
            } else {
                // Se for outro tipo de evento e a desserialização automática falhou
                System.out.println("Webhook AVISO: Desserialização falhou e o tipo de evento " + event.getType() + " não é explicitamente tratado para desserialização explícita.");
                // Não pode prosseguir sem o objeto de dados
                return;
            }
        }

        // Verifica se o objeto de dados foi obtido com sucesso (seja automático ou explícito)
        if (dataObject == null) {
            System.err.println("Webhook ERRO CRÍTICO: dataObject permaneceu nulo após tentativas de desserialização para o tipo de evento: " + event.getType());
            // Não pode prosseguir
            return;
        }

        // Processa eventos específicos usando o dataObject obtido
        switch (event.getType()) {
            case "checkout.session.completed":
                // Verifica o tipo antes de fazer o cast para segurança
                if (dataObject instanceof Session) {
                    Session session = (Session) dataObject;
                    System.out.println("Webhook: Processando checkout.session.completed para Session ID: " + session.getId());
                    processarCheckoutSession(session); // Chama o método que ativa o plano/compensa limite
                } else {
                    System.err.println("Webhook ERRO: Esperado objeto Session para checkout.session.completed, mas recebeu: " + dataObject.getClass().getName());
                }
                break;

            case "customer.subscription.deleted":
                if (dataObject instanceof Subscription) {
                    Subscription subscriptionDeleted = (Subscription) dataObject;
                    System.out.println("Webhook: Processando customer.subscription.deleted para Subscription ID: " + subscriptionDeleted.getId());
                    processarCancelamentoAssinatura(subscriptionDeleted); // Chama o método que rebaixa o plano
                } else {
                    System.err.println("Webhook ERRO: Esperado objeto Subscription para customer.subscription.deleted, mas recebeu: " + dataObject.getClass().getName());
                }
                break;

            // Adicionar tratamento para customer.subscription.updated se necessário
            case "customer.subscription.updated":
                if (dataObject instanceof Subscription) {
                    Subscription subscriptionUpdated = (Subscription) dataObject;
                    System.out.println("Webhook: Processando customer.subscription.updated para Subscription ID: " + subscriptionUpdated.getId());
                    // ADICIONAR LÓGICA AQUI SE NECESSÁRIO (ex: atualizar data de expiração se o plano for renovado)
                    // Exemplo: processarAtualizacaoAssinatura(subscriptionUpdated);
                } else {
                    System.err.println("Webhook ERRO: Esperado objeto Subscription para customer.subscription.updated, mas recebeu: " + dataObject.getClass().getName());
                }
                break;

            default:
                System.out.println("ℹ️ INFO: Evento Stripe não tratado recebido (após desserialização): " + event.getType());
                break;
        }
    }

    /**
     * Lógica para lidar com a conclusão de uma Checkout Session (Pagamento ou Assinatura).
     * CORRIGIDO para verificar 'planoKey' para assinaturas.
     */
    private void processarCheckoutSession(Session session) throws StripeException {
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("restauranteId")) {
            System.err.println("Webhook ERRO: Metadados inválidos ou restauranteId ausente na Session ID: " + session.getId());
            throw new RuntimeException("Metadados inválidos na sessão do Stripe.");
        }

        Long restauranteId = Long.valueOf(metadata.get("restauranteId"));
        String tipoProduto = metadata.get("tipoProduto"); // Usado para Pay-Per-Use
        String planoKey = metadata.get("planoKey");       // Usado para Assinaturas

        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no webhook. ID: " + restauranteId));

        System.out.println("Webhook: Restaurante encontrado: ID " + restauranteId + ", Nome: " + restaurante.getNome());

        if ("PAY_PER_USE".equals(tipoProduto)) {
            // Lógica para pagamento avulso
            System.out.println("Webhook: Identificado como PAY_PER_USE para restaurante ID: " + restauranteId);
            compensarLimite(restauranteId); // Chama o método que ajusta pedidosMesAtual

        } else if (planoKey != null && !planoKey.isEmpty()) {
            // Lógica para NOVA assinatura
            System.out.println("Webhook: Identificado como Assinatura com planoKey: " + planoKey + " para restaurante ID: " + restauranteId);

            // Tenta obter o objeto Subscription diretamente da Session (geralmente presente em modo subscription)
            Subscription subscription = null;
            String subscriptionId = session.getSubscription(); // Obtém o ID da assinatura (sub_...)

            if (subscriptionId != null) {
                System.out.println("Webhook: Subscription ID encontrado na sessão: " + subscriptionId);
                try {
                    // Busca os detalhes completos da assinatura usando o ID
                    subscription = Subscription.retrieve(subscriptionId);
                    System.out.println("Webhook: Detalhes da Subscription recuperados com sucesso.");
                } catch (StripeException e) {
                    System.err.println("Webhook ERRO: Falha ao buscar Subscription ID: " + subscriptionId + " - " + e.getMessage());
                    // Considerar logar o erro e talvez tentar novamente ou notificar admin
                    throw new RuntimeException("Erro ao buscar assinatura no Stripe: " + e.getMessage());
                }
            } else {
                System.err.println("Webhook ERRO: Subscription ID NULO na Session ID: " + session.getId() + " para restaurante ID: " + restauranteId + ". Não é possível ativar o plano.");
                // Isso não deveria acontecer em uma sessão de assinatura bem-sucedida
                throw new RuntimeException("ID da Assinatura não encontrado na sessão do Stripe.");
            }


            if (subscription != null) {
                // Salva os IDs importantes do Stripe no restaurante
                restaurante.setStripeSubscriptionId(subscription.getId());

                String customerId = session.getCustomer(); // ID do Cliente (cus_...)
                if (customerId != null) {
                    restaurante.setStripeCustomerId(customerId);
                    System.out.println("Webhook: Stripe Customer ID salvo: " + customerId + " para restaurante ID: " + restauranteId);
                } else {
                    System.err.println("Webhook AVISO: Customer ID nulo na sessão para restaurante ID: " + restauranteId + ". A gestão futura da assinatura pode ser afetada.");
                    // Não lançar erro, mas logar é importante
                }

                // Ativa o plano correspondente no banco de dados
                ativarPlanoPorChave(restaurante, planoKey); // Atualiza plano, flags (isDeliveryPro/isSalaoPro), dataExpiracao
                System.out.println("Webhook: Plano " + planoKey + " ativado no banco para restaurante ID: " + restauranteId + ". Sub ID: " + subscription.getId());

                restauranteRepository.save(restaurante); // Salva as alterações no restaurante
                System.out.println("Webhook: Alterações salvas no Restaurante ID: " + restauranteId);
            } else {
                // Se mesmo após buscar pelo ID, a assinatura não for encontrada (muito improvável)
                System.err.println("Webhook ERRO CRÍTICO: Objeto Subscription não pôde ser recuperado para restaurante ID: " + restauranteId + " e planoKey: " + planoKey);
                throw new RuntimeException("Objeto Subscription não pôde ser recuperado no Stripe.");
            }

        } else {
            // Caso não seja nem PAY_PER_USE nem uma assinatura (planoKey não encontrado nos metadados)
            // Isso pode acontecer para pagamentos únicos não relacionados ao PAY_PER_USE (se houver no futuro)
            System.out.println("Webhook INFO: checkout.session.completed recebido sem tipoProduto='PAY_PER_USE' ou planoKey. Session ID: " + session.getId() + ", Restaurante ID: " + restauranteId);
        }
    }


    @Transactional // Garante que as operações no banco sejam atômicas
    public void processarCancelamentoAssinatura(Subscription subscription) throws StripeException {
        String customerId = subscription.getCustomer();
        if (customerId == null) {
            System.err.println("Webhook ERRO: Customer ID nulo no evento customer.subscription.deleted. Subscription ID: " + subscription.getId());
            throw new RuntimeException("Customer ID não encontrado no evento de cancelamento da assinatura.");
        }
        System.out.println("Webhook: Processando cancelamento para Customer ID: " + customerId);

        // Busca o restaurante pelo ID do cliente Stripe (forma mais confiável)
        Restaurante restaurante = restauranteRepository.findByStripeCustomerId(customerId) // SUPONDO QUE VOCÊ ADICIONOU ESTE MÉTODO AO REPOSITORY
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no cancelamento de assinatura com Customer ID: " + customerId));

        System.out.println("Webhook: Restaurante encontrado para cancelamento: ID " + restaurante.getId() + ", Nome: " + restaurante.getNome());

        // Verifica se a assinatura cancelada é a que está ativa no restaurante
        if (subscription.getId().equals(restaurante.getStripeSubscriptionId())) {
            // Desativa o plano e reverte para GRATUITO
            restaurante.setPlano("GRATUITO");
            restaurante.setDeliveryPro(false);
            restaurante.setSalaoPro(false);
            restaurante.setStripeSubscriptionId(null); // Limpa o ID da assinatura cancelada
            // restaurante.setStripeCustomerId(null); // Opcional: manter ou limpar o customer ID
            restaurante.setDataExpiracaoPlano(LocalDateTime.now()); // Marca a expiração como agora
            restaurante.setPedidosMesAtual(0); // Reseta contador de pedidos
            restauranteRepository.save(restaurante);
            System.out.println("Webhook: Plano do Restaurante ID: " + restaurante.getId() + " revertido para GRATUITO devido ao cancelamento da Subscription ID: " + subscription.getId());
        } else {
            System.out.println("Webhook AVISO: Evento de cancelamento recebido para Subscription ID: " + subscription.getId() + ", mas não corresponde à assinatura ativa (" + restaurante.getStripeSubscriptionId() + ") do Restaurante ID: " + restaurante.getId() + ". Nenhuma ação tomada.");
        }
    }


    private void ativarPlanoPorChave(Restaurante restaurante, String planoKey) {
        System.out.println("Ativando plano " + planoKey + " para restaurante ID: " + restaurante.getId());
        restaurante.setPedidosMesAtual(0); // Reseta contador ao mudar/ativar plano
        LocalDateTime novaExpiracao = null;
        String nomePlano = "GRATUITO"; // Padrão
        boolean ativaDelivery = false;
        boolean ativaSalao = false;

        // Determina as flags e nome do plano baseado na chave
        if (planoKey.equals("DELIVERY_PRO_MENSAL")) {
            nomePlano = "DELIVERY_PRO";
            ativaDelivery = true;
            ativaSalao = false; // Garante que salão seja desativado se não for premium
            novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1); // Adiciona 1 dia de margem
        } else if (planoKey.equals("SALAO_PDV_MENSAL")) {
            nomePlano = "SALÃO_PDV";
            ativaDelivery = false; // Garante que delivery seja desativado se não for premium
            ativaSalao = true;
            novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
        } else if (planoKey.equals("PREMIUM_MENSAL")) {
            nomePlano = "PREMIUM";
            ativaDelivery = true;
            ativaSalao = true;
            novaExpiracao = LocalDateTime.now().plusMonths(1).plusDays(1);
        } else if (planoKey.equals("PREMIUM_ANUAL")) {
            nomePlano = "PREMIUM";
            ativaDelivery = true;
            ativaSalao = true;
            novaExpiracao = LocalDateTime.now().plusYears(1).plusDays(1);
        } else {
            System.err.println("AVISO: planoKey desconhecida em ativarPlanoPorChave: " + planoKey);
            // Mantém como GRATUITO e não define expiração se a chave for inválida
        }

        // Aplica as atualizações no objeto Restaurante
        restaurante.setPlano(nomePlano);
        restaurante.setDeliveryPro(ativaDelivery);
        restaurante.setSalaoPro(ativaSalao);
        restaurante.setDataExpiracaoPlano(novaExpiracao);

        System.out.println("Plano atualizado para: " + nomePlano + ", DeliveryPro: " + ativaDelivery + ", SalaoPro: " + ativaSalao + ", Expira em: " + novaExpiracao);
        // O save é feito no método chamador (processarCheckoutSession)
    }

    // --- Métodos Adicionais Necessários ---

    // Adicione este método ao seu RestauranteRepository.java
    // Optional<Restaurante> findByStripeCustomerId(String stripeCustomerId);
}