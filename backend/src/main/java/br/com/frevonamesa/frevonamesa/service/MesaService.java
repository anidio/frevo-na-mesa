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

    @Autowired
    private RestauranteService restauranteService;

    @Transactional
    public Mesa atualizarStatus(Long id, StatusMesa novoStatus) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        mesa.setStatus(novoStatus);

        // AQUI ESTÁ A CORREÇÃO:
        // Quando a mesa fica livre, apenas resetamos seus valores.
        // NUNCA mais desvinculamos os pedidos antigos. O histórico é mantido.
        if (novoStatus == StatusMesa.LIVRE) {
            mesa.setValorTotal(BigDecimal.ZERO);
            mesa.setNomeCliente(null);
            mesa.setHoraAbertura(null);
            // NÃO mexemos mais na lista de pedidos aqui.
        }

        return mesaRepository.save(mesa);
    }

    // O resto do arquivo continua igual...
    public List<Mesa> listarTodas() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        return mesaRepository.findByRestauranteId(restaurante.getId());
    }

    public Optional<Mesa> buscarPorId(Long id) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);

        if (mesaOpt.isPresent() && mesaOpt.get().getRestaurante().getId().equals(restaurante.getId())) {
            return mesaOpt;
        }
        return Optional.empty();
    }

    @Transactional
    public Mesa processarPagamento(Long id, TipoPagamento tipo) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (!mesa.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        if (mesa.getStatus() != StatusMesa.OCUPADA) {
            throw new RuntimeException("Apenas mesas ocupadas podem ter o pagamento processado.");
        }

        // Pagamos apenas os pedidos que ainda não têm um tipo de pagamento definido
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
        Restaurante restaurante = restauranteService.getRestauranteLogado();
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
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Long restauranteId = restaurante.getId(); // Usado para clareza

        // --- LÓGICA DE TRAVA DE MONETIZAÇÃO (CORREÇÃO) ---
        long mesasAtuais = mesaRepository.findByRestauranteId(restauranteId).size();

        // Verifica se o restaurante NÃO é LEGADO (piloto) E se atingiu o limite do plano.
        if (!restaurante.isLegacyFree() && !restaurante.isBetaTester() && mesasAtuais >= restaurante.getLimiteMesas()) {
            throw new RuntimeException("Limite de " + restaurante.getLimiteMesas() + " mesas atingido para o seu plano! Atualize para o plano Salão PDV.");
        }

        if (mesaRepository.existsByNumeroAndRestauranteId(dto.getNumero(), restaurante.getId())) {
            throw new RuntimeException("Já existe uma mesa com o número " + dto.getNumero() + " para este restaurante.");
        }

        Mesa novaMesa = new Mesa();
        novaMesa.setNumero(dto.getNumero());
        novaMesa.setNomeCliente(dto.getNomeCliente());
        novaMesa.setStatus(StatusMesa.LIVRE);
        novaMesa.setValorTotal(BigDecimal.ZERO);
        novaMesa.setPedidos(new ArrayList<>());
        novaMesa.setRestaurante(restaurante);

        return mesaRepository.save(novaMesa);
    }

    @Transactional
    public Mesa atualizarNumeroMesa(Long id, int novoNumero) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Mesa mesaParaAtualizar = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa com ID " + id + " não encontrada!"));

        if (!mesaParaAtualizar.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Esta mesa não pertence ao seu restaurante.");
        }

        if (mesaParaAtualizar.getNumero() == novoNumero) {
            return mesaParaAtualizar;
        }

        if (mesaRepository.existsByNumeroAndRestauranteId(novoNumero, restaurante.getId())) {
            throw new RuntimeException("O número de mesa " + novoNumero + " já está em uso!");
        }

        mesaParaAtualizar.setNumero(novoNumero);
        return mesaRepository.save(mesaParaAtualizar);
    }
}