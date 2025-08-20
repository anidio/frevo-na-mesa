package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.PedidoRequestDTO;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

@Service
public class PedidoService {

    @Autowired private MesaRepository mesaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;

    @Transactional // Garante que todas as operações com o banco sejam atômicas
    public Pedido criarPedido(PedidoRequestDTO dto) {
        // 1. Encontra a mesa
        Mesa mesa = mesaRepository.findById(dto.getMesaId())
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        // 2. Cria o objeto Pedido
        Pedido novoPedido = new Pedido();
        novoPedido.setMesa(mesa);
        novoPedido.setDataHora(LocalDateTime.now());
        novoPedido.setItens(new ArrayList<>());

        BigDecimal totalPedido = BigDecimal.ZERO;

        // 3. Itera sobre os itens do pedido recebidos
        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido); // Associa o item ao novo pedido

            novoPedido.getItens().add(itemPedido);
            totalPedido = totalPedido.add(produto.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade())));
        }

        // 4. Define o total do pedido e atualiza o total da mesa
        novoPedido.setTotal(totalPedido);
        mesa.setValorTotal(mesa.getValorTotal().add(totalPedido));
        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now()); // Pega a hora atual do servidor
        }

        // 5. Salva o pedido (os itens são salvos em cascata) e a mesa
        pedidoRepository.save(novoPedido);
        mesaRepository.save(mesa);

        return novoPedido;
    }
}
