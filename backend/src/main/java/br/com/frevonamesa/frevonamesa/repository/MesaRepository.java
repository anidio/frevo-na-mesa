package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.StatusMesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    long countByRestauranteId(Long restauranteId);

    long countByStatusAndRestauranteId(StatusMesa status, Long restauranteId);

    List<Mesa> findAllByStatusAndRestauranteId(StatusMesa status, Long restauranteId);

    long countByStatus(StatusMesa status);

    boolean existsByNumero(int numero);

    List<Mesa> findByRestauranteId(Long restauranteId);

    boolean existsByNumeroAndRestauranteId(int numero, Long restauranteId);

    Optional<Mesa> findByNumeroAndRestauranteId(int numero, Long restauranteId);
}