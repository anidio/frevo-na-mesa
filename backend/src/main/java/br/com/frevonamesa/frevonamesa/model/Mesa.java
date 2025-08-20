package br.com.frevonamesa.frevonamesa.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Mesa {

    @Id // Marca o campo como a chave primária (identificador único)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Pede ao banco para gerar o ID automaticamente
    private Long id;

    private int numero;
    private String nomeCliente;

    @Enumerated(EnumType.STRING) // Salva o status como texto ("LIVRE") em vez de número (0)
    private StatusMesa status;

    private BigDecimal valorTotal; // Usamos BigDecimal para precisão monetária

    private LocalTime horaAbertura;

    @OneToMany(mappedBy = "mesa")
    @JsonManagedReference
    private List<Pedido> pedidos;

    // Construtor vazio, exigido pelo JPA (a tecnologia de banco de dados)
    public Mesa() {
    }

    // Construtor que usaremos no DataInitializer para criar mesas facilmente
    public Mesa(int numero, StatusMesa status, BigDecimal valorTotal, LocalTime horaAbertura) {
        this.numero = numero;
        this.status = status;
        this.valorTotal = valorTotal;
        this.horaAbertura = horaAbertura;
    }

    // --- Getters e Setters ---
    // Métodos para acessar e modificar os campos da classe
    // Você pode gerar na sua IDE (Alt+Insert no IntelliJ, por exemplo)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public StatusMesa getStatus() {
        return status;
    }

    public void setStatus(StatusMesa status) {
        this.status = status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalTime getHoraAbertura() {
        return horaAbertura;
    }

    public void setHoraAbertura(LocalTime horaAbertura) {
        this.horaAbertura = horaAbertura;
    }
    
}
