package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.RelatorioDiarioDTO;
import br.com.frevonamesa.frevonamesa.service.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
@CrossOrigin(origins = "http://localhost:5173")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    @GetMapping("/hoje")
    public RelatorioDiarioDTO getRelatorioDiario() {
        return relatorioService.gerarRelatorioDoDia();
    }
}
