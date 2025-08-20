package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;

@Data
public class ItemPedidoRequestDTO {
    private Long produtoId;
    private int quantidade;
    private String observacao;
}