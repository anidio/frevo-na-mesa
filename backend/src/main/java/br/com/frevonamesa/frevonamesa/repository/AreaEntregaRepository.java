package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.AreaEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AreaEntregaRepository extends JpaRepository<AreaEntrega, Long> {
    List<AreaEntrega> findByRestauranteId(Long restauranteId);
}