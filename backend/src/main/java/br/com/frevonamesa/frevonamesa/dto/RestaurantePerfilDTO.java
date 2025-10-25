package br.com.frevonamesa.frevonamesa.dto;

import br.com.frevonamesa.frevonamesa.model.TipoEstabelecimento;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RestaurantePerfilDTO {
    private Long id;
    private String nome;
    private String email;
    private String endereco;
    private String cepRestaurante;
    private String logoUrl;
    private TipoEstabelecimento tipo;
    private boolean impressaoDeliveryAtivada;
    private boolean impressaoMesaAtivada = true; // Mantém valor padrão se houver
    private String whatsappNumber;
    private String plano;
    private Integer limiteUsuarios;
    private Integer limiteMesas;
    private boolean isLegacyFree;
    private boolean isBetaTester;
    private boolean isDeliveryPro;
    private boolean isSalaoPro;
    private BigDecimal taxaEntrega;
    private String stripeSubscriptionId;
    private String stripeCustomerId;
}