package br.com.frevonamesa.frevonamesa.dto;

import java.math.BigDecimal;

// Usamos @Data do Lombok para gerar Getters e Setters
import lombok.Data;

@Data
public class CaixaDashboardDTO {
    private long mesasAbertas;
    private BigDecimal totalEmAberto;
    private long mesasPagas; // Mesas com status PAGA
    private BigDecimal totalDoDia; // Simplificação: por enquanto, será o mesmo que o total em aberto
}
