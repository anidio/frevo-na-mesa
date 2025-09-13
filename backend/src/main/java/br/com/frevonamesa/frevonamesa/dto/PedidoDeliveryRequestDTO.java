package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.util.List;

@Data
public class PedidoDeliveryRequestDTO {
    private String nomeCliente;
    private String telefoneCliente;
    private String enderecoCliente;

    // Lista de itens do pedido (reaproveitando o DTO que já temos)
    private List<ItemPedidoRequestDTO> itens;
}