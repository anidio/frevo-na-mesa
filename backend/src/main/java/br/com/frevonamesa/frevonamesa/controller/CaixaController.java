package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.CaixaDashboardDTO;
import br.com.frevonamesa.frevonamesa.service.CaixaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    @Autowired
    private CaixaService caixaService;

    @GetMapping("/dashboard")
    public CaixaDashboardDTO getDashboard() {
        return caixaService.getDashboardInfo();
    }

    @PostMapping("/fechar")
    public ResponseEntity<Void> fecharCaixa() {
        caixaService.fecharCaixa();
        return ResponseEntity.ok().build();
    }
}
