package br.com.frevonamesa.frevonamesa.dto;

import br.com.frevonamesa.frevonamesa.model.ItemPedido;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PedidoFilaDTO {
    private Long id;
    private LocalDateTime dataHora;
    private List<ItemPedido> itens;
    private MesaSimplesDTO mesa; // Usando nosso DTO simples
}