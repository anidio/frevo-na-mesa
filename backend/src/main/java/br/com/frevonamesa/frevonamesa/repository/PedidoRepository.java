// backend/src/main/java/br/com/frevonamesa/frevonamesa/repository/PedidoRepository.java

package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Pedido;
import br.com.frevonamesa.frevonamesa.model.StatusPedido;
import br.com.frevonamesa.frevonamesa.model.TipoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importar Query
import org.springframework.data.repository.query.Param; // Importar Param

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection; // Importar Collection

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // --- Métodos existentes ---
    List<Pedido> findAllByDataHoraBetweenAndTipoPagamentoIsNotNull(LocalDateTime inicio, LocalDateTime fim);

    List<Pedido> findByTipoAndStatusAndRestauranteId(TipoPedido tipo, StatusPedido status, Long restauranteId);

    List<Pedido> findTop10ByRestauranteIdAndTipoAndStatusOrderByDataHoraDesc(Long restauranteId, TipoPedido tipo, StatusPedido status);

    List<Pedido> findAllByRestauranteIdAndTipoAndDataHoraBetweenAndTipoPagamentoIsNotNull(Long restauranteId, TipoPedido tipo, LocalDateTime inicio, LocalDateTime fim);

    Optional<Pedido> findByUuid(UUID uuid);

    /**
     * Busca pedidos de um tipo específico, dentro de uma lista de status,
     * para um restaurante, ordenados pela data/hora ascendente.
     * Usado para popular o painel Kanban de delivery.
     */
    List<Pedido> findByTipoAndStatusInAndRestauranteIdOrderByDataHoraAsc(
            TipoPedido tipo,
            Collection<StatusPedido> statuses,
            Long restauranteId
    );
}