package br.com.frevonamesa.frevonamesa.dto;

import br.com.frevonamesa.frevonamesa.model.TipoEstabelecimento;
import lombok.Data;

@Data
public class RestaurantePerfilDTO {
    private Long id;
    private String nome;
    private String email;
    private String endereco;
    private TipoEstabelecimento tipo;
    private boolean impressaoDeliveryAtivada;
    private boolean impressaoMesaAtivada = true;
    private String whatsappNumber;
    private String plano;          // Ex: GRATUITO, DELIVERY_PRO, PREMIUM
    private Integer limiteUsuarios; // Limite para Garçons/Caixas
    private Integer limiteMesas;    // Limite para mesas criadas
    private boolean isLegacyFree;
    private boolean isBetaTester;
}