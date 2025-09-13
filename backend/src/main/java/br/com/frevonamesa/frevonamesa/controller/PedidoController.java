package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.PedidoDeliveryRequestDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoFilaDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoRequestDTO;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.StatusPedido;
import br.com.frevonamesa.frevonamesa.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping("/mesa")
    public ResponseEntity<Pedido> criarPedidoDeMesa(@RequestBody PedidoRequestDTO pedidoDTO) {
        try {
            Pedido novoPedido = pedidoService.criarPedido(pedidoDTO);
            return ResponseEntity.ok(novoPedido);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/mesa/pendentes")
    public ResponseEntity<List<PedidoFilaDTO>> getPedidosDeMesaPendentes() {
        List<PedidoFilaDTO> pedidos = pedidoService.listarPedidosDeMesaPendentes();
        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("/mesa/{id}/confirmar")
    public ResponseEntity<Pedido> confirmarPedidoDeMesa(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoService.confirmarPedidoDeMesa(id);
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/delivery")
    public ResponseEntity<Pedido> criarPedidoDelivery(@RequestBody PedidoDeliveryRequestDTO pedidoDTO) {
        try {
            Pedido novoPedido = pedidoService.criarPedidoDelivery(pedidoDTO);
            return ResponseEntity.ok(novoPedido);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/delivery")
    public ResponseEntity<Map<StatusPedido, List<Pedido>>> getPedidosDelivery() {
        Map<StatusPedido, List<Pedido>> pedidos = pedidoService.listarPedidosDeliveryPorStatus();
        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("/delivery/{id}/status")
    public ResponseEntity<Pedido> atualizarStatusPedidoDelivery(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            StatusPedido novoStatus = StatusPedido.valueOf(body.get("status"));
            Pedido pedidoAtualizado = pedidoService.atualizarStatusPedidoDelivery(id, novoStatus);
            return ResponseEntity.ok(pedidoAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/delivery/finalizados")
    public ResponseEntity<List<Pedido>> getPedidosDeliveryFinalizados() {
        List<Pedido> pedidos = pedidoService.listarUltimos10Finalizados();
        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("/delivery/{id}/imprimir")
    public ResponseEntity<Pedido> imprimirPedidoDelivery(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoService.imprimirPedidoDelivery(id);
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/delivery/pendentes")
    public ResponseEntity<List<Pedido>> getPedidosDeliveryPendentes() {
        return ResponseEntity.ok(pedidoService.listarPedidosDeliveryPendentes());
    }
}