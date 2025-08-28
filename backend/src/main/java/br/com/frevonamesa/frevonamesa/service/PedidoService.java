package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.PedidoRequestDTO;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    @Autowired private RestauranteRepository restauranteRepository;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado: " + email));
    }

    @Transactional
    public Pedido criarPedido(PedidoRequestDTO dto) {
        Restaurante restaurante = getRestauranteLogado();

        // 1. Encontra a mesa
        Mesa mesa = mesaRepository.findById(dto.getMesaId())
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        // VALIDAÇÃO DE SEGURANÇA: Garante que a mesa pertence ao restaurante logado.
        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

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

            // VALIDAÇÃO DE SEGURANÇA: Garante que o produto também pertence ao restaurante.
            if (!produto.getRestaurante().getId().equals(restaurante.getId())) {
                throw new SecurityException("Acesso negado: O produto '" + produto.getNome() + "' não pertence ao seu restaurante.");
            }

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(produto);
            itemPedido.setQuantidade(itemDto.getQuantidade());
            itemPedido.setPrecoUnitario(produto.getPreco());
            itemPedido.setObservacao(itemDto.getObservacao());
            itemPedido.setPedido(novoPedido);

            novoPedido.getItens().add(itemPedido);
            totalPedido = totalPedido.add(produto.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade())));
        }

        // 4. Define o total do pedido e atualiza o total da mesa
        novoPedido.setTotal(totalPedido);
        mesa.setValorTotal(mesa.getValorTotal().add(totalPedido));
        if (mesa.getStatus() == StatusMesa.LIVRE) {
            mesa.setStatus(StatusMesa.OCUPADA);
            mesa.setHoraAbertura(LocalTime.now());
        }

        // 5. Salva o pedido (os itens são salvos em cascata) e a mesa
        pedidoRepository.save(novoPedido);
        mesaRepository.save(mesa);

        return novoPedido;
    }
}