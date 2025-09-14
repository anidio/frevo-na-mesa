package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.util.List; // Adicionar import

@Data
public class ItemPedidoRequestDTO {
    private Long produtoId;
    private int quantidade;
    private String observacao;
    private List<Long> adicionaisIds;
}