package br.com.frevonamesa.frevonamesa.dto;

import br.com.frevonamesa.frevonamesa.model.TipoEstabelecimento;
import lombok.Data;

@Data
public class RestauranteDTO {
    private String nome;
    private String email;
    private String senha;
    private String endereco;
    private TipoEstabelecimento tipo;
}