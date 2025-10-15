package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.AreaEntregaDTO;
import br.com.frevonamesa.frevonamesa.model.AreaEntrega;
import br.com.frevonamesa.frevonamesa.service.AreaEntregaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas-entrega")
@PreAuthorize("hasRole('ADMIN')")
public class AreaEntregaController {

    @Autowired
    private AreaEntregaService areaEntregaService;

    @GetMapping
    public List<AreaEntrega> listarAreas() {
        return areaEntregaService.listarTodas();
    }

    @PostMapping
    public ResponseEntity<AreaEntrega> criarArea(@RequestBody AreaEntregaDTO dto) {
        AreaEntrega novaArea = areaEntregaService.criar(dto);
        return ResponseEntity.status(201).body(novaArea);
    }

    // NOVO: Atualizar faixa de distância
    @PutMapping("/{id}")
    public ResponseEntity<AreaEntrega> atualizarArea(@PathVariable Long id, @RequestBody AreaEntregaDTO dto) {
        try {
            AreaEntrega atualizada = areaEntregaService.atualizar(id, dto);
            return ResponseEntity.ok(atualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // NOVO: Deletar faixa de distância
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarArea(@PathVariable Long id) {
        try {
            areaEntregaService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}