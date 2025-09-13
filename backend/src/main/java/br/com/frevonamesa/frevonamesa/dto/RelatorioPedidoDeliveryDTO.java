package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RelatorioPedidoDeliveryDTO {
    private Long id;
    private String nomeCliente;
    private String tipoPagamento;
    private BigDecimal totalPedido;
    private List<RelatorioItemPedidoDTO> itens;
}