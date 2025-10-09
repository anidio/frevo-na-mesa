package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.TipoPedido;
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

    @Autowired
    private RestauranteService restauranteService;


    private RelatorioDeliveryDTO gerarRelatorioDeliveryDoDia(Restaurante restaurante, LocalDateTime inicioDoDia, LocalDateTime fimDoDia) {
        List<Pedido> pedidosDeliveryPagos = pedidoRepository.findAllByRestauranteIdAndTipoAndDataHoraBetweenAndTipoPagamentoIsNotNull(
                restaurante.getId(), TipoPedido.DELIVERY, inicioDoDia, fimDoDia);

        if (pedidosDeliveryPagos.isEmpty()) {
            return null;
        }

        BigDecimal faturamentoTotal = pedidosDeliveryPagos.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> faturamentoPorTipo = pedidosDeliveryPagos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getTipoPagamento().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Pedido::getTotal, BigDecimal::add)
                ));

        List<RelatorioPedidoDeliveryDTO> pedidosDto = pedidosDeliveryPagos.stream().map(pedido -> {
            List<RelatorioItemPedidoDTO> itensDto = pedido.getItens().stream().map(item -> {
                RelatorioItemPedidoDTO itemDto = new RelatorioItemPedidoDTO();
                itemDto.setQuantidade(item.getQuantidade());
                itemDto.setNomeProduto(item.getProduto().getNome());
                itemDto.setObservacao(item.getObservacao());
                itemDto.setSubtotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
                return itemDto;
            }).collect(Collectors.toList());

            RelatorioPedidoDeliveryDTO pedidoDto = new RelatorioPedidoDeliveryDTO();
            pedidoDto.setId(pedido.getId());
            pedidoDto.setNomeCliente(pedido.getNomeClienteDelivery());
            pedidoDto.setTipoPagamento(pedido.getTipoPagamento().toString());
            pedidoDto.setTotalPedido(pedido.getTotal());
            pedidoDto.setItens(itensDto);
            return pedidoDto;
        }).collect(Collectors.toList());

        RelatorioDeliveryDTO relatorio = new RelatorioDeliveryDTO();
        relatorio.setFaturamentoTotal(faturamentoTotal);
        relatorio.setFaturamentoPorTipoPagamento(faturamentoPorTipo);
        relatorio.setPedidos(pedidosDto);
        return relatorio;
    }

    public RelatorioDiarioDTO gerarRelatorioDoDia() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        LocalDateTime inicioDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime fimDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<Pedido> pedidosPagosHoje = pedidoRepository.findAllByRestauranteIdAndTipoAndDataHoraBetweenAndTipoPagamentoIsNotNull(
                restaurante.getId(), TipoPedido.MESA, inicioDoDia, fimDoDia);

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

        relatorioFinal.setRelatorioDelivery(gerarRelatorioDeliveryDoDia(restaurante, inicioDoDia, fimDoDia));

        return relatorioFinal;
    }
}