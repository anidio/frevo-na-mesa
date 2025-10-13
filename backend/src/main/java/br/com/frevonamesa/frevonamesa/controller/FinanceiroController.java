// backend/src/main/java/br/com/frevonamesa/frevonamesa/controller/FinanceiroController.java

package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.service.FinanceiroService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    @Autowired
    private FinanceiroService financeiroService;

    @Autowired
    private RestauranteService restauranteService;

    /**
     * Endpoint chamado pelo Front-end para iniciar o fluxo de pagamento do Pay-per-Use.
     * Retorna a URL de Checkout do Mercado Pago.
     */
    @PostMapping("/iniciar-pagamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAIXA')")
    public ResponseEntity<?> iniciarPagamento() {
        try {
            Restaurante restaurante = restauranteService.getRestauranteLogado();
            // Verifica se o usuário tem o plano correto para evitar chamadas desnecessárias à API do MP
            if (!restaurante.getPlano().equals("GRATUITO")) {
                return ResponseEntity.badRequest().body("Usuário já tem pedidos ilimitados.");
            }

            String paymentUrl = financeiroService.gerarUrlPagamento(restaurante.getId());

            // Retorna a URL para o Front-end redirecionar o usuário
            Map<String, String> response = Map.of("paymentUrl", paymentUrl);
            return ResponseEntity.ok(response);

        } catch (MPException | MPApiException e) {
            return ResponseEntity.status(500).body("Erro ao gerar pagamento: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Webhook (IPN) do Mercado Pago para notificar sobre a mudança de status do pagamento.
     * Rota de recebimento de notificação.
     */
    @PostMapping("/webhook/mp")
    public ResponseEntity<Void> receberNotificacaoMP(@RequestParam("id") String resourceId,
                                                     @RequestParam("topic") String topic) {

        System.out.println("WEBHOOK MP RECEBIDO: ID=" + resourceId + ", Tópico=" + topic);

        try {
            // A validação de segurança e a busca pelo status ocorrem DENTRO do service.
            financeiroService.processarNotificacaoWebhook(resourceId, topic);

            // Retorna 200 OK para o Mercado Pago, confirmando que a notificação foi recebida.
            return ResponseEntity.ok().build();

        } catch (MPException | MPApiException e) {
            // Em caso de falha (ex: API do MP inacessível), o MP irá re-enviar
            System.err.println("ERRO CRÍTICO ao processar webhook MP: " + e.getMessage());
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            System.err.println("ERRO INESPERADO ao processar webhook: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }


    @PostMapping("/upgrade/delivery-mensal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradeDeliveryMensal() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradeDeliveryMensal(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao gerar pagamento: " + e.getMessage());
        }
    }

    @PostMapping("/upgrade/salao-mensal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradeSalaoMensal() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradeSalaoMensal(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao gerar pagamento: " + e.getMessage());
        }
    }

    @PostMapping("/upgrade/premium-mensal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradePremiumMensal() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradePremiumMensal(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao gerar pagamento: " + e.getMessage());
        }
    }

    @PostMapping("/upgrade/premium-anual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradePremiumAnual() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradePremiumAnual(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao gerar pagamento: " + e.getMessage());
        }
    }

    @GetMapping("/status-plano")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatusPlanoDetalhado() {
        try {
            Map<String, Object> status = restauranteService.getStatusPlanoDetalhado();
            // Adicione aqui os status dos novos booleanos se desejar (opcional, mas bom para debug)
            // status.put("isDeliveryPro", restauranteService.getRestauranteLogado().isDeliveryPro());
            // status.put("isSalaoPro", restauranteService.getRestauranteLogado().isSalaoPro());
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}