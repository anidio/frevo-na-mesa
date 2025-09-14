package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.util.List;

@Data
public class PedidoClienteDTO {
    private Long restauranteId;
    private int numeroMesa;
    private String nomeCliente;
    private List<ItemPedidoRequestDTO> itens;
}