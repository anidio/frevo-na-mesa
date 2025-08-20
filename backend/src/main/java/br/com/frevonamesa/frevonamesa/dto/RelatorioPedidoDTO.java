package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RelatorioPedidoDTO {
    private Long id;
    private String tipoPagamento;
    private BigDecimal totalPedido;
    private List<RelatorioItemPedidoDTO> itens;
}
