package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.service.FinanceiroService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
// IMPORTS DO MERCADO PAGO REMOVIDOS
import com.stripe.exception.StripeException; // NOVO IMPORT
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
     * Endpoint para iniciar o pagamento do Pay-per-Use (Stripe Payment - Avulso).
     * Retorna a URL de Checkout do Stripe.
     */
    @PostMapping("/iniciar-pagamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAIXA')")
    public ResponseEntity<?> iniciarPagamento() {
        try {
            Restaurante restaurante = restauranteService.getRestauranteLogado();

            if (!restaurante.getPlano().equals("GRATUITO")) {
                return ResponseEntity.badRequest().body("Usuário já tem pedidos ilimitados.");
            }

            // O service agora retorna a URL de Checkout do Stripe Session
            String paymentUrl = financeiroService.gerarUrlPagamento(restaurante.getId());

            // Retorna a URL para o Front-end redirecionar o usuário
            Map<String, String> response = Map.of("paymentUrl", paymentUrl);
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            // Captura as exceções do Stripe SDK
            System.err.println("ERRO STRIPE ao gerar pagamento: " + e.getMessage());
            return ResponseEntity.status(500).body("Erro ao gerar pagamento Stripe: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * NOVO: Endpoint do Webhook para receber as notificações do Stripe.
     * Esta rota deve ser exposta publicamente e não exige autenticação.
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> receberNotificacaoStripe(@RequestBody String payload,
                                                           @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // A validação de segurança e o processamento do evento são delegados ao service.
            financeiroService.processarWebhookStripe(payload, sigHeader);

            // Retorna 200 OK para o Stripe, confirmando o recebimento
            return ResponseEntity.ok().body("Success");

        } catch (StripeException e) {
            // Erro na API do Stripe ou na validação do Webhook
            System.err.println("ERRO STRIPE na assinatura do Webhook: " + e.getMessage());
            return ResponseEntity.status(400).body("Webhook Error: Assinatura inválida.");
        } catch (RuntimeException e) {
            // Erros internos de regra de negócio, como restaurante não encontrado
            System.err.println("ERRO INTERNO no Webhook: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERRO INESPERADO no Webhook: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    // REMOVIDO: O endpoint /webhook/mp foi removido.

    // --- ENDPOINTS DE UPGRADE (Stripe Subscription) ---

    @PostMapping("/upgrade/delivery-mensal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradeDeliveryMensal() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradeDeliveryMensal(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            // Captura StripeException ou RuntimeException
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

    // --- ENDPOINT DE STATUS (MANTIDO) ---
    @GetMapping("/status-plano")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatusPlanoDetalhado() {
        try {
            Map<String, Object> status = restauranteService.getStatusPlanoDetalhado();
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}