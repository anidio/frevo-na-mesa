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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.HashMap;

@Service
public class FinanceiroService {

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

    @Value("${stripe.webhook-secret}") // Chave secreta para validação de webhook
    private String webhookSecret;

    // URL base do seu Frontend (usada para sucesso/cancelamento)
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // Mapeamento dos IDs de Preço do Stripe
    private final Map<String, String> priceIds = Map.of(
            "PAY_PER_USE", "price_ID_PACOTE_AVULSO_AQUI", // R$ 14,90 (Avulso)
            "DELIVERY_PRO_MENSAL", "price_ID_DELIVERY_PRO_AQUI", // R$ 29,90 (Mensal)
            "SALAO_PDV_MENSAL", "price_ID_SALAO_PDV_AQUI", // R$ 39,90 (Mensal)
            "PREMIUM_MENSAL", "price_ID_PREMIUM_MENSAL_AQUI", // R$ 49,90 (Mensal)
            "PREMIUM_ANUAL", "price_ID_PREMIUM_ANUAL_AQUI" // R$ 499,00 (Anual)
    );
    // --- FIM CONFIGURAÇÕES DO STRIPE ---


    // CONSTANTES ANTIGAS MANTIDAS PARA LÓGICA INTERNA
    private static final int PEDIDOS_POR_PACOTE = 10;
    private static final BigDecimal CUSTO_PACOTE = new BigDecimal("14.90");

    @PostConstruct
    public void init() {
        // Inicializa o SDK do Stripe com a chave secreta
        Stripe.apiKey = stripeSecretKey;
    }

    // --- LÓGICA PAY-PER-USE (MODO PAYMENT) ---
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
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado."));

        if (!restaurante.getPlano().equals("GRATUITO")) {
            System.out.println("INFO: Pagamento de pacote recebido, mas o restaurante já está no plano " + restaurante.getPlano());
            return;
        }

        restaurante.setPedidosMesAtual(Math.max(0, restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE));
        restauranteRepository.save(restaurante);
    }
    // --- FIM LÓGICA PAY-PER-USE ---


    // --- LÓGICA DE ASSINATURA (MODO SUBSCRIPTION) ---
    private String gerarUrlUpgradeAssinatura(String planoKey, Long restauranteId) throws StripeException {
        String priceId = priceIds.get(planoKey);
        String frontendBaseUrl = allowedOrigins.split(",")[0];

        String successUrl = frontendBaseUrl + "/admin/financeiro?subscription=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendBaseUrl + "/admin/financeiro?subscription=cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("restauranteId", restauranteId.toString())
                .putMetadata("planoKey", planoKey)
                .setCustomerEmail(restauranteService.getRestauranteLogado().getEmail())
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

    // --- LÓGICA DE PEDIDO PÚBLICO (LANÇA EXCEÇÃO APÓS A MIGRAÇÃO) ---
    public String gerarUrlPagamentoPedidoPublico(UUID uuidPedido, BigDecimal totalPedido) {
        throw new UnsupportedOperationException("A funcionalidade de pagamento público precisa ser migrada do MP para o Stripe.");
    }

    // --- LÓGICA DE WEBHOOK DO STRIPE ---

    /**
     * Valida a assinatura do webhook e despacha o evento para a lógica de negócio.
     * Implementa a lógica que faltava para resolver as referências no Controller.
     */
    @Transactional
    public void processarWebhookStripe(String payload, String sigHeader) throws StripeException {
        Event event = null;

        try {
            // 1. Constrói o evento e valida a assinatura (Segurança crítica)
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

        } catch (SignatureVerificationException e) {
            System.err.println("ERRO: Falha na verificação da assinatura do Webhook do Stripe.");
            throw new SignatureVerificationException("Assinatura inválida.", sigHeader);
        }

        // 2. Despacha o evento
        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (dataObject == null) {
            System.out.println("AVISO: Evento Stripe sem objeto de dados. Tipo: " + event.getType());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                // Disparado para PAGAMENTOS ÚNICOS (Pay-per-Use) E NOVAS ASSINATURAS
                Session session = (Session) dataObject;
                processarCheckoutSession(session);
                break;

            case "customer.subscription.deleted":
                // Disparado quando a assinatura é cancelada pelo cliente, falha na recorrência, ou é excluída.
                Subscription subscriptionDeleted = (Subscription) dataObject;
                processarCancelamentoAssinatura(subscriptionDeleted);
                break;

            default:
                System.out.println("AVISO: Evento Stripe não tratado: " + event.getType());
                break;
        }
    }

    /**
     * Lógica para lidar com a conclusão de uma Checkout Session (Pagamento ou Assinatura).
     */
    private void processarCheckoutSession(Session session) throws StripeException {
        Map<String, String> metadata = session.getMetadata();
        Long restauranteId = Long.valueOf(metadata.get("restauranteId"));
        String tipoProduto = metadata.get("tipoProduto");

        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no webhook."));

        // Lógica de Ativação
        switch (tipoProduto) {
            case "PAY_PER_USE":
                compensarLimite(restauranteId);
                break;

            case "DELIVERY_PRO_MENSAL":
            case "SALAO_PDV_MENSAL":
            case "PREMIUM_MENSAL":
            case "PREMIUM_ANUAL":
                // Se for assinatura, ativamos o plano e salvamos a Subscription ID.
                Subscription subscription = session.getSubscriptionObject();
                if (subscription != null) {
                    restaurante.setStripeSubscriptionId(subscription.getId());
                    restaurante.setStripeCustomerId(session.getCustomer());
                    ativarPlanoPorChave(restaurante, tipoProduto);
                }
                break;
        }
        restauranteRepository.save(restaurante);
    }

    /**
     * Lógica para lidar com o cancelamento/exclusão de uma assinatura.
     */
    private void processarCancelamentoAssinatura(Subscription subscription) throws StripeException {
        Customer customer = Customer.retrieve(subscription.getCustomer());

        Restaurante restaurante = restauranteRepository.findByEmail(customer.getEmail())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado no cancelamento de assinatura."));

        // Desativa o plano e reverte para GRATUITO
        restaurante.setPlano("GRATUITO");
        restaurante.setDeliveryPro(false);
        restaurante.setSalaoPro(false);
        restaurante.setStripeSubscriptionId(null);
        restaurante.setDataExpiracaoPlano(LocalDateTime.now());
        restaurante.setPedidosMesAtual(0);
        restauranteRepository.save(restaurante);
    }

    /**
     * Método auxiliar para centralizar a lógica de ativação de plano.
     */
    private void ativarPlanoPorChave(Restaurante restaurante, String planoKey) {
        restaurante.setPedidosMesAtual(0);
        LocalDateTime novaExpiracao = null;

        if (planoKey.contains("DELIVERY_PRO")) {
            restaurante.setPlano("DELIVERY_PRO");
            restaurante.setDeliveryPro(true);
            restaurante.setSalaoPro(false);
            novaExpiracao = LocalDateTime.now().plusMonths(1);
        } else if (planoKey.contains("SALAO_PDV")) {
            restaurante.setPlano("SALÃO_PDV");
            restaurante.setDeliveryPro(false);
            restaurante.setSalaoPro(true);
            novaExpiracao = LocalDateTime.now().plusMonths(1);
        } else if (planoKey.contains("PREMIUM_MENSAL")) {
            restaurante.setPlano("PREMIUM");
            restaurante.setDeliveryPro(true);
            restaurante.setSalaoPro(true);
            novaExpiracao = LocalDateTime.now().plusMonths(1);
        } else if (planoKey.contains("PREMIUM_ANUAL")) {
            restaurante.setPlano("PREMIUM");
            restaurante.setDeliveryPro(true);
            restaurante.setSalaoPro(true);
            novaExpiracao = LocalDateTime.now().plusYears(1);
        }

        restaurante.setDataExpiracaoPlano(novaExpiracao);
    }
}