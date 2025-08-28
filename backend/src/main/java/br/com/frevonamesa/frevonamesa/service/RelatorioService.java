package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado: " + email));
    }

    public RelatorioDiarioDTO gerarRelatorioDoDia() {
        Restaurante restaurante = getRestauranteLogado();
        LocalDateTime inicioDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime fimDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // Busca todos os pedidos pagos hoje E que pertencem ao restaurante logado
        List<Pedido> pedidosPagosHoje = pedidoRepository.findAllByDataHoraBetweenAndTipoPagamentoIsNotNull(inicioDoDia, fimDoDia)
                .stream()
                .filter(pedido -> pedido.getMesa().getRestaurante().getId().equals(restaurante.getId()))
                .collect(Collectors.toList());

        // O resto da lógica para montar o DTO permanece a mesma, pois já está baseada na lista filtrada
        Map<Mesa, List<Pedido>> pedidosPorMesa = pedidosPagosHoje.stream()
                .collect(Collectors.groupingBy(Pedido::getMesa));

        List<RelatorioMesaDTO> mesasAtendidas = pedidosPorMesa.entrySet().stream()
                .map(entry -> {
                    Mesa mesa = entry.getKey();
                    List<Pedido> pedidosDaMesa = entry.getValue();

                    List<RelatorioPedidoDTO> pedidosDto = pedidosDaMesa.stream().map(pedido -> {
                        List<RelatorioItemPedidoDTO> itensDto = pedido.getItens().stream().map(item -> {
                            RelatorioItemPedidoDTO itemDto = new RelatorioItemPedidoDTO();
                            itemDto.setQuantidade(item.getQuantidade());
                            itemDto.setNomeProduto(item.getProduto().getNome());
                            itemDto.setObservacao(item.getObservacao());
                            itemDto.setSubtotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
                            return itemDto;
                        }).collect(Collectors.toList());

                        RelatorioPedidoDTO pedidoDto = new RelatorioPedidoDTO();
                        pedidoDto.setId(pedido.getId());
                        pedidoDto.setTipoPagamento(pedido.getTipoPagamento().toString());
                        pedidoDto.setTotalPedido(pedido.getTotal());
                        pedidoDto.setItens(itensDto);
                        return pedidoDto;
                    }).collect(Collectors.toList());

                    RelatorioMesaDTO mesaDto = new RelatorioMesaDTO();
                    mesaDto.setNumeroMesa(mesa.getNumero());
                    mesaDto.setNomeCliente(mesa.getNomeCliente());
                    mesaDto.setPedidos(pedidosDto);
                    return mesaDto;
                }).collect(Collectors.toList());

        BigDecimal faturamentoTotal = pedidosPagosHoje.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> faturamentoPorTipo = pedidosPagosHoje.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getTipoPagamento().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Pedido::getTotal, BigDecimal::add)
                ));

        RelatorioDiarioDTO relatorioFinal = new RelatorioDiarioDTO();
        relatorioFinal.setData(LocalDate.now());
        relatorioFinal.setFaturamentoTotal(faturamentoTotal);
        relatorioFinal.setFaturamentoPorTipoPagamento(faturamentoPorTipo);
        relatorioFinal.setMesasAtendidas(mesasAtendidas);

        return relatorioFinal;
    }
}