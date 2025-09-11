package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.MesaSimplesDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoFilaDTO;
import br.com.frevonamesa.frevonamesa.dto.PedidoRequestDTO;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    @Autowired private MesaRepository mesaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private RestauranteRepository restauranteRepository;

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
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        Pedido novoPedido = new Pedido();
        novoPedido.setMesa(mesa);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());

        BigDecimal totalPedido = BigDecimal.ZERO;

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

            if (!produto.getRestaurante().getId().equals(restaurante.getId())) {
                throw new SecurityException("Acesso negado: O produto '" + produto.getNome() + "' não pertence ao seu restaurante.");
            }

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido);

            novoPedido.getItens().add(itemPedido);
            totalPedido = totalPedido.add(produto.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade())));
        }

        // CORREÇÃO FINAL: Apenas define o total no pedido. Não mexe na mesa.
        novoPedido.setTotal(totalPedido);

        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now());
        }

        pedidoRepository.save(novoPedido);
        mesaRepository.save(mesa);

        return novoPedido;
    }

    public List<PedidoFilaDTO> listarPedidosNaoImpressos() {
        Restaurante restaurante = getRestauranteLogado();
        List<Mesa> mesasDoRestaurante = mesaRepository.findByRestauranteId(restaurante.getId());
        List<Pedido> pedidos = pedidoRepository.findByMesaInAndImpressoIsFalse(mesasDoRestaurante);

        // Converte a lista de Pedido para uma lista de PedidoFilaDTO
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
    public Pedido marcarComoImpresso(Long pedidoId) {
        Restaurante restaurante = getRestauranteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getMesa() == null || !pedido.getMesa().getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: este pedido não pertence ao seu restaurante.");
        }

        if (!pedido.isImpresso()) {
            Mesa mesa = pedido.getMesa();
            mesa.setValorTotal(mesa.getValorTotal().add(pedido.getTotal()));
            mesaRepository.save(mesa);
        }

        pedido.setImpresso(true);
        return pedidoRepository.save(pedido);
    }
}