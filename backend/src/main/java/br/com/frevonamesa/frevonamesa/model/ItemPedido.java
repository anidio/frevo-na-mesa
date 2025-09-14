// src/main/java/br/com/frevonamesa/frevonamesa/model/ItemPedido.java

package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor; // >>> NOVO: Importe o NoArgsConstructor

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "itemPedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ItemPedidoAdicional> adicionais = new ArrayList<>();


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
        BigDecimal precoDosAdicionais = adicionais.stream()
                .map(ItemPedidoAdicional::getPrecoAdicional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal precoTotalUnitario = this.precoUnitario.add(precoDosAdicionais);

        return precoTotalUnitario.multiply(BigDecimal.valueOf(this.quantidade));
    }
}