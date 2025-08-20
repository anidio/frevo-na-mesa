package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public RelatorioDiarioDTO gerarRelatorioDoDia() {
        // Define o intervalo de hoje (da meia-noite até o fim do dia)
        LocalDateTime inicioDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime fimDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // Busca todos os pedidos pagos hoje
        List<Pedido> pedidosPagosHoje = pedidoRepository.findAllByDataHoraBetweenAndTipoPagamentoIsNotNull(inicioDoDia, fimDoDia);

        // Agrupa os pedidos por mesa
        Map<Mesa, List<Pedido>> pedidosPorMesa = pedidosPagosHoje.stream()
                .collect(Collectors.groupingBy(Pedido::getMesa));

        // Monta o DTO de cada mesa
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

        // Calcula os totais de faturamento
        BigDecimal faturamentoTotal = pedidosPagosHoje.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> faturamentoPorTipo = pedidosPagosHoje.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getTipoPagamento().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Pedido::getTotal, BigDecimal::add)
                ));

        // Monta o DTO final
        RelatorioDiarioDTO relatorioFinal = new RelatorioDiarioDTO();
        relatorioFinal.setData(LocalDate.now());
        relatorioFinal.setFaturamentoTotal(faturamentoTotal);
        relatorioFinal.setFaturamentoPorTipoPagamento(faturamentoPorTipo);
        relatorioFinal.setMesasAtendidas(mesasAtendidas);

        return relatorioFinal;
    }
}
