package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.MesaRequestDTO;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MesaService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado com o email: " + email));
    }

    public List<Mesa> listarTodas() {
        Restaurante restauranteLogado = getRestauranteLogado();
        return mesaRepository.findByRestauranteId(restauranteLogado.getId());
    }

    public Optional<Mesa> buscarPorId(Long id) {
        Restaurante restaurante = getRestauranteLogado();
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);

        if (mesaOpt.isPresent() && mesaOpt.get().getRestaurante().getId().equals(restaurante.getId())) {
            return mesaOpt;
        }
        return Optional.empty();
    }

    @Transactional
    public Mesa atualizarStatus(Long id, StatusMesa novoStatus) {
        Restaurante restaurante = getRestauranteLogado();
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        mesa.setStatus(novoStatus);

        if (novoStatus == StatusMesa.LIVRE) {
            // Zera os valores para o próximo cliente
            mesa.setValorTotal(BigDecimal.ZERO);
            mesa.setNomeCliente(null);
            mesa.setHoraAbertura(null);

            // Desvincula os pedidos antigos da mesa, sem apagá-los do sistema.
            // Isso garante que eles não aparecerão mais na tela da mesa, mas continuarão existindo para os relatórios.
            for (Pedido pedido : new ArrayList<>(mesa.getPedidos())) {
                pedido.setMesa(null); // Define a referência da mesa como nula no pedido
                pedidoRepository.save(pedido); // Salva o pedido desvinculado
            }
            mesa.getPedidos().clear(); // Limpa a lista de pedidos da instância atual da mesa
        }

        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa processarPagamento(Long id, TipoPagamento tipo) {
        Restaurante restaurante = getRestauranteLogado();
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        if (mesa.getStatus() != StatusMesa.OCUPADA) {
            throw new RuntimeException("Apenas mesas ocupadas podem ter o pagamento processado.");
        }

        for (Pedido pedido : mesa.getPedidos()) {
            if (pedido.getTipoPagamento() == null) {
                pedido.setTipoPagamento(tipo);
                pedidoRepository.save(pedido);
            }
        }

        mesa.setStatus(StatusMesa.PAGA);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa atualizarNomeCliente(Long id, String nomeCliente) {
        Restaurante restaurante = getRestauranteLogado();
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        mesa.setNomeCliente(nomeCliente);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa criarMesa(MesaRequestDTO dto) {
        Restaurante restauranteLogado = getRestauranteLogado();

        if (mesaRepository.existsByNumeroAndRestauranteId(dto.getNumero(), restauranteLogado.getId())) {
            throw new RuntimeException("Já existe uma mesa com o número " + dto.getNumero() + " para este restaurante.");
        }

        Mesa novaMesa = new Mesa();
        novaMesa.setNumero(dto.getNumero());
        novaMesa.setStatus(StatusMesa.LIVRE);
        novaMesa.setValorTotal(BigDecimal.ZERO);
        novaMesa.setPedidos(new ArrayList<>());
        novaMesa.setRestaurante(restauranteLogado);

        return mesaRepository.save(novaMesa);
    }

    @Transactional
    public Mesa atualizarNumeroMesa(Long id, int novoNumero) {
        Restaurante restauranteLogado = getRestauranteLogado();
        Mesa mesaParaAtualizar = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa com ID " + id + " não encontrada!"));

        if (!mesaParaAtualizar.getRestaurante().getId().equals(restauranteLogado.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        if (mesaParaAtualizar.getNumero() == novoNumero) {
            return mesaParaAtualizar;
        }

        if (mesaRepository.existsByNumeroAndRestauranteId(novoNumero, restauranteLogado.getId())) {
            throw new RuntimeException("O número de mesa " + novoNumero + " já está em uso!");
        }

        mesaParaAtualizar.setNumero(novoNumero);
        return mesaRepository.save(mesaParaAtualizar);
    }
}