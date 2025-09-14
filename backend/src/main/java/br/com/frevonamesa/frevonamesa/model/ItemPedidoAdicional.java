package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
public class ItemPedidoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_pedido_id")
    @JsonBackReference
    private ItemPedido itemPedido;

    // Guarda os dados do adicional no momento do pedido para histórico
    private String nomeAdicional;
    private BigDecimal precoAdicional;

    public ItemPedidoAdicional(ItemPedido itemPedido, Adicional adicional) {
        this.itemPedido = itemPedido;
        this.nomeAdicional = adicional.getNome();
        this.precoAdicional = adicional.getPreco();
    }
}