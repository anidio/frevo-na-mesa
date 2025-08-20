package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class RelatorioDiarioDTO {
    private LocalDate data;
    private BigDecimal faturamentoTotal;
    private Map<String, BigDecimal> faturamentoPorTipoPagamento;
    private List<RelatorioMesaDTO> mesasAtendidas;
}

