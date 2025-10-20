package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.TipoPagamento;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime; // NOVO IMPORT: Para calcular a expiração
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FinanceiroService {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

    // NOVO: Precisamos injetar o PedidoService para finalizar a ordem após o pagamento
    @Lazy
    @Autowired
    private PedidoService pedidoService;

    @Value("${mp.access-token}")
    private String accessToken;

    // URL de Webhook para receber a notificação do Mercado Pago
    @Value("${mp.notification-url}")
    private String notificationUrl;

    // Flag para usar o ambiente de Sandbox
    @Value("${mp.sandbox-mode}")
    private boolean sandboxMode;

    private static final int PEDIDOS_POR_PACOTE = 10;
    private static final BigDecimal CUSTO_PACOTE = new BigDecimal("14.90");

    // NOVAS CONSTANTES DE PREÇO (FASE II)
    private static final BigDecimal CUSTO_DELIVERY_PRO = new BigDecimal("29.90");
    private static final BigDecimal CUSTO_SALAO_PDV = new BigDecimal("35.90");
    private static final BigDecimal CUSTO_PREMIUM = new BigDecimal("49.90");
    private static final BigDecimal CUSTO_PREMIUM_ANUAL = new BigDecimal("499.00");

    private static final int LIMITE_MESAS_PRO = 60;

    @PostConstruct
    public void init() {
        // Inicializa o SDK com o Access Token
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    /**
     * Gera uma URL de checkout para a compra do pacote Pay-per-Use.
     * @param restauranteId ID do restaurante para identificar no webhook.
     * @return URL de Checkout do Mercado Pago.
     */
    public String gerarUrlPagamento(Long restauranteId) throws MPException, MPApiException {
        // 1. Cria o item que será cobrado
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Pacote 10 Pedidos Extras - Frevo na Mesa")
                .quantity(1)
                .unitPrice(CUSTO_PACOTE)
                .build();

        // 2. Dados de Metadados: CRÍTICO para identificarmos o restaurante no Webhook
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("restaurante_id", restauranteId);
        metadata.put("tipo_produto", "PAY_PER_USE");

        // 3. Define a URL de notificação (Webhook) e a requisição
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(Collections.singletonList(item))
                .notificationUrl(notificationUrl)
                .metadata(metadata)
                .externalReference(restauranteId.toString()) // Referência externa
                .build();

        // 4. Cria a preferência no Mercado Pago
        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        // 5. Retorna o init_point (URL de checkout)
        if (sandboxMode) {
            return preference.getSandboxInitPoint(); // URL de teste
        } else {
            return preference.getInitPoint(); // URL de produção
        }
    }

    /**
     * Lógica de compensação de limite (Chamada pelo WEBHOOK/IPN).
     * @param restauranteId ID do restaurante que efetuou o pagamento.
     */
    @Transactional
    public void compensarLimite(Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado durante compensação."));

        if (!restaurante.getPlano().equals("GRATUITO")) {
            System.out.println("INFO: Pagamento de pacote recebido, mas o restaurante já está no plano " + restaurante.getPlano());
            return;
        }

        // Subtrai 10 do contador atual para liberar a capacidade de pedidos
        // Usamos Math.max(0, ...) para garantir que o contador não fique negativo
        restaurante.setPedidosMesAtual(Math.max(0, restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE));

        restauranteRepository.save(restaurante);
    }

    /**
     * NOVO: Gera a URL de checkout para um pedido público, usando um UUID temporário.
     */
    public String gerarUrlPagamentoPedidoPublico(UUID uuidPedido, BigDecimal totalPedido) throws MPException, MPApiException {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Pedido Delivery #"+uuidPedido.toString().substring(0, 8))
                .quantity(1)
                .unitPrice(totalPedido)
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("uuid_pedido", uuidPedido.toString());
        metadata.put("tipo_produto", "PEDIDO_DELIVERY"); // NOVO TIPO DE PRODUTO

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(Collections.singletonList(item))
                .notificationUrl(notificationUrl)
                .metadata(metadata)
                .externalReference(uuidPedido.toString())
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        if (sandboxMode) {
            return preference.getSandboxInitPoint();
        } else {
            return preference.getInitPoint();
        }
    }


    /**
     * MÉTODO CRÍTICO CORRIGIDO: Processa a notificação do Mercado Pago (Webhook).
     * Adiciona a lógica de data de expiração para o CHURN.
     */
    @Transactional
    public void processarNotificacaoWebhook(String resourceId, String topic) throws MPException, MPApiException {
        if (!topic.equals("payment")) {
            System.out.println("INFO: Webhook recebido com tópico diferente de 'payment': " + topic);
            return;
        }

        PaymentClient paymentClient = new PaymentClient();

        // 1. Busca os detalhes do pagamento na API do MP
        Payment payment = paymentClient.get(Long.valueOf(resourceId));

        // 2. Verifica se o pagamento foi aprovado
        if (payment.getStatus().equals("approved")) {

            // 3. Extrai metadados
            Map<String, Object> metadata = payment.getMetadata();
            // Mercado Pago API retorna os metadados como Object, fazemos o cast
            Long restauranteId = (Long) metadata.get("restaurante_id");
            String tipoProduto = (String) metadata.get("tipo_produto");

            // --- Lógica para Pedido Público (Finaliza a ordem no banco) ---
            if (tipoProduto != null && tipoProduto.equals("PEDIDO_DELIVERY")) {
                UUID pedidoUuid = UUID.fromString((String) metadata.get("uuid_pedido"));

                if (pedidoUuid != null) {
                    // Assumimos PIX para pagamento online (simplificação do fluxo de demo)
                    pedidoService.finalizarPedidoAprovado(pedidoUuid, payment.getPaymentMethodId().equals("pix") ? TipoPagamento.PIX : TipoPagamento.CARTAO_CREDITO);
                    System.out.println("SUCESSO: Pagamento online de Pedido Delivery finalizado e enviado para preparo. UUID: " + pedidoUuid);
                }
                return; // Sai do webhook
            }
            // --- FIM LÓGICA DE PEDIDO PÚBLICO ---


            if (restauranteId == null || tipoProduto == null) {
                System.err.println("ERRO: Metadados críticos (restaurante_id ou tipo_produto) ausentes no pagamento " + resourceId);
                return;
            }

            Restaurante restaurante = restauranteRepository.findById(restauranteId).orElseThrow(() -> new RuntimeException("Restaurante não encontrado na compensação PRO."));

            // NOVO: Variável para rastrear a data de expiração
            LocalDateTime novaExpiracao = null;

            switch (tipoProduto) {
                case "PAY_PER_USE":
                    compensarLimite(restauranteId);
                    break;
                case "PLANO_DELIVERY_MENSAL":
                    restaurante.setPlano("DELIVERY_PRO");
                    restaurante.setDeliveryPro(true);
                    restaurante.setSalaoPro(false);
                    novaExpiracao = LocalDateTime.now().plusMonths(1); // Expira em 1 mês
                    break;
                case "PLANO_SALAO_MENSAL":
                    restaurante.setPlano("SALÃO_PDV");
                    restaurante.setDeliveryPro(false);
                    restaurante.setSalaoPro(true);
                    novaExpiracao = LocalDateTime.now().plusMonths(1); // Expira em 1 mês
                    break;
                case "PLANO_PREMIUM_MENSAL":
                    restaurante.setPlano("PREMIUM");
                    restaurante.setDeliveryPro(true);
                    restaurante.setSalaoPro(true);
                    novaExpiracao = LocalDateTime.now().plusMonths(1); // Expira em 1 mês
                    break;
                case "PLANO_PREMIUM_ANUAL":
                    restaurante.setPlano("PREMIUM");
                    restaurante.setDeliveryPro(true);
                    restaurante.setSalaoPro(true);
                    novaExpiracao = LocalDateTime.now().plusYears(1); // Expira em 1 ano
                    break;
                default:
                    System.out.println("INFO: Tipo de produto desconhecido: " + tipoProduto);
                    return;
            }

            // Lógica comum de atualização:
            restaurante.setPedidosMesAtual(0);
            restaurante.setDataExpiracaoPlano(novaExpiracao); // SALVA A DATA DE EXPIRAÇÃO
            restauranteRepository.save(restaurante);
            System.out.println("SUCESSO: Pagamento de " + tipoProduto + " aplicado para o restaurante " + restauranteId + " e expiração definida para: " + novaExpiracao);
        } else {
            System.out.println("INFO: Pagamento não aprovado para o ID: " + resourceId + ". Status: " + payment.getStatus());
        }
    }


    // MÉTODO GENÉRICO PARA CRIAR PREFERÊNCIA DE PLANO (RECORRÊNCIA SIMULADA)
    private String criarPreferenciaPlano(Long restauranteId, String planoNome, BigDecimal custo, String tipoProduto, String titulo) throws MPException, MPApiException {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(titulo)
                .quantity(1)
                .unitPrice(custo)
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("restaurante_id", restauranteId);
        metadata.put("tipo_produto", tipoProduto); // Tipo de produto que o Webhook usará

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(Collections.singletonList(item))
                .notificationUrl(notificationUrl)
                .metadata(metadata)
                .externalReference(restauranteId.toString() + "_" + planoNome)
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        if (sandboxMode) {
            return preference.getSandboxInitPoint();
        } else {
            return preference.getInitPoint();
        }
    }

    // NOVOS MÉTODOS DE UPGRADE
    public String gerarUrlUpgradeDeliveryMensal(Long restauranteId) throws MPException, MPApiException {
        return criarPreferenciaPlano(restauranteId, "DELIVERY_M", CUSTO_DELIVERY_PRO, "PLANO_DELIVERY_MENSAL", "Plano Delivery PRO (Pedidos Ilimitados) - Mensal");
    }

    public String gerarUrlUpgradeSalaoMensal(Long restauranteId) throws MPException, MPApiException {
        return criarPreferenciaPlano(restauranteId, "SALAO_M", CUSTO_SALAO_PDV, "PLANO_SALAO_MENSAL", "Plano Salão PDV (Mesas Ilimitadas) - Mensal");
    }

    public String gerarUrlUpgradePremiumMensal(Long restauranteId) throws MPException, MPApiException {
        return criarPreferenciaPlano(restauranteId, "PREMIUM_M", CUSTO_PREMIUM, "PLANO_PREMIUM_MENSAL", "Plano Premium (Delivery + Salão) - Mensal");
    }

    public String gerarUrlUpgradePremiumAnual(Long restauranteId) throws MPException, MPApiException {
        return criarPreferenciaPlano(restauranteId, "PREMIUM_A", CUSTO_PREMIUM_ANUAL, "PLANO_PREMIUM_ANUAL", "Plano Premium (Delivery + Salão) - Anual");
    }
}