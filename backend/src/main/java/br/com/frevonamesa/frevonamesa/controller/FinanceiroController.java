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
    public ResponseEntity<Void> receberNotificacaoMP(@RequestParam("id") String paymentId,
                                                     @RequestParam("topic") String topic) {

        // 🚨 CRÍTICO: Na vida real, esta rota faria a validação de segurança e buscaria o status do pagamento na API do MP.

        System.out.println("WEBHOOK MP RECEBIDO: ID=" + paymentId + ", Tópico=" + topic);

        // Simulação de Sucesso: Na vida real, se o status for 'APROVADO', você chamaria:
        // financeiroService.compensarLimite(restauranteId_obtido_do_webhook);

        return ResponseEntity.ok().build();
    }


    /**
     * Endpoint para iniciar o fluxo de Upgrade para o Plano PRO.
     * Agora trata as exceções obrigatórias do Mercado Pago.
     */
    @PostMapping("/upgrade-pro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarUpgradePro() {
        try {
            Long restauranteId = restauranteService.getRestauranteLogado().getId();

            // A chamada para o método que usa o SDK precisa ser protegida
            String upgradeUrl = financeiroService.gerarUrlUpgradePro(restauranteId);

            Map<String, String> response = Map.of("upgradeUrl", upgradeUrl);
            return ResponseEntity.ok(response);

        } catch (MPException | MPApiException e) {
            // Captura exceções lançadas pelo SDK do Mercado Pago
            return ResponseEntity.status(500).body("Erro ao gerar pagamento de upgrade: " + e.getMessage());
        } catch (RuntimeException e) {
            // Captura outras exceções de regra de negócio (como usuário não encontrado)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}