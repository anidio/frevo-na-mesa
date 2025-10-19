package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private List<Usuario> usuarios;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstabelecimento tipo;
    private boolean impressaoDeliveryAtivada = true;
    private boolean impressaoMesaAtivada = true;

    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("restaurante-pedido")
    private List<Pedido> pedidos;

    @Column(nullable = false)
    private String plano = "GRATUITO"; // Ex: GRATUITO, DELIVERY_PRO, PREMIUM

    @Column(nullable = false)
    private boolean isBetaTester = false; // NOVO: Flag para Beta Tester

    @Column(nullable = false)
    private boolean isLegacyFree = false; // TRUE para clientes piloto (Grandfathering)

    @Column(nullable = false)
    private boolean isDeliveryPro = false; // Permite pedidos ilimitados
    @Column(nullable = false)
    private boolean isSalaoPro = false;

    // Limites de uso do plano GRATUITO/FREE
    private Integer limiteMesas = 10;
    private Integer limiteUsuarios = 4;
    private Integer pedidosMesAtual = 0; // Contador de pedidos para Pay-per-Use

    private String whatsappNumber;
    private BigDecimal taxaEntrega = BigDecimal.ZERO;

    public Restaurante(String nome, String email, String senha, TipoEstabelecimento tipo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.tipo = tipo;
        this.usuarios = new ArrayList<>();
    }
}