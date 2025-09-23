package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;

@Data
public class RestauranteSettingsDTO {
    private boolean impressaoMesaAtivada;
    private boolean impressaoDeliveryAtivada;
    private String whatsappPhoneNumberId;
    private String whatsappApiToken;
}