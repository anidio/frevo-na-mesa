package br.com.frevonamesa.frevonamesa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    @JsonBackReference("restaurante-pedido") // Damos um nome para diferenciar da outra JsonBackReference
    private Restaurante restaurante;

    @ManyToOne // Muitos pedidos podem pertencer a UMA mesa.
    @JoinColumn(name = "mesa_id") // Chave estrangeira no banco
    @JsonBackReference
    private Mesa mesa;

    @Enumerated(EnumType.STRING) // Salva o texto (ex: "PIX") no banco
    private TipoPagamento tipoPagamento;

    // cascade = CascadeType.ALL: Se o pedido for salvo, os itens também são.
    // orphanRemoval = true: Se um item for removido da lista, ele é apagado do banco.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ItemPedido> itens;

    @Column(nullable = false)
    private boolean impresso = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPedido tipo;

    private BigDecimal total;
    private LocalDateTime dataHora;
    private String nomeClienteDelivery;
    private String telefoneClienteDelivery;
    private String enderecoClienteDelivery;
}
