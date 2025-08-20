// src/main/java/br/com/frevonamesa/frevonamesa/service/CaixaService.java

package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.CaixaDashboardDTO;
import br.com.frevonamesa.frevonamesa.dto.RelatorioDiarioDTO;
import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.StatusMesa;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository;
import br.com.frevonamesa.frevonamesa.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.IntStream;

@Service
@Transactional
public class CaixaService {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RelatorioService relatorioService; // Injeta o RelatorioService

    public CaixaDashboardDTO getDashboardInfo() {
        CaixaDashboardDTO dashboard = new CaixaDashboardDTO();

        // Conta mesas abertas (status OCUPADA)
        long mesasOcupadas = mesaRepository.countByStatus(StatusMesa.OCUPADA);
        dashboard.setMesasAbertas(mesasOcupadas);

        // Conta mesas pagas (status PAGA)
        long mesasPagas = mesaRepository.countByStatus(StatusMesa.PAGA);
        dashboard.setMesasPagas(mesasPagas);

        // Calcula o valor total em aberto
        BigDecimal totalEmAberto = mesaRepository.findAll().stream()
                .filter(mesa -> mesa.getStatus() == StatusMesa.OCUPADA)
                .map(Mesa::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalEmAberto(totalEmAberto);

        // LÓGICA CORRIGIDA: Busca o faturamento real do dia a partir do RelatorioService
        RelatorioDiarioDTO relatorio = relatorioService.gerarRelatorioDoDia();
        dashboard.setTotalDoDia(relatorio.getFaturamentoTotal());

        return dashboard;
    }

    public void fecharCaixa() {
        // 1. Deleta todos os registros de pedidos e itens de pedidos (em cascata)
        pedidoRepository.deleteAll();

        // 2. Deleta todas as mesas existentes
        mesaRepository.deleteAll();

        // 3. Cria 10 novas mesas no estado LIVRE
        IntStream.rangeClosed(1, 10).forEach(numero -> {
            Mesa novaMesa = new Mesa();
            novaMesa.setNumero(numero);
            novaMesa.setStatus(StatusMesa.LIVRE);
            novaMesa.setValorTotal(BigDecimal.ZERO);
            novaMesa.setPedidos(new ArrayList<>());
            mesaRepository.save(novaMesa);
        });
    }
}