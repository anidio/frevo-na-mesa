// backend/src/main/java/br/com/frevonamesa/frevonamesa/controller/FinanceiroController.java

package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.service.FinanceiroService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException; // Import necessário
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Garantir que está importado
import org.springframework.web.bind.annotation.*;
import java.util.Map; // Garantir que está importado

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    @Autowired
    private FinanceiroService financeiroService;

    @Autowired
    private RestauranteService restauranteService;

    // --- >> INÍCIO: NOVOS ENDPOINTS STRIPE CONNECT << ---

    /**
     * Endpoint para o administrador do restaurante iniciar o processo de
     * criação/conexão da sua conta Stripe Connect.
     * Retorna a URL de onboarding gerada pelo Stripe.
     */
    @PostMapping("/connect/onboard")
    @PreAuthorize("hasRole('ADMIN')") // Apenas o admin do restaurante pode fazer isso
    public ResponseEntity<?> gerarLinkOnboardingConnect() {
        try {
            Restaurante restaurante = restauranteService.getRestauranteLogado();
            String onboardingUrl = financeiroService.createConnectAccountLink(restaurante.getId());
            // Retorna a URL para o frontend redirecionar o usuário
            return ResponseEntity.ok(Map.of("onboardingUrl", onboardingUrl));
        } catch (StripeException e) {
            System.err.println("ERRO STRIPE ao gerar link onboarding: " + e.getMessage());
            // Retorna um erro 500 para o frontend
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao conectar com o sistema de pagamentos: " + e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("ERRO ao gerar link onboarding: " + e.getMessage());
            // Retorna um erro 400 ou 500 dependendo da causa
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao gerar link de conexão: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para onde o Stripe redireciona após o onboarding.
     * O frontend pode usar essa rota para saber que o processo terminou
     * e então chamar refreshProfile() para atualizar o status da conexão.
     * Opcionalmente, pode chamar verifyConnectedAccount no backend.
     */
    @GetMapping("/connect/return")
    // Pode ser @PreAuthorize("isAuthenticated()") se não fizer escrita,
    // ou manter ADMIN se chamar verify que pode potencialmente salvar algo.
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> handleConnectReturn() {
        try {
            Restaurante restaurante = restauranteService.getRestauranteLogado();
            // Opcional: Chamar a verificação aqui para logar o status imediatamente
            boolean isVerified = financeiroService.verifyConnectedAccount(restaurante.getId());
            System.out.println("Retorno do Onboarding Connect para Restaurante ID: " + restaurante.getId() + ". Verificado? " + isVerified);

            // Apenas retorna uma mensagem de sucesso. O frontend deve atualizar a UI.
            return ResponseEntity.ok(Map.of("message", "Processo de conexão com Stripe concluído. Verifique o status na página financeira."));
        } catch (StripeException e) {
            System.err.println("ERRO STRIPE no retorno do Connect: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao verificar status da conta Stripe: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("ERRO no retorno do Connect: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao processar retorno do Stripe Connect."));
        }
    }

    // --- >> FIM: NOVOS ENDPOINTS STRIPE CONNECT << ---


    // --- Endpoints Existentes (Planos da Plataforma, Webhook, etc.) ---

    // Endpoint Pay-per-Use (Plataforma)
    @PostMapping("/iniciar-pagamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAIXA')")
    public ResponseEntity<?> iniciarPagamentoPacotePedidos() { // Renomeado para clareza
        try {
            Restaurante restaurante = restauranteService.getRestauranteLogado();
            // A lógica de verificar se já é PRO ou gratuito está no service compensarLimite
            String paymentUrl = financeiroService.gerarUrlPagamento(restaurante.getId());
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (StripeException e) {
            System.err.println("ERRO STRIPE ao gerar pagamento PayPerUse: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao gerar pagamento: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint Webhook Stripe (sem alterações)
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> receberNotificacaoStripe(@RequestBody(required = false) String payload, // Torna opcional para logar mesmo se vazio
                                                           @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) { // Torna opcional para logar
        if (payload == null || payload.isEmpty()) {
            System.err.println("WARN WEBHOOK: Payload vazio recebido.");
            return ResponseEntity.badRequest().body("Webhook Error: Payload vazio.");
        }
        if (sigHeader == null || sigHeader.isEmpty()) {
            System.err.println("WARN WEBHOOK: Cabeçalho Stripe-Signature ausente.");
            // Em produção, você DEVE retornar erro aqui. Em testes, pode ser mais permissivo.
            // return ResponseEntity.badRequest().body("Webhook Error: Assinatura ausente.");
        }
        try {
            financeiroService.processarWebhookStripe(payload, sigHeader);
            return ResponseEntity.ok().body("Success"); // Retorna 200 OK para o Stripe
        } catch (SignatureVerificationException e) {
            System.err.println("ERRO STRIPE na assinatura do Webhook: " + e.getMessage());
            return ResponseEntity.status(400).body("Webhook Error: Assinatura inválida.");
        } catch (StripeException e) {
            System.err.println("ERRO STRIPE no processamento do Webhook: " + e.getMessage());
            return ResponseEntity.status(500).body("Erro ao processar webhook Stripe: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("ERRO INTERNO no Webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno ao processar webhook: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERRO INESPERADO no Webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro inesperado no servidor.");
        }
    }


    // --- ENDPOINTS DE UPGRADE (Assinaturas da Plataforma) ---

    @PostMapping("/upgrade/delivery-mensal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradeDeliveryMensal() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();
            String upgradeUrl = financeiroService.gerarUrlUpgradeDeliveryMensal(restauranteId);
            return ResponseEntity.ok(Map.of("upgradeUrl", upgradeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao gerar link de assinatura: " + e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao gerar link de assinatura: " + e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao gerar link de assinatura: " + e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao gerar link de assinatura: " + e.getMessage()));
        }
    }

    // --- ENDPOINT DE STATUS (Plano da Plataforma) ---
    @GetMapping("/status-plano")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatusPlanoDetalhado() {
        try {
            Map<String, Object> status = restauranteService.getStatusPlanoDetalhado();
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) { // Captura UsernameNotFoundException também
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // --- ENDPOINT PARA O PORTAL DO CLIENTE (Assinaturas da Plataforma) ---
    @PostMapping("/portal-session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> criarSessaoPortal() {
        try {
            String portalUrl = financeiroService.criarSessaoPortalCliente();
            return ResponseEntity.ok(Map.of("portalUrl", portalUrl));
        } catch (StripeException e) {
            System.err.println("ERRO STRIPE ao criar sessão do portal: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao conectar com o portal de pagamentos: " + e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("ERRO ao criar sessão do portal: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao gerar acesso ao portal: " + e.getMessage()));
        }
    }

} // Fim da classe FinanceiroController