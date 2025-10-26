package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*; // Certifique-se que usa jakarta.persistence
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String email;
    private String senha;
    private String endereco;
    private String cepRestaurante;
    private String logoUrl;

    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("restaurante-usuario")
    private List<Usuario> usuarios = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstabelecimento tipo = TipoEstabelecimento.MESAS_E_DELIVERY;

    private boolean impressaoDeliveryAtivada = true;
    private boolean impressaoMesaAtivada = true;

    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("restaurante-pedido")
    private List<Pedido> pedidos = new ArrayList<>();

    @Column(nullable = false)
    private String plano = "GRATUITO";

    @Column(nullable = false)
    private boolean isBetaTester = false;

    @Column(nullable = false)
    private boolean isLegacyFree = false;

    @Column(nullable = false)
    private boolean isDeliveryPro = false;
    @Column(nullable = false)
    private boolean isSalaoPro = false;

    private Integer limiteMesas = 10;
    private Integer limiteUsuarios = 4;
    private Integer pedidosMesAtual = 0;

    private String whatsappNumber;
    private BigDecimal taxaEntrega = BigDecimal.ZERO;

    // --- MUDANÇA AQUI: Adiciona @Column com nome explícito ---
    @Column(name = "calculo_haversine_ativo", nullable = false) // Mapeia para a coluna snake_case
    private boolean calculoHaversineAtivo = false;
    // --- FIM DA MUDANÇA ---

    private String stripeCustomerId;
    private String stripeSubscriptionId;

    private LocalDateTime dataExpiracaoPlano;

    // Construtor principal
    public Restaurante(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public boolean isCalculoHaversineAtivo() {
        return calculoHaversineAtivo;
    }

    public void setCalculoHaversineAtivo(boolean calculoHaversineAtivo) {
        this.calculoHaversineAtivo = calculoHaversineAtivo;
    }
}