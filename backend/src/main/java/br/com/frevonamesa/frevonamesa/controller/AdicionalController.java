package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.model.Adicional;
import br.com.frevonamesa.frevonamesa.service.AdicionalService; // Crie este service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adicionais")
public class AdicionalController {

    @Autowired
    private AdicionalService adicionalService;

    @GetMapping
    public List<Adicional> listarAdicionais() {
        return adicionalService.listarPorRestaurante();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Adicional criarAdicional(@RequestBody Adicional adicional) {
        return adicionalService.salvar(adicional);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Adicional atualizarAdicional(@PathVariable Long id, @RequestBody Adicional adicional) {
        return adicionalService.atualizar(id, adicional);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletarAdicional(@PathVariable Long id) {
        adicionalService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}