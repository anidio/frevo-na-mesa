package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.util.List;

@Data
public class RelatorioMesaDTO {
    private int numeroMesa;
    private String nomeCliente;
    private List<RelatorioPedidoDTO> pedidos;
}
