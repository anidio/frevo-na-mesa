package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.CardapioPublicoDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoClienteDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoDeliveryClienteDTO;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.TipoPagamento;
import br.com.frevonamesa.frevonamesa.service.AreaEntregaService;
import br.com.frevonamesa.frevonamesa.service.PedidoService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/publico")
public class PublicController {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private AreaEntregaService areaEntregaService;

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

    @PostMapping("/pedido/delivery") // Reabilitado para Salvar Pedido Offline
    public ResponseEntity<Pedido> criarPedidoDeliveryCliente(@RequestBody PedidoDeliveryClienteDTO pedidoDTO,
                                                             @RequestParam(value = "pagamento", required = false) String pagamento) {
        try {
            // O tipo de pagamento é passado como parâmetro na URL do frontend.
            TipoPagamento tipoPgto = TipoPagamento.valueOf(pagamento);

            Pedido novoPedido = pedidoService.salvarPedidoDeliveryCliente(pedidoDTO, tipoPgto);
            return ResponseEntity.status(201).body(novoPedido);
        } catch (IllegalArgumentException e) {
            // Se o 'pagamento' não for válido (ex: PIX), ele cai aqui.
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // O endpoint /pedido/delivery FOI REMOVIDO/SUBSTITUÍDO
    // pela lógica de Pagamento Online Integrado no /pagar/delivery abaixo.

    @PostMapping("/pagar/delivery") // NOVO ENDPOINT DE PAGAMENTO PÚBLICO
    public ResponseEntity<?> iniciarPagamentoDeliveryCliente(@RequestBody PedidoDeliveryClienteDTO pedidoDTO) {
        try {
            // CORREÇÃO: O novo método no PedidoService retorna a URL do Mercado Pago
            String paymentUrl = pedidoService.iniciarPagamentoDeliveryCliente(pedidoDTO);

            // O Controller retorna a URL dentro de um objeto JSON
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));

        } catch (RuntimeException e) {
            // Captura RuntimeExceptions de regras de negócio (ex: produto não encontrado, total 0)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Captura exceções do Mercado Pago (MPException/MPApiException)
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao iniciar pagamento: " + e.getMessage()));
        }
    }

    @GetMapping("/frete/{restauranteId}/{cep}")
    public ResponseEntity<Map<String, Object>> calcularFrete(
            @PathVariable Long restauranteId,
            @PathVariable String cep) {

        BigDecimal taxa = areaEntregaService.calcularTaxa(restauranteId, cep);

        if (taxa.compareTo(new BigDecimal("-1.00")) == 0) {
            // Código -1.00 significa que o CEP não foi encontrado em nenhuma área
            return ResponseEntity.badRequest().body(Map.of("error", "Entrega indisponível para este CEP."));
        }

        if (taxa.compareTo(new BigDecimal("-2.00")) == 0) {
            // Código -2.00 significa que o CEP é inválido
            return ResponseEntity.badRequest().body(Map.of("error", "CEP inválido."));
        }

        // Retorna a taxa de entrega
        return ResponseEntity.ok(Map.of("taxaEntrega", taxa));
    }
}