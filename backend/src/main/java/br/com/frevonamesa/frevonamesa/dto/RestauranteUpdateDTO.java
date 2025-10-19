package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;

@Data
public class RestauranteUpdateDTO {
    private String nome;
    private String endereco;
    private String logoUrl;
    private String cepRestaurante;
}