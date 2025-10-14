package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CardapioPublicoDTO {
    private String nomeRestaurante;
    private String enderecoRestaurante;
    private List<CategoriaCardapioDTO> categorias;
    private BigDecimal taxaEntrega;
}