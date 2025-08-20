package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RelatorioItemPedidoDTO {
    private int quantidade;
    private String nomeProduto;
    private String observacao;
    private BigDecimal subtotal;
}
