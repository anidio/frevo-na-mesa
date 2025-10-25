package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.service.FinanceiroService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException; // Import necessário
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

    // Endpoint Pay-per-Use (sem alterações)
    @PostMapping("/iniciar-pagamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAIXA')")
    public ResponseEntity<?> iniciarPagamento() {
        try {
            Restaurante restaurante = restauranteService.getRestauranteLogado();

            // A lógica de verificar se já é PRO pode ser feita no service se preferir
            if (!restaurante.getPlano().equals("GRATUITO")) {
                // Considerar se pay-per-use ainda faz sentido se o plano não for gratuito
                // return ResponseEntity.badRequest().body("Usuário já tem plano PRO ou pacote ativo.");
            }

            String paymentUrl = financeiroService.gerarUrlPagamento(restaurante.getId());
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));

        } catch (StripeException e) {
            System.err.println("ERRO STRIPE ao gerar pagamento PayPerUse: " + e.getMessage());
            return ResponseEntity.status(500).body("Erro ao gerar pagamento: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint Webhook Stripe (sem alterações)
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> receberNotificacaoStripe(@RequestBody String payload,
                                                           @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            financeiroService.processarWebhookStripe(payload, sigHeader);
            return ResponseEntity.ok().body("Success");
        } catch (SignatureVerificationException e) { // Mais específico para assinatura inválida
            System.err.println("ERRO STRIPE na assinatura do Webhook: " + e.getMessage());
            return ResponseEntity.status(400).body("Webhook Error: Assinatura inválida.");
        } catch (StripeException e) { // Outros erros da API Stripe
            System.err.println("ERRO STRIPE no processamento do Webhook: " + e.getMessage());
            return ResponseEntity.status(500).body("Erro ao processar webhook Stripe: " + e.getMessage());
        } catch (RuntimeException e) { // Erros internos da sua aplicação
            System.err.println("ERRO INTERNO no Webhook: " + e.getMessage());
            e.printStackTrace(); // Logar stacktrace pode ajudar
            return ResponseEntity.status(500).body("Erro interno ao processar webhook: " + e.getMessage());
        } catch (Exception e) { // Captura genérica para erros inesperados
            System.err.println("ERRO INESPERADO no Webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro inesperado no servidor.");
        }
    }


    // --- ENDPOINTS DE UPGRADE (Checkout - sem alterações na chamada) ---

    @PostMapping("/upgrade/delivery-mensal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradeDeliveryMensal() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradeDeliveryMensal(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao gerar link de assinatura: " + e.getMessage());
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
            return ResponseEntity.badRequest().body("Erro ao gerar link de assinatura: " + e.getMessage());
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
            return ResponseEntity.badRequest().body("Erro ao gerar link de assinatura: " + e.getMessage());
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
            return ResponseEntity.badRequest().body("Erro ao gerar link de assinatura: " + e.getMessage());
        }
    }

    // --- ENDPOINT DE STATUS (sem alterações) ---
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

    // --- NOVO ENDPOINT PARA O PORTAL DO CLIENTE ---
    @PostMapping("/portal-session")
    @PreAuthorize("hasRole('ADMIN')") // Garante que apenas o admin logado acesse
    public ResponseEntity<?> criarSessaoPortal() {
        try {
            String portalUrl = financeiroService.criarSessaoPortalCliente();
            // Retorna a URL para o frontend em um JSON
            return ResponseEntity.ok(Map.of("portalUrl", portalUrl));
        } catch (StripeException e) {
            System.err.println("ERRO STRIPE ao criar sessão do portal: " + e.getMessage());
            return ResponseEntity.status(500).body("Erro ao conectar com o portal de pagamentos: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("ERRO ao criar sessão do portal: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao gerar acesso ao portal: " + e.getMessage());
        }
    }
    // --- FIM DO NOVO ENDPOINT ---
}