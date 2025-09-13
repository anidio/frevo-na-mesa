package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class RelatorioDeliveryDTO {
    private BigDecimal faturamentoTotal;
    private Map<String, BigDecimal> faturamentoPorTipoPagamento;
    private List<RelatorioPedidoDeliveryDTO> pedidos;
}