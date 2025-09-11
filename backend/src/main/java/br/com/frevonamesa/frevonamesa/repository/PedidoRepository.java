package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByDataHoraBetweenAndTipoPagamentoIsNotNull(LocalDateTime inicio, LocalDateTime fim);

    // Encontra todos os pedidos de uma lista de mesas que ainda não foram impressos
    List<Pedido> findByMesaInAndImpressoIsFalse(List<Mesa> mesas);
}

