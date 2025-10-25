package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor // Mantém o construtor sem argumentos exigido pelo JPA
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
    private List<Usuario> usuarios = new ArrayList<>(); // Inicializa a lista

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // **AJUSTE:** Define o valor padrão diretamente na declaração do campo
    private TipoEstabelecimento tipo = TipoEstabelecimento.MESAS_E_DELIVERY;

    private boolean impressaoDeliveryAtivada = true;
    private boolean impressaoMesaAtivada = true;

    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("restaurante-pedido")
    private List<Pedido> pedidos = new ArrayList<>(); // Inicializa a lista

    @Column(nullable = false)
    private String plano = "GRATUITO"; // Padrão

    @Column(nullable = false)
    private boolean isBetaTester = false;

    @Column(nullable = false)
    private boolean isLegacyFree = false;

    @Column(nullable = false)
    private boolean isDeliveryPro = false; // Padrão
    @Column(nullable = false)
    private boolean isSalaoPro = false; // Padrão

    // Limites de uso do plano GRATUITO/FREE (padrões)
    private Integer limiteMesas = 10;
    private Integer limiteUsuarios = 4; // Ajustado para incluir o admin + 3 (total 4)
    private Integer pedidosMesAtual = 0;

    private String whatsappNumber;
    private BigDecimal taxaEntrega = BigDecimal.ZERO; // Padrão

    // [CORREÇÃO CRÍTICA] Renomeado de 'isCalculoHaversineAtivo' para forçar o JPA a gravar.
    @Column(nullable = false)
    private boolean calculoHaversineAtivo = false;

    private String stripeCustomerId;
    private String stripeSubscriptionId;

    private LocalDateTime dataExpiracaoPlano;

    public Restaurante(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    // [ADICIONAL] Métodos explícitos para leitura do Frontend (isXxx) e persistência (setXxx)
    public boolean isCalculoHaversineAtivo() {
        return this.calculoHaversineAtivo;
    }

    public void setCalculoHaversineAtivo(boolean calculoHaversineAtivo) {
        this.calculoHaversineAtivo = calculoHaversineAtivo;
    }
}