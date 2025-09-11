package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.PedidoFilaDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoRequestDTO;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping // Responde a requisições do tipo POST
    public ResponseEntity<Pedido> criarPedido(@RequestBody PedidoRequestDTO pedidoDTO) {
        try {
            Pedido novoPedido = pedidoService.criarPedido(pedidoDTO);
            return ResponseEntity.ok(novoPedido);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/nao-impressos")
    public ResponseEntity<List<PedidoFilaDTO>> getPedidosNaoImpressos() {
        List<PedidoFilaDTO> pedidos = pedidoService.listarPedidosNaoImpressos();
        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("/{id}/marcar-impresso")
    public ResponseEntity<Pedido> marcarPedidoComoImpresso(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoService.marcarComoImpresso(id);
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
