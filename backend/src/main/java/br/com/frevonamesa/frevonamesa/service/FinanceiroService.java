// backend/src/main/java/br/com/frevonamesa/frevonamesa/service/FinanceiroService.java

package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
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
import org.springframework.stereotype.Service;
// NOVOS IMPORTS
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
// FIM NOVOS IMPORTS

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class FinanceiroService {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

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
    private static final BigDecimal CUSTO_PLANO_PRO = new BigDecimal("69.90"); // NOVO: Preço do Plano PRO
    private static final int LIMITE_MESAS_PRO = 50;

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
     * NOVO MÉTODO CRÍTICO: Processa a notificação do Mercado Pago (Webhook).
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

            if (restauranteId == null || tipoProduto == null) {
                System.err.println("ERRO: Metadados críticos (restaurante_id ou tipo_produto) ausentes no pagamento " + resourceId);
                return;
            }

            // 4. Lógica de Compensação baseada no tipo de produto
            if (tipoProduto.equals("PAY_PER_USE")) {
                // Chama o método de compensação
                compensarLimite(restauranteId);
                System.out.println("SUCESSO: Compensação de limite PAY_PER_USE aplicada para o restaurante " + restauranteId);
            } else if (tipoProduto.equals("PLANO_PRO")) {
                // Atualiza o plano para PRO
                Restaurante restaurante = restauranteRepository.findById(restauranteId)
                        .orElseThrow(() -> new RuntimeException("Restaurante não encontrado na compensação PRO."));
                restaurante.setPlano("DELIVERY_PRO");
                // Zera o contador de pedidos
                restaurante.setPedidosMesAtual(0);
                restauranteRepository.save(restaurante);
                System.out.println("SUCESSO: Upgrade para DELIVERY_PRO aplicado para o restaurante " + restauranteId);
            } else {
                System.out.println("INFO: Tipo de produto desconhecido: " + tipoProduto);
            }

        } else {
            System.out.println("INFO: Pagamento não aprovado para o ID: " + resourceId + ". Status: " + payment.getStatus());
        }
    }


    /**
     * NOVO MÉTODO: Gera a URL de Upgrade para o Plano PRO.
     */
    public String gerarUrlUpgradePro(Long restauranteId) throws MPException, MPApiException {
        // 1. Define o item (Assinatura)
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Assinatura Plano Delivery PRO - Mensal")
                .quantity(1)
                .unitPrice(CUSTO_PLANO_PRO)
                .build();

        // 2. Metadados
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("restaurante_id", restauranteId);
        metadata.put("tipo_produto", "PLANO_PRO");

        // 3. Cria a Preferência
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(Collections.singletonList(item))
                .notificationUrl(notificationUrl)
                .metadata(metadata)
                .externalReference(restauranteId.toString() + "_PRO")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        // 4. Retorna a URL
        if (sandboxMode) {
            return preference.getSandboxInitPoint();
        } else {
            return preference.getInitPoint();
        }
    }
}