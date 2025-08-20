package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.MesaRequestDTO;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.StatusMesa;
import br.com.frevonamesa.frevonamesa.model.TipoPagamento;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository;

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

    public List<Mesa> listarTodas() {
        return mesaRepository.findAll();
    }

    public Optional<Mesa> buscarPorId(Long id) {
        return mesaRepository.findById(id);
    }

    @Transactional
    public Mesa atualizarStatus(Long id, StatusMesa novoStatus) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        mesa.setStatus(novoStatus);

        if (novoStatus == StatusMesa.LIVRE) {
            // 1. Zera o valor total para o próximo cliente
            mesa.setValorTotal(BigDecimal.ZERO);
            // 2. Limpa o nome do cliente para o próximo atendimento
            mesa.setNomeCliente(null);
            // 3. Desvincula os pedidos antigos desta instância da mesa para o próximo atendimento.
            //    Os pedidos CONTINUARÃO no banco de dados para o relatório.
            mesa.getPedidos().clear();
        }

        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa processarPagamento(Long id, TipoPagamento tipo) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        if (mesa.getStatus() != StatusMesa.OCUPADA) {
            throw new RuntimeException("Apenas mesas ocupadas podem ter o pagamento processado.");
        }

        for (Pedido pedido : mesa.getPedidos()) {
            // No futuro, poderíamos verificar se o pedido já foi pago
            pedido.setTipoPagamento(tipo);
            pedidoRepository.save(pedido);
        }

        mesa.setStatus(StatusMesa.PAGA);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa atualizarNomeCliente(Long id, String nomeCliente) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada!"));

        mesa.setNomeCliente(nomeCliente);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa criarMesa(MesaRequestDTO dto) {
        // Regra de Negócio: Verifica se já existe uma mesa com este número
        if (mesaRepository.existsByNumero(dto.getNumero())) {
            throw new RuntimeException("Já existe uma mesa com o número " + dto.getNumero());
        }

        Mesa novaMesa = new Mesa();
        novaMesa.setNumero(dto.getNumero());
        novaMesa.setStatus(StatusMesa.LIVRE);
        novaMesa.setValorTotal(BigDecimal.ZERO);
        novaMesa.setPedidos(new ArrayList<>()); // Inicializa a lista de pedidos vazia

        return mesaRepository.save(novaMesa);
    }

    @Transactional
    public Mesa atualizarNumeroMesa(Long id, int novoNumero) {
        // Busca a mesa que queremos editar
        Mesa mesaParaAtualizar = mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa com ID " + id + " não encontrada!"));

        // Se o número não mudou, não faz nada
        if (mesaParaAtualizar.getNumero() == novoNumero) {
            return mesaParaAtualizar;
        }

        // REGRA DE NEGÓCIO: Verifica se o novo número já está em uso por OUTRA mesa
        if (mesaRepository.existsByNumero(novoNumero)) {
            throw new RuntimeException("O número de mesa " + novoNumero + " já está em uso!");
        }

        // Se a validação passar, atualiza o número e salva
        mesaParaAtualizar.setNumero(novoNumero);
        return mesaRepository.save(mesaParaAtualizar);
    }
}