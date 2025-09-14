package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoriaCardapioDTO {
    private String nome;
    private List<ProdutoCardapioDTO> produtos;
}