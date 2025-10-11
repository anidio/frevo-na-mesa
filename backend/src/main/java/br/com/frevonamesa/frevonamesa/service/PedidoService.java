package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.AdicionalRepository;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import br.com.frevonamesa.frevonamesa.repository.ProdutoRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

@Service
public class PedidoService {

    @Autowired private MesaRepository mesaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private AdicionalRepository adicionalRepository;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    @Autowired
    private RestauranteService restauranteService;

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

        // Se o restaurante NÃO for LEGADO E atingiu o limite de 30 pedidos
        if (!restaurante.isLegacyFree() && restaurante.getPedidosMesAtual() >= 5) {
            // Se o plano for o gratuito (Frevo GO!), trava e informa sobre a cobrança
            if (restaurante.getPlano().equals("GRATUITO")) {
                throw new RuntimeException("Limite de 30 pedidos mensais atingido! Pague o excedente (R$ 1,49/pedido) ou assine o Plano Delivery PRO.");
            }
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

        restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() + 1);
        restauranteRepository.save(restaurante);

        return pedidoSalvo;
    }

    public Map<StatusPedido, List<Pedido>> listarPedidosDeliveryPorStatus() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Map<StatusPedido, List<Pedido>> pedidosAgrupados = new HashMap<>();
        pedidosAgrupados.put(StatusPedido.PENDENTE, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PENDENTE, restaurante.getId()));
        pedidosAgrupados.put(StatusPedido.EM_PREPARO, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.EM_PREPARO, restaurante.getId()));
        pedidosAgrupados.put(StatusPedido.PRONTO_PARA_ENTREGA, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PRONTO_PARA_ENTREGA, restaurante.getId()));
        return pedidosAgrupados;
    }

    @Transactional
    public Pedido atualizarStatusPedidoDelivery(Long pedidoId, StatusPedido novoStatus) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (!pedido.getRestaurante().getId().equals(restaurante.getId()) || pedido.getTipo() != TipoPedido.DELIVERY) {
            throw new SecurityException("Acesso negado.");
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

    @Transactional
    public Pedido criarPedidoDeliveryCliente(PedidoDeliveryClienteDTO dto) {
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado!"));

        Pedido novoPedido = new Pedido();
        novoPedido.setUuid(UUID.randomUUID());
        novoPedido.setRestaurante(restaurante);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());
        novoPedido.setTipo(TipoPedido.DELIVERY);

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

        Pedido pedidoSalvo = pedidoRepository.save(novoPedido);

        return pedidoSalvo;
    }

    public Pedido rastrearPedidoPorUuid(UUID uuid) {
        return pedidoRepository.findByUuid(uuid).orElse(null);
    }
}