// backend/src/main/java/br/com/frevonamesa/frevonamesa/controller/PublicController.java

package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.CardapioPublicoDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoClienteDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoDeliveryClienteDTO;
import br.com.frevonamesa.frevonamesa.exception.PedidoLimitException; // Importar exceção
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.Restaurante; // Importar Restaurante
import br.com.frevonamesa.frevonamesa.model.TipoPagamento;
import br.com.frevonamesa.frevonamesa.service.AreaEntregaService;
import br.com.frevonamesa.frevonamesa.service.PedidoService;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository; // Importar repositório
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Importar HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory


import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional; // Importar Optional
import java.util.UUID;

@RestController
@RequestMapping("/api/publico")
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class); // Adiciona logger

    @Autowired
    private RestauranteService restauranteService; // Usado para getCardapioPublico

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private AreaEntregaService areaEntregaService; // Para calcular taxa

    @Autowired
    private RestauranteRepository restauranteRepository; // Para buscar o restaurante

    @GetMapping("/cardapio/{restauranteId}")
    public ResponseEntity<CardapioPublicoDTO> getCardapioPublico(@PathVariable Long restauranteId) {
        try {
            CardapioPublicoDTO cardapio = restauranteService.getCardapioPublico(restauranteId);
            return ResponseEntity.ok(cardapio);
        } catch (RuntimeException e) {
            logger.error("Erro ao buscar cardápio público para restaurante ID {}: {}", restauranteId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/pedido/{uuid}/rastrear")
    public ResponseEntity<Pedido> rastrearPedido(@PathVariable UUID uuid) {
        Pedido pedido = pedidoService.rastrearPedidoPorUuid(uuid);
        if (pedido != null) {
            return ResponseEntity.ok(pedido);
        }
        logger.warn("Tentativa de rastrear pedido com UUID não encontrado: {}", uuid);
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/pedido/mesa")
    public ResponseEntity<?> criarPedidoMesaCliente(@RequestBody PedidoClienteDTO pedidoDTO) {
        try {
            Pedido novoPedido = pedidoService.criarPedidoMesaCliente(pedidoDTO);
            logger.info("Pedido de cliente para mesa criado com sucesso. Pedido ID: {}", novoPedido.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(novoPedido);
        } catch (RuntimeException e) {
            logger.error("Erro ao criar pedido de cliente para mesa: {}", e.getMessage(), e);
            // Retorna a mensagem de erro específica (ex: "Mesa não encontrada")
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar pedido de cliente para mesa: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erro interno no servidor."));
        }
    }

    // --- Endpoint para PAGAMENTO NA ENTREGA (salvar pedido offline) ---
    @PostMapping("/pedido/delivery")
    public ResponseEntity<?> criarPedidoDeliveryClienteOffline(@RequestBody PedidoDeliveryClienteDTO pedidoDTO,
                                                               @RequestParam(value = "pagamento") String pagamentoStr) { // Pagamento é obrigatório aqui
        logger.info("Recebida requisição para criar pedido offline (pagamento na entrega). Restaurante ID: {}", pedidoDTO.getRestauranteId());
        try {
            // 1. Valida o tipo de pagamento (não pode ser ONLINE aqui)
            TipoPagamento tipoPgto = TipoPagamento.valueOf(pagamentoStr.toUpperCase());
            if (tipoPgto == TipoPagamento.CARTAO_CREDITO && pagamentoStr.equalsIgnoreCase("ONLINE")) { // Evita confusão se front mandar "ONLINE"
                logger.warn("Tentativa de criar pedido offline com tipo de pagamento ONLINE.");
                return ResponseEntity.badRequest().body(Map.of("error", "Use o endpoint /pagar/delivery para pagamentos online."));
            }

            // 2. Busca o restaurante para verificar modo de cálculo de frete
            Restaurante restaurante = restauranteRepository.findById(pedidoDTO.getRestauranteId())
                    .orElseThrow(() -> new RuntimeException("Restaurante não encontrado."));

            // 3. Calcula a taxa de entrega ANTES de salvar
            BigDecimal taxaEntregaCalculada;
            if (restaurante.isCalculoHaversineAtivo()) {
                if (pedidoDTO.getCepCliente() == null || pedidoDTO.getCepCliente().isBlank()) {
                    throw new RuntimeException("CEP do cliente é obrigatório para cálculo de frete por distância.");
                }
                logger.debug("Calculando taxa Haversine para CEP {}", pedidoDTO.getCepCliente());
                taxaEntregaCalculada = areaEntregaService.calcularTaxa(restaurante.getId(), pedidoDTO.getCepCliente());
                // Validação da taxa calculada (lança exceção se inválida/fora de área)
                if (taxaEntregaCalculada.compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("Não foi possível calcular a taxa de entrega para o CEP informado ou está fora da área de cobertura.");
                }
                logger.debug("Taxa Haversine calculada: {}", taxaEntregaCalculada);
            } else {
                taxaEntregaCalculada = restaurante.getTaxaEntrega() != null ? restaurante.getTaxaEntrega() : BigDecimal.ZERO;
                logger.debug("Usando taxa fixa: {}", taxaEntregaCalculada);
            }

            // 4. Chama o serviço para SALVAR o pedido offline, passando a taxa
            Pedido novoPedido = pedidoService.salvarPedidoDeliveryCliente(pedidoDTO, tipoPgto, taxaEntregaCalculada);
            logger.info("Pedido offline criado com sucesso. Pedido ID: {}", novoPedido.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(novoPedido);

        } catch (IllegalArgumentException e) {
            logger.warn("Tipo de pagamento inválido recebido: {}", pagamentoStr);
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de pagamento inválido: " + pagamentoStr));
        } catch (PedidoLimitException e) {
            logger.warn("Limite de pedidos atingido ao tentar criar pedido offline. Restaurante ID: {}", pedidoDTO.getRestauranteId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage(), "errorCode", "PEDIDO_LIMIT_REACHED")); // Retorna 403 Forbidden
        } catch (RuntimeException e) {
            logger.error("Erro ao criar pedido offline: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar pedido offline: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erro interno no servidor."));
        }
    }

    // --- Endpoint para PAGAMENTO ONLINE (via Stripe Connect) ---
    @PostMapping("/pagar/delivery")
    public ResponseEntity<?> iniciarPagamentoDeliveryClienteOnline(@RequestBody PedidoDeliveryClienteDTO pedidoDTO) {
        logger.info("Recebida requisição para iniciar pagamento online. Restaurante ID: {}", pedidoDTO.getRestauranteId());
        try {
            // 1. Busca o restaurante para verificar modo de cálculo de frete e conta connect
            Restaurante restaurante = restauranteRepository.findById(pedidoDTO.getRestauranteId())
                    .orElseThrow(() -> new RuntimeException("Restaurante não encontrado."));

            // Validação adicional: Verifica se a conta connect está configurada ANTES de calcular taxa
            if (restaurante.getStripeConnectAccountId() == null || restaurante.getStripeConnectAccountId().isEmpty()) {
                logger.error("Tentativa de pagamento online sem conta Stripe Connect configurada. Restaurante ID: {}", restaurante.getId());
                throw new RuntimeException("Pagamento online indisponível para este restaurante no momento.");
            }

            // 2. Calcula a taxa de entrega ANTES de gerar a URL de pagamento
            BigDecimal taxaEntregaCalculada;
            if (restaurante.isCalculoHaversineAtivo()) {
                if (pedidoDTO.getCepCliente() == null || pedidoDTO.getCepCliente().isBlank()) {
                    throw new RuntimeException("CEP do cliente é obrigatório para cálculo de frete por distância.");
                }
                logger.debug("Calculando taxa Haversine para CEP {}", pedidoDTO.getCepCliente());
                taxaEntregaCalculada = areaEntregaService.calcularTaxa(restaurante.getId(), pedidoDTO.getCepCliente());
                // Validação crucial antes de ir pro Stripe
                if (taxaEntregaCalculada.compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("Não foi possível calcular a taxa de entrega para o CEP informado ou está fora da área de cobertura.");
                }
                logger.debug("Taxa Haversine calculada: {}", taxaEntregaCalculada);
            } else {
                taxaEntregaCalculada = restaurante.getTaxaEntrega() != null ? restaurante.getTaxaEntrega() : BigDecimal.ZERO;
                logger.debug("Usando taxa fixa: {}", taxaEntregaCalculada);
            }

            // 3. Chama o serviço para CRIAR o pedido (status AGUARDANDO...) e GERAR a URL do Stripe, passando a taxa
            String paymentUrl = pedidoService.iniciarPagamentoDeliveryCliente(pedidoDTO, taxaEntregaCalculada);
            logger.info("URL de pagamento Stripe Connect gerada com sucesso para Restaurante ID {}", restaurante.getId());

            // Retorna a URL para o frontend redirecionar o cliente
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));

        } catch (RuntimeException e) {
            logger.error("Erro ao iniciar pagamento online: {}", e.getMessage(), e);
            // Retorna a mensagem de erro específica (ex: "Conta não configurada", "CEP inválido", "Valor inválido")
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) { // Captura StripeException ou outros erros inesperados
            logger.error("Erro inesperado ao iniciar pagamento online: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erro ao processar pagamento: " + e.getMessage()));
        }
    }

    // --- Endpoint para Calcular Frete (Existente - sem alterações) ---
    @GetMapping("/frete/{restauranteId}/{cep}")
    public ResponseEntity<Map<String, Object>> calcularFrete(
            @PathVariable Long restauranteId,
            @PathVariable String cep) {
        logger.debug("Calculando frete para Restaurante ID {} e CEP {}", restauranteId, cep);
        try {
            BigDecimal taxa = areaEntregaService.calcularTaxa(restauranteId, cep);
            logger.info("Taxa de entrega calculada para Restaurante ID {} e CEP {}: {}", restauranteId, cep, taxa);

            // Retorna a taxa de entrega (pode ser 0 ou o valor calculado/fixo)
            // A lógica de erro (-1, -2) foi movida para o service para lançamento de exceções
            return ResponseEntity.ok(Map.of("taxaEntrega", taxa));

        } catch (RuntimeException e) {
            logger.warn("Erro ao calcular frete para Restaurante ID {} e CEP {}: {}", restauranteId, cep, e.getMessage());
            // Retorna a mensagem de erro específica do service (CEP inválido, Fora de área, etc.)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao calcular frete para Restaurante ID {} e CEP {}: {}", restauranteId, cep, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erro interno ao calcular frete."));
        }
    }
} // Fim da classe PublicController