package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.CardapioPublicoDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoClienteDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoDeliveryClienteDTO;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.service.PedidoService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/publico")
public class PublicController {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private PedidoService pedidoService;

    @GetMapping("/cardapio/{restauranteId}")
    public ResponseEntity<CardapioPublicoDTO> getCardapioPublico(@PathVariable Long restauranteId) {
        try {
            CardapioPublicoDTO cardapio = restauranteService.getCardapioPublico(restauranteId);
            return ResponseEntity.ok(cardapio);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/pedido/{uuid}/rastrear")
    public ResponseEntity<Pedido> rastrearPedido(@PathVariable UUID uuid) {
        Pedido pedido = pedidoService.rastrearPedidoPorUuid(uuid);
        if (pedido != null) {
            return ResponseEntity.ok(pedido);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/pedido/mesa")
    public ResponseEntity<Pedido> criarPedidoMesaCliente(@RequestBody PedidoClienteDTO pedidoDTO) {
        try {
            Pedido novoPedido = pedidoService.criarPedidoMesaCliente(pedidoDTO);
            return ResponseEntity.status(201).body(novoPedido);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/pedido/delivery")
    public ResponseEntity<Pedido> criarPedidoDeliveryCliente(@RequestBody PedidoDeliveryClienteDTO pedidoDTO) {
        try {
            Pedido novoPedido = pedidoService.criarPedidoDeliveryCliente(pedidoDTO);
            return ResponseEntity.status(201).body(novoPedido);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}