package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.StatusPedido;
import br.com.frevonamesa.frevonamesa.model.TipoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByDataHoraBetweenAndTipoPagamentoIsNotNull(LocalDateTime inicio, LocalDateTime fim);

    List<Pedido> findByTipoAndStatusAndRestauranteId(TipoPedido tipo, StatusPedido status, Long restauranteId);
}

