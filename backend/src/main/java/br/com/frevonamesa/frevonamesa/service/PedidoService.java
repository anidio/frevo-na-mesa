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

@Service
public class PedidoService {

    @Autowired private MesaRepository mesaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private AdicionalRepository adicionalRepository;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado: " + email));
    }

    @Transactional
    public Pedido criarPedido(PedidoRequestDTO dto) {
        Restaurante restaurante = getRestauranteLogado();
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
        Restaurante restaurante = getRestauranteLogado();
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
        Restaurante restaurante = getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (!pedido.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        pedido.setStatus(StatusPedido.CONFIRMADO);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido criarPedidoDelivery(PedidoDeliveryRequestDTO dto) {
        Restaurante restaurante = getRestauranteLogado();

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
        return pedidoSalvo;
    }

    public Map<StatusPedido, List<Pedido>> listarPedidosDeliveryPorStatus() {
        Restaurante restaurante = getRestauranteLogado();
        Map<StatusPedido, List<Pedido>> pedidosAgrupados = new HashMap<>();
        pedidosAgrupados.put(StatusPedido.PENDENTE, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PENDENTE, restaurante.getId()));
        pedidosAgrupados.put(StatusPedido.EM_PREPARO, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.EM_PREPARO, restaurante.getId()));
        pedidosAgrupados.put(StatusPedido.PRONTO_PARA_ENTREGA, pedidoRepository.findByTipoAndStatusAndRestauranteId(TipoPedido.DELIVERY, StatusPedido.PRONTO_PARA_ENTREGA, restaurante.getId()));
        return pedidosAgrupados;
    }

    @Transactional
    public Pedido atualizarStatusPedidoDelivery(Long pedidoId, StatusPedido novoStatus) {
        Restaurante restaurante = getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (!pedido.getRestaurante().getId().equals(restaurante.getId()) || pedido.getTipo() != TipoPedido.DELIVERY) {
            throw new SecurityException("Acesso negado.");
        }
        if (novoStatus == StatusPedido.FINALIZADO) {
            if (pedido.getTipoPagamento() == null) {
                pedido.setTipoPagamento(TipoPagamento.DINHEIRO);
            }
        }
        pedido.setStatus(novoStatus);

        // --- LÓGICA PARA ENVIAR MENSAGEM DO WHATSAPP ---
        String urlRastreamento = "https://frevo-na-mesa.com/rastrear/" + pedido.getUuid();
        String templateName;
        Map<String, String> parametros = new HashMap<>();
        parametros.put("pedido_id", String.valueOf(pedido.getId()));
        parametros.put("url_rastreamento", urlRastreamento);

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarUltimos10Finalizados() {
        Restaurante restaurante = getRestauranteLogado();
        return pedidoRepository.findTop10ByRestauranteIdAndTipoAndStatusOrderByDataHoraDesc(
                restaurante.getId(), TipoPedido.DELIVERY, StatusPedido.FINALIZADO);
    }

    @Transactional
    public Pedido imprimirPedidoDelivery(Long pedidoId) {
        Restaurante restaurante = getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (!pedido.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        pedido.setImpresso(true);
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidosDeliveryPendentes() {
        Restaurante restaurante = getRestauranteLogado();
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