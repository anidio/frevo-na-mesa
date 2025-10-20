package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.exception.PedidoLimitException;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.AdicionalRepository;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import br.com.frevonamesa.frevonamesa.repository.ProdutoRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
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

// IMPORTS DO MERCADO PAGO REMOVIDOS (MPApiException, MPException)
import java.util.UUID;


@Service
public class PedidoService {

    @Autowired private MesaRepository mesaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private AdicionalRepository adicionalRepository;

    @Autowired
    private FinanceiroService financeiroService;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    @Autowired
    private RestauranteService restauranteService;

    // MÉTODO AUXILIAR para calcular o valor total do pedido com a taxa de entrega
    private BigDecimal calcularTotalComFrete(Restaurante restaurante, BigDecimal subtotal) {
        return subtotal.add(restaurante.getTaxaEntrega());
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
            itemPedido.setPedido(novoPedido);

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido."));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            }
            novoPedido.getItens().add(itemPedido);
        }

        BigDecimal totalPedido = novoPedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        novoPedido.setTotal(totalPedido);
        mesa.setValorTotal(mesa.getValorTotal().add(totalPedido));

        if (restaurante.isImpressaoMesaAtivada()) {
            novoPedido.setStatus(StatusPedido.PENDENTE);
        } else {
            novoPedido.setStatus(StatusPedido.CONFIRMADO);
        }

        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now());
        }

        pedidoRepository.save(novoPedido);
        mesaRepository.save(mesa);
        return novoPedido;
    }

    public List<PedidoFilaDTO> listarPedidosDeMesaPendentes() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        List<Pedido> pedidos = pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.MESA, StatusPedido.PENDENTE, restaurante.getId());
        return pedidos.stream().map(pedido -> {
            Mesa mesa = pedido.getMesa();
            MesaSimplesDTO mesaDto = new MesaSimplesDTO(mesa.getNumero(), mesa.getNomeCliente());
            PedidoFilaDTO pedidoDto = new PedidoFilaDTO();
            pedidoDto.setId(pedido.getId());
            pedidoDto.setDataHora(pedido.getDataHora());
            pedidoDto.setItens(pedido.getItens());
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
        pedido.setStatus(StatusPedido.CONFIRMADO);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido criarPedidoDelivery(PedidoDeliveryRequestDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        // Limite fixo para demonstração
        int hardLimit = 30;

        // VERIFICAÇÃO DE LIMITE ATUALIZADA:
        // O limite só é verificado se o plano for GRATUITO, e o usuário NÃO for Legacy Free NEM Beta Tester.
        boolean shouldCheckLimit = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester();

        if (shouldCheckLimit && restaurante.getPedidosMesAtual() >= hardLimit) {
            // Lança a exceção para abrir o modal de pagamento no painel do staff.
            throw new PedidoLimitException("Limite de pedidos mensais atingido! O pedido não pode ser salvo até que o limite seja liberado.", restaurante.getPedidosMesAtual(), hardLimit);
        }

        Pedido novoPedido = new Pedido();
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());
        novoPedido.setTipo(TipoPedido.DELIVERY);
        novoPedido.setUuid(UUID.randomUUID());

        if (restaurante.isImpressaoDeliveryAtivada()) {
            novoPedido.setStatus(StatusPedido.PENDENTE);
        } else {
            novoPedido.setStatus(StatusPedido.EM_PREPARO);
        }

        novoPedido.setNomeClienteDelivery(dto.getNomeCliente());
        novoPedido.setTelefoneClienteDelivery(dto.getTelefoneCliente());
        novoPedido.setEnderecoClienteDelivery(dto.getEnderecoCliente());
        novoPedido.setPontoReferencia(dto.getPontoReferencia());

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
            itemPedido.setPedido(novoPedido);

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido."));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            }
            novoPedido.getItens().add(itemPedido);
        }

        BigDecimal totalPedido = novoPedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        novoPedido.setTotal(totalPedido);

        Pedido pedidoSalvo = pedidoRepository.save(novoPedido);

        // INCREMENTO ATUALIZADO: Contabiliza APENAS se deve checar o limite e ele não foi atingido
        if (shouldCheckLimit) {
            restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
            restauranteRepository.save(restaurante);
        }

        return pedidoSalvo;
    }


    public Map<StatusPedido, List<Pedido>> listarPedidosDeliveryPorStatus() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Map<StatusPedido, List<Pedido>> pedidosAgrupados = new HashMap<>();

        // Buscamos pedidos PENDENTES, EM PREPARO e PRONTO PARA ENTREGA normalmente
        pedidosAgrupados.put(StatusPedido.PENDENTE, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PENDENTE, restaurante.getId()));
        pedidosAgrupados.put(StatusPedido.EM_PREPARO, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.EM_PREPARO, restaurante.getId()));
        pedidosAgrupados.put(StatusPedido.PRONTO_PARA_ENTREGA, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PRONTO_PARA_ENTREGA, restaurante.getId()));

        // NOVO: Adicionamos pedidos retidos na coluna PENDENTE para que o PainelDelivery os exiba como "Novos Pedidos"
        List<Pedido> pedidosPendentes = pedidosAgrupados.getOrDefault(StatusPedido.PENDENTE, new ArrayList<>());
        pedidosPendentes.addAll(pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.AGUARDANDO_PGTO_LIMITE, restaurante.getId()));

        // Ordena para garantir que os mais novos apareçam primeiro
        pedidosPendentes.sort(Comparator.comparing(Pedido::getDataHora).reversed());
        pedidosAgrupados.put(StatusPedido.PENDENTE, pedidosPendentes);

        return pedidosAgrupados;
    }

    @Transactional
    public Pedido atualizarStatusPedidoDelivery(Long pedidoId, StatusPedido novoStatus) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (!pedido.getRestaurante().getId().equals(restaurante.getId()) || pedido.getTipo() != TipoPedido.DELIVERY) {
            throw new SecurityException("Acesso negado.");
        }

        // BLOQUEIO: Pedido retido só pode ser CANCELADO (FINALIZADO).
        // Qualquer tentativa de avançar (para PENDENTE ou EM_PREPARO) é bloqueada aqui.
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PGTO_LIMITE && novoStatus != StatusPedido.FINALIZADO) {
            throw new RuntimeException("Pedido retido! O limite deve ser pago antes de iniciar o preparo.");
        }

        if (novoStatus == StatusPedido.FINALIZADO && pedido.getTipoPagamento() == null) {
            pedido.setTipoPagamento(TipoPagamento.DINHEIRO);
        }

        pedido.setStatus(novoStatus);

        // --- LÓGICA FINAL DE WEBHOOK PARA O N8N ---
        // Verifica se a URL mestre do n8n e o número do restaurante estão configurados
        if (n8nWebhookUrl != null && !n8nWebhookUrl.isBlank() && restaurante.getWhatsappNumber() != null && !restaurante.getWhatsappNumber().isBlank()) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Monta um objeto JSON com todos os dados que o n8n vai precisar
            String requestBody = String.format(
                    "{ \"pedidoId\": %d, \"novoStatus\": \"%s\", \"clienteNome\": \"%s\", \"clienteTelefone\": \"%s\", \"restauranteWhatsapp\": \"%s\" }",
                    pedido.getId(),
                    novoStatus.toString(),
                    pedido.getNomeClienteDelivery(),
                    pedido.getTelefoneClienteDelivery(),
                    restaurante.getWhatsappNumber() // Enviando o número do restaurante
            );

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            try {
                restTemplate.postForEntity(n8nWebhookUrl, entity, String.class);
                System.out.println("INFO: Webhook do n8n notificado para o pedido #" + pedido.getId());
            } catch (Exception e) {
                System.err.println("ERRO: Falha ao notificar webhook do n8n: " + e.getMessage());
            }
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
        pedido.setImpresso(true);
        return pedidoRepository.save(pedido);
    }

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

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido);

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido."));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            }
            novoPedido.getItens().add(itemPedido);
        }

        BigDecimal totalPedido = novoPedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        novoPedido.setTotal(totalPedido);
        mesa.setValorTotal(mesa.getValorTotal().add(totalPedido));

        if (restaurante.isImpressaoMesaAtivada()) {
            novoPedido.setStatus(StatusPedido.PENDENTE);
        } else {
            novoPedido.setStatus(StatusPedido.CONFIRMADO);
        }

        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now());
        }

        pedidoRepository.save(novoPedido);
        mesaRepository.save(mesa);
        return novoPedido;
    }

    /**
     * NOVO: Salva um pedido de cliente final com pagamento offline (DINHEIRO/CARTAO)
     * @param dto Dados do pedido.
     * @param tipoPagamento Tipo de pagamento offline selecionado (DINHEIRO, CARTAO_DEBITO, etc.)
     * @return O Pedido salvo.
     */
    @Transactional
    public Pedido salvarPedidoDeliveryCliente(PedidoDeliveryClienteDTO dto, TipoPagamento tipoPagamento) {
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado!"));

        // --- 1. Lógica de Monetização de Limite para Plano GRATUITO ---
        int hardLimit = 2; // Limite do plano gratuito
        boolean isLimitedPlan = restaurante.getPlano().equals("GRATUITO");
        boolean shouldCheckLimit = !restaurante.isLegacyFree() && !restaurante.isBetaTester() && isLimitedPlan;
        boolean limitReached = shouldCheckLimit && restaurante.getPedidosMesAtual() >= hardLimit;

        if (limitReached) {
            // Se o limite foi atingido, LANÇA EXCEÇÃO (Para o ADMIN liberar)
            throw new RuntimeException("Limite de pedidos atingido! Não é possível aceitar pagamento na entrega.");
        }

        // --- 2. Criação e Cálculo do Total ---
        Pedido novoPedido = new Pedido();
        novoPedido.setUuid(UUID.randomUUID());
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setTipo(TipoPedido.DELIVERY);
        novoPedido.setItens(new ArrayList<>());

        // Status inicial: PENDENTE se tiver impressão, EM_PREPARO se não tiver.
        novoPedido.setStatus(restaurante.isImpressaoDeliveryAtivada() ? StatusPedido.PENDENTE : StatusPedido.EM_PREPARO);

        novoPedido.setNomeClienteDelivery(dto.getNomeCliente());
        novoPedido.setTelefoneClienteDelivery(dto.getTelefoneCliente());
        novoPedido.setEnderecoClienteDelivery(dto.getEnderecoCliente());
        novoPedido.setPontoReferencia(dto.getPontoReferencia());

        // Define o pagamento AQUI
        novoPedido.setTipoPagamento(tipoPagamento);

        // Lógica de cálculo (COMPLETA E CORRIGIDA)
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

            // É necessário setar o Pedido antes de calcular o subtotal para que ItemPedido.getSubtotal() funcione
            itemPedido.setPedido(novoPedido);

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido."));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            }

            BigDecimal subtotalItem = itemPedido.getSubtotal();
            subtotalPedido = subtotalPedido.add(subtotalItem);

            itensParaSalvar.add(itemPedido);
        }

        // CORREÇÃO CRÍTICA: Calcula o total com Frete
        BigDecimal totalComFrete = calcularTotalComFrete(restaurante, subtotalPedido);

        if (totalComFrete.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do pedido deve ser maior que zero.");
        }

        novoPedido.setTotal(totalComFrete);
        novoPedido.setItens(itensParaSalvar); // Associa os itens ao novo pedido

        Pedido pedidoSalvo = pedidoRepository.save(novoPedido);

        // 3. Incrementa o contador APENAS se não estava retido e era plano GRATUITO.
        if (shouldCheckLimit) {
            restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
            restauranteRepository.save(restaurante);
        }

        return pedidoSalvo;
    }


    /**
     * NOVO: Inicia o fluxo de pagamento para um pedido de cliente final (Cardápio Público).
     * @param dto Dados do pedido.
     * @return URL de pagamento do Stripe.
     */
    @Transactional
    public String iniciarPagamentoDeliveryCliente(PedidoDeliveryClienteDTO dto) {
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado!"));

        // --- 1. Criação do Pedido e Cálculo do Total ---
        UUID pedidoUuid = UUID.randomUUID();
        BigDecimal subtotalPedido = BigDecimal.ZERO;
        List<Adicional> adicionaisDisponiveis = adicionalRepository.findByRestauranteId(restaurante.getId());
        List<ItemPedido> itensParaSalvar = new ArrayList<>();

        // Cria um pedido temporário para associar os itens e calcular os subtotais corretamente
        Pedido pedidoTemporario = new Pedido();

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());

            // Associa o pedido temporário para que getSubtotal() funcione com adicionais
            itemPedido.setPedido(pedidoTemporario);

            if (itemDto.getAdicionaisIds() != null && !itemDto.getAdicionaisIds().isEmpty()) {
                for (Long adicionalId : itemDto.getAdicionaisIds()) {
                    Adicional adicional = adicionaisDisponiveis.stream()
                            .filter(a -> a.getId().equals(adicionalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Adicional inválido."));
                    itemPedido.getAdicionais().add(new ItemPedidoAdicional(itemPedido, adicional));
                }
            }

            BigDecimal subtotalItem = itemPedido.getSubtotal();
            subtotalPedido = subtotalPedido.add(subtotalItem);

            itensParaSalvar.add(itemPedido);
        }

        // 2. Calcula o total com o frete
        BigDecimal totalComFrete = calcularTotalComFrete(restaurante, subtotalPedido);

        if (totalComFrete.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do pedido deve ser maior que zero.");
        }

        // --- 3. Salva o Pedido Pre-Pago ---
        Pedido pedidoPrePago = new Pedido();
        pedidoPrePago.setUuid(pedidoUuid);
        pedidoPrePago.setRestaurante(restaurante);
        pedidoPrePago.setDataHora(LocalDateTime.now());
        pedidoPrePago.setTipo(TipoPedido.DELIVERY);
        pedidoPrePago.setTotal(totalComFrete); // Total com frete
        pedidoPrePago.setStatus(StatusPedido.AGUARDANDO_PGTO_LIMITE);

        pedidoPrePago.setNomeClienteDelivery(dto.getNomeCliente());
        pedidoPrePago.setTelefoneClienteDelivery(dto.getTelefoneCliente());
        pedidoPrePago.setEnderecoClienteDelivery(dto.getEnderecoCliente());
        pedidoPrePago.setPontoReferencia(dto.getPontoReferencia());

        // Associa os itens ao pedido
        for(ItemPedido item : itensParaSalvar) {
            item.setPedido(pedidoPrePago);
        }
        pedidoPrePago.setItens(itensParaSalvar);

        // SALVA O PEDIDO EM STATUS AGUARDANDO PGTO EXTERNO
        pedidoRepository.save(pedidoPrePago);

        // 4. Geração da URL de pagamento (Chama FinanceiroService)
        // O THROW EXPLICITO FOI REMOVIDO DA ASSINATURA, MAS O METODO AINDA PODE LANÇAR EXCEPTION
        try {
            // O método gerarUrlPagamentoPedidoPublico AGORA está no FinanceiroService
            // e deve ser refatorado para usar o Stripe. O try/catch é para capturar StripeException.
            return financeiroService.gerarUrlPagamentoPedidoPublico(pedidoUuid, totalComFrete);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar URL de pagamento: " + e.getMessage(), e);
        }
    }


    /**
     * NOVO: Finaliza o Pedido após o pagamento externo ser aprovado via Webhook.
     */
    @Transactional
    public Pedido finalizarPedidoAprovado(UUID pedidoUuid, TipoPagamento tipoPagamento) {
        Pedido pedido = pedidoRepository.findByUuid(pedidoUuid)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + pedidoUuid));

        // Verifica se o pedido está em status de aguardando pagamento externo
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PGTO_LIMITE) {
            System.err.println("Webhook: Pedido #" + pedido.getId() + " não estava em status AGUARDANDO_PGTO_LIMITE. Status atual: " + pedido.getStatus());
            return pedido;
        }

        // 1. Atualiza o status e a forma de pagamento
        pedido.setStatus(StatusPedido.EM_PREPARO); // Direto para preparo (Pagamento confirmado)
        pedido.setTipoPagamento(tipoPagamento);

        // 2. Incrementa o contador (regra de monetização)
        Restaurante restaurante = pedido.getRestaurante();
        boolean isLimitedPlan = restaurante.getPlano().equals("GRATUITO");
        boolean shouldCheckLimit = !restaurante.isLegacyFree() && !restaurante.isBetaTester() && isLimitedPlan;

        if (shouldCheckLimit) {
            restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
            restauranteRepository.save(restaurante);
        }

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido aceitarPedidoRetido(Long pedidoId) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado."));

        if (!pedido.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        // Verifica se o pedido está no status correto antes de aceitar
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PGTO_LIMITE) {
            throw new RuntimeException("O pedido não está no status de retenção (AGUARDANDO_PGTO_LIMITE).");
        }

        // NOVO STATUS: PENDENTE (se a impressão estiver ativada) ou EM_PREPARO (se não)
        StatusPedido novoStatus = restaurante.isImpressaoDeliveryAtivada() ? StatusPedido.PENDENTE : StatusPedido.EM_PREPARO;

        pedido.setStatus(novoStatus);

        return pedidoRepository.save(pedido);
    }

    public Pedido rastrearPedidoPorUuid(UUID uuid) {
        return pedidoRepository.findByUuid(uuid).orElse(null);
    }
}