package br.com.frevonamesa.frevonamesa.dto;

import java.util.List;

public class PedidoRequestDTO {
    private Long mesaId;
    private List<ItemPedidoRequestDTO> itens;

    // Getters e Setters
    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }
    public List<ItemPedidoRequestDTO> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequestDTO> itens) { this.itens = itens; }
}
