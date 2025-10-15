package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProdutoCardapioDTO {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private String imageUrl;
}