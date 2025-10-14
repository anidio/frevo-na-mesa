package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RestauranteSettingsDTO {
    private boolean impressaoMesaAtivada;
    private boolean impressaoDeliveryAtivada;
    private String whatsappNumber;
    private BigDecimal taxaEntrega;
}