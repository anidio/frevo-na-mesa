package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;
import java.util.List;

@Data
public class PedidoDeliveryClienteDTO {
    private Long restauranteId;
    private String nomeCliente;
    private String telefoneCliente;
    private String enderecoCliente;
    private String pontoReferencia;
    private String cepCliente;
    private List<ItemPedidoRequestDTO> itens;
}