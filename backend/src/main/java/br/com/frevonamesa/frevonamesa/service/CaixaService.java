package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.CaixaDashboardDTO;
import br.com.frevonamesa.frevonamesa.dto.RelatorioDiarioDTO;
import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.StatusMesa;
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
import java.util.stream.IntStream;

@Service
@Transactional
public class CaixaService {

    @Autowired
    private MesaRepository mesaRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private RestauranteRepository restauranteRepository;
    @Autowired
    private RelatorioService relatorioService;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado com o email: " + email));
    }

    public CaixaDashboardDTO getDashboardInfo() {
        Restaurante restaurante = getRestauranteLogado();
        Long restauranteId = restaurante.getId();
        CaixaDashboardDTO dashboard = new CaixaDashboardDTO();

        long mesasOcupadas = mesaRepository.countByStatusAndRestauranteId(StatusMesa.OCUPADA, restauranteId);
        dashboard.setMesasAbertas(mesasOcupadas);

        long mesasPagas = mesaRepository.countByStatusAndRestauranteId(StatusMesa.PAGA, restauranteId);
        dashboard.setMesasPagas(mesasPagas);

        BigDecimal totalEmAberto = mesaRepository.findAllByStatusAndRestauranteId(StatusMesa.OCUPADA, restauranteId).stream()
                .map(Mesa::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalEmAberto(totalEmAberto);

        RelatorioDiarioDTO relatorio = relatorioService.gerarRelatorioDoDia();
        dashboard.setTotalDoDia(relatorio.getFaturamentoTotal());

        return dashboard;
    }

    public void fecharCaixa() {
        Restaurante restaurante = getRestauranteLogado();

        // 1. Encontra todos os pedidos do restaurante logado
        List<Mesa> mesasDoRestaurante = mesaRepository.findByRestauranteId(restaurante.getId());
        List<Pedido> pedidosParaDeletar = new ArrayList<>();
        for (Mesa mesa : mesasDoRestaurante) {
            pedidosParaDeletar.addAll(mesa.getPedidos());
        }

        // 2. Deleta os pedidos e mesas apenas deste restaurante
        pedidoRepository.deleteAll(pedidosParaDeletar);
        mesaRepository.deleteAll(mesasDoRestaurante);

        // 3. (Opcional) Recria mesas vazias para este restaurante
        // Esta lógica pode ser ajustada conforme a necessidade do negócio
        IntStream.rangeClosed(1, 10).forEach(numero -> {
            Mesa novaMesa = new Mesa();
            novaMesa.setNumero(numero);
            novaMesa.setStatus(StatusMesa.LIVRE);
            novaMesa.setValorTotal(BigDecimal.ZERO);
            novaMesa.setPedidos(new ArrayList<>());
            novaMesa.setRestaurante(restaurante); // Associa ao restaurante correto
            mesaRepository.save(novaMesa);
        });
    }
}