package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProdutoDTO {
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private String imageUrl;
    private Long categoriaId;
}
