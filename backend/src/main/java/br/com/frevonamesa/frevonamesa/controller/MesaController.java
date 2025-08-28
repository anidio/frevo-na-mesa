package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.MesaClienteDTO;
import br.com.frevonamesa.frevonamesa.dto.MesaRequestDTO;
import br.com.frevonamesa.frevonamesa.dto.MesaStatusDTO;
import br.com.frevonamesa.frevonamesa.dto.PagamentoDTO;
import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.service.MesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    @Autowired
    private MesaService mesaService;

    @GetMapping
    public List<Mesa> listarMesas() {
        return mesaService.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mesa> buscarMesaPorId(@PathVariable Long id) {
        return mesaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criarMesa(@RequestBody MesaRequestDTO mesaDTO) {
        try {
            Mesa novaMesa = mesaService.criarMesa(mesaDTO);
            return ResponseEntity.status(201).body(novaMesa);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Mesa> atualizarStatusMesa(@PathVariable Long id, @RequestBody MesaStatusDTO statusDTO) {
        try {
            Mesa mesaAtualizada = mesaService.atualizarStatus(id, statusDTO.getStatus());
            return ResponseEntity.ok(mesaAtualizada);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/pagar")
    public ResponseEntity<Mesa> pagarMesa(@PathVariable Long id, @RequestBody PagamentoDTO pagamentoDTO) {
        try {
            Mesa mesaPaga = mesaService.processarPagamento(id, pagamentoDTO.getTipoPagamento());
            return ResponseEntity.ok(mesaPaga);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/{id}/cliente")
    public ResponseEntity<Mesa> atualizarNomeCliente(@PathVariable Long id, @RequestBody MesaClienteDTO clienteDTO) {
        try {
            Mesa mesaAtualizada = mesaService.atualizarNomeCliente(id, clienteDTO.getNomeCliente());
            return ResponseEntity.ok(mesaAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> atualizarNumeroMesa(@PathVariable Long id, @RequestBody MesaRequestDTO mesaDTO) {
        try {
            Mesa mesaAtualizada = mesaService.atualizarNumeroMesa(id, mesaDTO.getNumero());
            return ResponseEntity.ok(mesaAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}