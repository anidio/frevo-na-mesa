package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String whatsappPhoneNumberId;
    private String whatsappApiToken;

    @Column(unique = true)
    private String email;
    private String senha;
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstabelecimento tipo;
    private boolean impressaoDeliveryAtivada = true;
    private boolean impressaoMesaAtivada = true;

    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("restaurante-pedido")
    private List<Pedido> pedidos;

    public Restaurante(String nome, String email, String senha, TipoEstabelecimento tipo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.tipo = tipo;
    }
}