package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
public class AreaEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    @JsonBackReference
    private Restaurante restaurante;

    private String nome;

    // Armazena o CEP inicial (ex: 50000-000)
    private String cepInicial;

    // Armazena o CEP final (ex: 51999-999)
    private String cepFinal;

    @Column(nullable = false)
    private BigDecimal valorEntrega;

    // Adiciona uma taxa minima para pedidos
    private BigDecimal valorMinimoPedido;
}