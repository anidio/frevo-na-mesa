package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AreaEntregaDTO {
    private Long id;
    private String nome;
    private String cepInicial;
    private String cepFinal;
    private BigDecimal valorEntrega;
    private BigDecimal valorMinimoPedido;
}