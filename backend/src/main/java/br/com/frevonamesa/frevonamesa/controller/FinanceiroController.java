package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.service.FinanceiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    @Autowired
    private FinanceiroService financeiroService;

    @PostMapping("/comprar-pedidos-extras")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAIXA')")
    public ResponseEntity<?> comprarPedidosExtras() {
        try {
            financeiroService.comprarPacoteDePedidosExtras();
            return ResponseEntity.ok("Pacote de pedidos extras ativado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upgrade-pro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> upgradePro() {
        try {
            financeiroService.upgradeParaDeliveryPro();
            return ResponseEntity.ok("Upgrade para Plano Delivery PRO realizado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}