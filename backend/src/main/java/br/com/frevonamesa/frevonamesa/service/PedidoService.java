// backend/src/main/java/br/com/frevonamesa/frevonamesa/service/PedidoService.java

package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.exception.PedidoLimitException;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory


import java.util.UUID;


@Service
public class PedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoService.class); // Adiciona logger

    @Autowired private MesaRepository mesaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private AdicionalRepository adicionalRepository;

    @Autowired
    private FinanceiroService financeiroService; // FinanceiroService já estava injetado

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    @Autowired
    private RestauranteService restauranteService; // RestauranteService já estava injetado

    // MÉTODO AUXILIAR para calcular o valor total do pedido com a taxa de entrega
    private BigDecimal calcularTotalComFrete(Restaurante restaurante, BigDecimal subtotal, BigDecimal taxaEntregaCalculada) {
        // Se a taxa for negativa (erro de cálculo/fora de área), retorna apenas o subtotal
        if (taxaEntregaCalculada.compareTo(BigDecimal.ZERO) < 0) {
            return subtotal;
        }
        return subtotal.add(taxaEntregaCalculada);
    }
    // Sobrecarga para usar a taxa fixa do restaurante (quando Haversine está desativado)
    private BigDecimal calcularTotalComFrete(Restaurante restaurante, BigDecimal subtotal) {
        return subtotal.add(restaurante.getTaxaEntrega() != null ? restaurante.getTaxaEntrega() : BigDecimal.ZERO);
    }


    @Transactional
    public Pedido criarPedido(PedidoRequestDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Mesa mesa = mesaRepository.findById(dto.getMesaId())
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        Pedido novoPedido = new Pedido();
        novoPedido.setMesa(mesa);
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());
        novoPedido.setTipo(TipoPedido.MESA);

        List<Adicional> adicionaisDisponiveis = adicionalRepository.findByRestauranteId(restaurante.getId());

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId()).orElseThrow(() -> new RuntimeException("Produto não encontrado!"));
            if (!produto.getRestaurante().getId().equals(restaurante.getId())) {
                throw new SecurityException("Acesso negado.");
            }
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido); // Essencial para cálculo de subtotal com adicionais

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                itemPedido.setAdicionais(new ArrayList<>()); // Inicializa a lista
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido: ID " + adicionalId));
                    // Cria e adiciona o ItemPedidoAdicional
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            } else {
                itemPedido.setAdicionais(new ArrayList<>()); // Garante que a lista não seja nula
            }
            novoPedido.getItens().add(itemPedido);
        }

        BigDecimal totalPedido = novoPedido.getItens().stream()
                .map(ItemPedido::getSubtotal) // Usa o método getSubtotal que inclui adicionais
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        novoPedido.setTotal(totalPedido);

        // Atualiza valor total da mesa SOMENTE com o valor do NOVO pedido
        BigDecimal valorTotalAtualizadoMesa = mesa.getValorTotal() != null ? mesa.getValorTotal().add(totalPedido) : totalPedido;
        mesa.setValorTotal(valorTotalAtualizadoMesa);


        if (restaurante.isImpressaoMesaAtivada()) {
            novoPedido.setStatus(StatusPedido.PENDENTE);
        } else {
            novoPedido.setStatus(StatusPedido.CONFIRMADO);
        }

        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now());
        }

        pedidoRepository.save(novoPedido); // Salva o pedido (e itens em cascata)
        mesaRepository.save(mesa); // Salva a mesa atualizada
        return novoPedido;
    }

    public List<PedidoFilaDTO> listarPedidosDeMesaPendentes() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        List<Pedido> pedidos = pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.MESA, StatusPedido.PENDENTE, restaurante.getId());
        return pedidos.stream().map(pedido -> {
            Mesa mesa = pedido.getMesa();
            // Evita NPE se mesa for nula (improvável, mas seguro)
            MesaSimplesDTO mesaDto = (mesa != null) ? new MesaSimplesDTO(mesa.getNumero(), mesa.getNomeCliente()) : new MesaSimplesDTO(0, "N/A");
            PedidoFilaDTO pedidoDto = new PedidoFilaDTO();
            pedidoDto.setId(pedido.getId());
            pedidoDto.setDataHora(pedido.getDataHora());
            pedidoDto.setItens(pedido.getItens()); // Itens já vêm carregados (eager ou join fetch se necessário)
            pedidoDto.setMesa(mesaDto);
            return pedidoDto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Pedido confirmarPedidoDeMesa(Long pedidoId) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (!pedido.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new RuntimeException("Apenas pedidos pendentes podem ser confirmados.");
        }
        pedido.setStatus(StatusPedido.CONFIRMADO);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido criarPedidoDelivery(PedidoDeliveryRequestDTO dto) throws PedidoLimitException { // Adiciona throws
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        // Limite padrão (pode vir de config futuramente)
        int hardLimit = 30; // Exemplo de limite para planos limitados

        // VERIFICAÇÃO DE LIMITE ATUALIZADA:
        boolean isGratuitoLimitado = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isDeliveryPro()
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester();

        if (isGratuitoLimitado && restaurante.getPedidosMesAtual() >= hardLimit) {
            // Lança a exceção específica para o frontend tratar
            throw new PedidoLimitException("Limite de pedidos mensais atingido! O pedido não pode ser salvo até que o limite seja liberado.", restaurante.getPedidosMesAtual(), hardLimit);
        }

        Pedido novoPedido = new Pedido();
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());
        novoPedido.setTipo(TipoPedido.DELIVERY);
        novoPedido.setUuid(UUID.randomUUID()); // Gera UUID para rastreio

        // Status inicial depende da configuração de impressão
        novoPedido.setStatus(restaurante.isImpressaoDeliveryAtivada() ? StatusPedido.PENDENTE : StatusPedido.EM_PREPARO);

        novoPedido.setNomeClienteDelivery(dto.getNomeCliente());
        novoPedido.setTelefoneClienteDelivery(dto.getTelefoneCliente());
        novoPedido.setEnderecoClienteDelivery(dto.getEnderecoCliente());
        novoPedido.setPontoReferencia(dto.getPontoReferencia());

        List<Adicional> adicionaisDisponiveis = adicionalRepository.findByRestauranteId(restaurante.getId());
        BigDecimal subtotalPedido = BigDecimal.ZERO;

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId()).orElseThrow(() -> new RuntimeException("Produto não encontrado!"));
            if (!produto.getRestaurante().getId().equals(restaurante.getId())) {
                throw new SecurityException("Acesso negado: produto inválido.");
            }
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido); // Associa ao pedido

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                itemPedido.setAdicionais(new ArrayList<>()); // Inicializa
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido: ID " + adicionalId));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            } else {
                itemPedido.setAdicionais(new ArrayList<>());
            }
            subtotalPedido = subtotalPedido.add(itemPedido.getSubtotal()); // Acumula subtotal do item
            novoPedido.getItens().add(itemPedido);
        }

        // Calcula total com frete (usando taxa fixa, pois este método é interno do painel)
        BigDecimal totalComFrete = calcularTotalComFrete(restaurante, subtotalPedido);
        novoPedido.setTotal(totalComFrete);

        Pedido pedidoSalvo = pedidoRepository.save(novoPedido); // Salva pedido e itens

        // INCREMENTO ATUALIZADO: Contabiliza APENAS se for plano gratuito limitado
        if (isGratuitoLimitado) {
            restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
            restauranteRepository.save(restaurante); // Atualiza contador
            logger.info("Contador de pedidos incrementado para Restaurante ID {}: {}", restaurante.getId(), restaurante.getPedidosMesAtual());
        }

        logger.info("Pedido Delivery (interno) criado com sucesso: ID {}", pedidoSalvo.getId());
        return pedidoSalvo;
    }


    public Map<StatusPedido, List<Pedido>> listarPedidosDeliveryPorStatus() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Long restauranteId = restaurante.getId();
        Map<StatusPedido, List<Pedido>> pedidosAgrupados = new HashMap<>();

        // Lista de status ativos para o painel Kanban
        List<StatusPedido> statusKanban = Arrays.asList(
                StatusPedido.PENDENTE,
                StatusPedido.EM_PREPARO,
                StatusPedido.PRONTO_PARA_ENTREGA,
                StatusPedido.AGUARDANDO_PGTO_LIMITE // Inclui pedidos retidos
        );

        // Busca todos os pedidos nesses status de uma vez
        List<Pedido> pedidosAtivos = pedidoRepository.findByTipoAndStatusInAndRestauranteIdOrderByDataHoraAsc(
                TipoPedido.DELIVERY, statusKanban, restauranteId
        );

        // Agrupa manualmente
        for (StatusPedido status : statusKanban) {
            pedidosAgrupados.put(status, new ArrayList<>());
        }
        for (Pedido p : pedidosAtivos) {
            pedidosAgrupados.get(p.getStatus()).add(p);
        }

        // Se quiser que AGUARDANDO_PGTO_LIMITE apareça na coluna PENDENTE, combine as listas:
        List<Pedido> pendentesCombinados = new ArrayList<>(pedidosAgrupados.getOrDefault(StatusPedido.PENDENTE, Collections.emptyList()));
        pendentesCombinados.addAll(pedidosAgrupados.getOrDefault(StatusPedido.AGUARDANDO_PGTO_LIMITE, Collections.emptyList()));
        // Ordena combinados por data (mais antigos primeiro, ou mais novos se preferir reversed)
        pendentesCombinados.sort(Comparator.comparing(Pedido::getDataHora));
        pedidosAgrupados.put(StatusPedido.PENDENTE, pendentesCombinados);
        pedidosAgrupados.remove(StatusPedido.AGUARDANDO_PGTO_LIMITE); // Remove a coluna separada se combinado

        return pedidosAgrupados;
    }

    @Transactional
    public Pedido atualizarStatusPedidoDelivery(Long pedidoId, StatusPedido novoStatus) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (!pedido.getRestaurante().getId().equals(restaurante.getId()) || pedido.getTipo() != TipoPedido.DELIVERY) {
            throw new SecurityException("Acesso negado.");
        }

        // BLOQUEIO: Pedido retido só pode ser CANCELADO (FINALIZADO) via fluxo normal.
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PGTO_LIMITE && novoStatus != StatusPedido.FINALIZADO) {
            throw new RuntimeException("Pedido retido! O limite deve ser pago antes de avançar o status.");
        }

        // Define pagamento como DINHEIRO se finalizar sem ter sido pago online/definido antes
        if (novoStatus == StatusPedido.FINALIZADO && pedido.getTipoPagamento() == null) {
            pedido.setTipoPagamento(TipoPagamento.DINHEIRO); // Assume dinheiro como padrão
        }

        pedido.setStatus(novoStatus);
        logger.info("Status do Pedido Delivery ID {} atualizado para {}", pedidoId, novoStatus);

        // --- LÓGICA DE WEBHOOK PARA O N8N ---
        if (n8nWebhookUrl != null && !n8nWebhookUrl.isBlank() && restaurante.getWhatsappNumber() != null && !restaurante.getWhatsappNumber().isBlank()) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = String.format(
                    "{ \"pedidoId\": %d, \"novoStatus\": \"%s\", \"clienteNome\": \"%s\", \"clienteTelefone\": \"%s\", \"restauranteWhatsapp\": \"%s\", \"pedidoUuid\": \"%s\" }",
                    pedido.getId(),
                    novoStatus.toString(),
                    pedido.getNomeClienteDelivery(),
                    pedido.getTelefoneClienteDelivery(),
                    restaurante.getWhatsappNumber(),
                    pedido.getUuid() != null ? pedido.getUuid().toString() : "" // Envia UUID se existir
            );

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            try {
                restTemplate.postForEntity(n8nWebhookUrl, entity, String.class);
                logger.info("Webhook do n8n notificado para o pedido #{}", pedido.getId());
            } catch (Exception e) {
                logger.error("Falha ao notificar webhook do n8n para pedido #{}: {}", pedido.getId(), e.getMessage());
                // Não lançar exceção aqui para não reverter a atualização de status
            }
        } else {
            logger.warn("Webhook n8n ou WhatsApp do restaurante não configurados. Notificação não enviada para pedido #{}", pedido.getId());
        }

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarUltimos10Finalizados() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        return pedidoRepository.findTop10ByRestauranteIdAndTipoAndStatusOrderByDataHoraDesc(
                restaurante.getId(), TipoPedido.DELIVERY, StatusPedido.FINALIZADO);
    }

    @Transactional
    public Pedido imprimirPedidoDelivery(Long pedidoId) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (!pedido.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        pedido.setImpresso(true); // Marca como impresso
        logger.info("Pedido Delivery ID {} marcado como impresso.", pedidoId);
        return pedidoRepository.save(pedido);
    }

    // Método não parece ser mais usado pelo frontend, mas mantido por segurança
    public List<Pedido> listarPedidosDeliveryPendentes() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        return pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PENDENTE, restaurante.getId());
    }

    @Transactional
    public Pedido criarPedidoMesaCliente(PedidoClienteDTO dto) {
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado!"));
        Mesa mesa = mesaRepository.findByNumeroAndRestauranteId(dto.getNumeroMesa(), restaurante.getId())
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada ou inválida para este restaurante."));

        // Atualiza nome do cliente na mesa se informado no DTO
        if (dto.getNomeCliente() != null && !dto.getNomeCliente().trim().isEmpty()) {
            mesa.setNomeCliente(dto.getNomeCliente());
        }

        Pedido novoPedido = new Pedido();
        novoPedido.setMesa(mesa);
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());
        novoPedido.setTipo(TipoPedido.MESA);

        List<Adicional> adicionaisDisponiveis = adicionalRepository.findByRestauranteId(restaurante.getId());
        BigDecimal totalPedido = BigDecimal.ZERO;

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido); // Associa

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                itemPedido.setAdicionais(new ArrayList<>());
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido: ID " + adicionalId));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            } else {
                itemPedido.setAdicionais(new ArrayList<>());
            }
            totalPedido = totalPedido.add(itemPedido.getSubtotal()); // Acumula
            novoPedido.getItens().add(itemPedido);
        }

        novoPedido.setTotal(totalPedido);
        // Atualiza valor total da mesa
        BigDecimal valorTotalAtualizadoMesa = mesa.getValorTotal() != null ? mesa.getValorTotal().add(totalPedido) : totalPedido;
        mesa.setValorTotal(valorTotalAtualizadoMesa);

        novoPedido.setStatus(restaurante.isImpressaoMesaAtivada() ? StatusPedido.PENDENTE : StatusPedido.CONFIRMADO);

        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now());
        }

        pedidoRepository.save(novoPedido);
        mesaRepository.save(mesa);
        logger.info("Pedido de Cliente para Mesa {} criado com sucesso: Pedido ID {}", mesa.getNumero(), novoPedido.getId());
        return novoPedido;
    }


    /**
     * Salva um pedido de cliente final com pagamento offline (DINHEIRO/CARTAO/PIX na entrega)
     */
    @Transactional
    public Pedido salvarPedidoDeliveryCliente(PedidoDeliveryClienteDTO dto, TipoPagamento tipoPagamento, BigDecimal taxaEntregaCalculada) throws PedidoLimitException {
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado!"));

        // --- Lógica de Monetização ---
        int hardLimit = 30; // Exemplo de limite
        boolean isGratuitoLimitado = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isDeliveryPro()
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester();

        if (isGratuitoLimitado && restaurante.getPedidosMesAtual() >= hardLimit) {
            logger.warn("Limite de pedidos atingido para Restaurante ID {} (Plano Gratuito)", restaurante.getId());
            // Lança exceção para o controller tratar (retornar erro para o cliente)
            throw new PedidoLimitException("Limite de pedidos atingido para este restaurante.", restaurante.getPedidosMesAtual(), hardLimit);
        }

        // --- Criação e Cálculo do Total ---
        Pedido novoPedido = new Pedido();
        novoPedido.setUuid(UUID.randomUUID());
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setTipo(TipoPedido.DELIVERY);
        novoPedido.setItens(new ArrayList<>());
        novoPedido.setStatus(restaurante.isImpressaoDeliveryAtivada() ? StatusPedido.PENDENTE : StatusPedido.EM_PREPARO);
        novoPedido.setNomeClienteDelivery(dto.getNomeCliente());
        novoPedido.setTelefoneClienteDelivery(dto.getTelefoneCliente());
        novoPedido.setEnderecoClienteDelivery(dto.getEnderecoCliente());
        novoPedido.setPontoReferencia(dto.getPontoReferencia());
        novoPedido.setTipoPagamento(tipoPagamento); // Define pagamento offline AQUI

        BigDecimal subtotalPedido = BigDecimal.ZERO;
        List<Adicional> adicionaisDisponiveis = adicionalRepository.findByRestauranteId(restaurante.getId());
        List<ItemPedido> itensParaSalvar = new ArrayList<>();

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido); // Associa

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                itemPedido.setAdicionais(new ArrayList<>());
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido: ID " + adicionalId));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            } else {
                itemPedido.setAdicionais(new ArrayList<>());
            }

            subtotalPedido = subtotalPedido.add(itemPedido.getSubtotal()); // Acumula
            itensParaSalvar.add(itemPedido);
        }

        // Usa a taxa de entrega calculada (passada como parâmetro)
        BigDecimal totalComFrete = calcularTotalComFrete(restaurante, subtotalPedido, taxaEntregaCalculada);

        if (totalComFrete.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do pedido deve ser maior que zero.");
        }

        novoPedido.setTotal(totalComFrete);
        novoPedido.setItens(itensParaSalvar); // Associa itens

        Pedido pedidoSalvo = pedidoRepository.save(novoPedido); // Salva

        // Incrementa o contador APENAS se for plano gratuito limitado
        if (isGratuitoLimitado) {
            restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
            restauranteRepository.save(restaurante);
            logger.info("Contador de pedidos (offline) incrementado para Restaurante ID {}: {}", restaurante.getId(), restaurante.getPedidosMesAtual());
        }

        logger.info("Pedido Delivery Cliente (offline) salvo com sucesso: ID {}", pedidoSalvo.getId());
        return pedidoSalvo;
    }


    /**
     * Inicia o fluxo de pagamento online para um pedido de cliente final.
     * Cria o pedido com status AGUARDANDO_PGTO_LIMITE e chama o FinanceiroService.
     */
    @Transactional
    public String iniciarPagamentoDeliveryCliente(PedidoDeliveryClienteDTO dto, BigDecimal taxaEntregaCalculada) {
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado!"));

        // --- Criação do Pedido e Cálculo do Total ---
        UUID pedidoUuid = UUID.randomUUID();
        BigDecimal subtotalPedido = BigDecimal.ZERO;
        List<Adicional> adicionaisDisponiveis = adicionalRepository.findByRestauranteId(restaurante.getId());
        List<ItemPedido> itensParaSalvar = new ArrayList<>();
        Pedido pedidoTemporario = new Pedido(); // Para cálculo de subtotal com adicionais

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(pedidoTemporario); // Associa temporário

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                itemPedido.setAdicionais(new ArrayList<>());
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido: ID " + adicionalId));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            } else {
                itemPedido.setAdicionais(new ArrayList<>());
            }
            subtotalPedido = subtotalPedido.add(itemPedido.getSubtotal()); // Acumula
            itensParaSalvar.add(itemPedido);
        }

        // Usa a taxa de entrega calculada
        BigDecimal totalComFrete = calcularTotalComFrete(restaurante, subtotalPedido, taxaEntregaCalculada);

        if (totalComFrete.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do pedido deve ser maior que zero.");
        }

        // --- Salva o Pedido Pre-Pago ---
        Pedido pedidoPrePago = new Pedido();
        pedidoPrePago.setUuid(pedidoUuid);
        pedidoPrePago.setRestaurante(restaurante);
        pedidoPrePago.setDataHora(LocalDateTime.now());
        pedidoPrePago.setTipo(TipoPedido.DELIVERY);
        pedidoPrePago.setTotal(totalComFrete);
        pedidoPrePago.setStatus(StatusPedido.AGUARDANDO_PGTO_LIMITE); // Status inicial para pagamento online
        pedidoPrePago.setNomeClienteDelivery(dto.getNomeCliente());
        pedidoPrePago.setTelefoneClienteDelivery(dto.getTelefoneCliente());
        pedidoPrePago.setEnderecoClienteDelivery(dto.getEnderecoCliente());
        pedidoPrePago.setPontoReferencia(dto.getPontoReferencia());

        for(ItemPedido item : itensParaSalvar) {
            item.setPedido(pedidoPrePago); // Associa ao pedido real
        }
        pedidoPrePago.setItens(itensParaSalvar);

        pedidoRepository.save(pedidoPrePago); // Salva o pedido ANTES de gerar a URL
        logger.info("Pedido Delivery Cliente (pré-pagamento) salvo com ID {} e UUID {}", pedidoPrePago.getId(), pedidoUuid);


        // --- Geração da URL de pagamento (MODIFICADO para passar restauranteId) ---
        try {
            // Chama o método do FinanceiroService que agora inclui o restauranteId
            return financeiroService.gerarUrlPagamentoPedidoPublico(pedidoUuid, totalComFrete, restaurante.getId()); // <-- AJUSTE AQUI
        } catch (Exception e) {
            // Captura StripeException ou RuntimeException do FinanceiroService
            logger.error("Erro ao chamar financeiroService.gerarUrlPagamentoPedidoPublico para Pedido UUID {}: {}", pedidoUuid, e.getMessage(), e);
            // Propaga a exceção para o controller lidar (ex: retornar 500 ou mensagem de erro)
            throw new RuntimeException("Erro ao iniciar pagamento online: " + e.getMessage(), e);
        }
    }


    /**
     * Finaliza o Pedido após o pagamento externo (Stripe) ser aprovado via Webhook.
     * Atualiza o status e incrementa o contador de pedidos, se aplicável.
     */
    @Transactional
    public Pedido finalizarPedidoAprovado(UUID pedidoUuid, TipoPagamento tipoPagamento) {
        Pedido pedido = pedidoRepository.findByUuid(pedidoUuid)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado via webhook: " + pedidoUuid));

        // Verifica se o pedido está no status correto
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PGTO_LIMITE) {
            logger.warn("Webhook: Pedido #{} (UUID {}) não estava em status AGUARDANDO_PGTO_LIMITE. Status atual: {}. Ignorando.", pedido.getId(), pedidoUuid, pedido.getStatus());
            return pedido; // Retorna o pedido sem alteração
        }

        // 1. Atualiza o status e a forma de pagamento
        Restaurante restaurante = pedido.getRestaurante();
        // Define o status inicial pós-pagamento (PENDENTE se imprime, EM_PREPARO se não)
        StatusPedido proximoStatus = restaurante.isImpressaoDeliveryAtivada() ? StatusPedido.PENDENTE : StatusPedido.EM_PREPARO;
        pedido.setStatus(proximoStatus);
        pedido.setTipoPagamento(tipoPagamento); // Registra como foi pago
        logger.info("Pedido #{} (UUID {}) finalizado após pagamento online. Novo status: {}", pedido.getId(), pedidoUuid, proximoStatus);


        // 2. Incrementa o contador (regra de monetização)
        boolean isGratuitoLimitado = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isDeliveryPro()
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester();

        if (isGratuitoLimitado) {
            restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
            restauranteRepository.save(restaurante); // Atualiza contador
            logger.info("Contador de pedidos (online) incrementado para Restaurante ID {}: {}", restaurante.getId(), restaurante.getPedidosMesAtual());
        }

        // Notifica N8N (opcional, pode ser feito em atualizarStatusPedidoDelivery também)
        // ... (lógica do webhook n8n pode ser adicionada aqui se necessário) ...

        return pedidoRepository.save(pedido); // Salva as alterações no pedido
    }

    @Transactional
    public Pedido aceitarPedidoRetido(Long pedidoId) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado."));

        if (!pedido.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PGTO_LIMITE) {
            throw new RuntimeException("O pedido não está no status de retenção (AGUARDANDO_PGTO_LIMITE). Status atual: " + pedido.getStatus());
        }

        // Define o status pós-aceitação (depende da impressão)
        StatusPedido novoStatus = restaurante.isImpressaoDeliveryAtivada() ? StatusPedido.PENDENTE : StatusPedido.EM_PREPARO;
        pedido.setStatus(novoStatus);
        logger.info("Pedido Retido ID {} aceito manualmente. Novo status: {}", pedidoId, novoStatus);

        return pedidoRepository.save(pedido);
    }

    // Consulta pública para rastreio
    public Pedido rastrearPedidoPorUuid(UUID uuid) {
        // Considerar adicionar Join Fetch para itens e produto para evitar N+1
        return pedidoRepository.findByUuid(uuid).orElse(null);
    }

    // Método auxiliar interno para buscar pedidos por status e tipo (pode ser útil)
    private List<Pedido> findPedidosByStatusAndType(Restaurante restaurante, TipoPedido tipo, StatusPedido status) {
        return pedidoRepository.findByTipoAndStatusAndRestauranteId(tipo, status, restaurante.getId());
    }

    // Novo método auxiliar para consulta com múltiplos status
    @Transactional(readOnly = true) // Apenas leitura
    public List<Pedido> findByTipoAndStatusInAndRestauranteIdOrderByDataHoraAsc(TipoPedido tipo, List<StatusPedido> statuses, Long restauranteId) {
        // Esta implementação depende de uma nova consulta no PedidoRepository
        return pedidoRepository.findByTipoAndStatusInAndRestauranteIdOrderByDataHoraAsc(tipo, statuses, restauranteId);
    }


}