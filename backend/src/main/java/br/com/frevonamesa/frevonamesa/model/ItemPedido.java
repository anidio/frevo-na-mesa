// src/main/java/br/com/frevonamesa/frevonamesa/model/ItemPedido.java

package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor; // >>> NOVO: Importe o NoArgsConstructor

import java.math.BigDecimal;

@Data
@NoArgsConstructor // >>> NOVO: Adicione esta anotação
@Entity
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    @JsonBackReference
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private int quantidade;
    private BigDecimal precoUnitario;
    private String observacao;

    /**
     * Construtor customizado para facilitar a criação de itens no DataInitializer.
     */
    public ItemPedido(Pedido pedido, Produto produto, int quantidade, BigDecimal precoUnitario) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    /**
     * Método 'ajudante' para calcular o subtotal deste item.
     * A anotação @Transient diz ao JPA para ignorar este método e não criar
     * uma coluna "subtotal" no banco de dados.
     */
    @Transient
    public BigDecimal getSubtotal() {
        return this.precoUnitario.multiply(BigDecimal.valueOf(this.quantidade));
    }
}