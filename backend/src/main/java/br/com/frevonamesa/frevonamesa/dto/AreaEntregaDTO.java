package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AreaEntregaDTO {
    private Long id;
    private String nome;
    private Double maxDistanceKm;
    private BigDecimal valorEntrega;
    private BigDecimal valorMinimoPedido;
}