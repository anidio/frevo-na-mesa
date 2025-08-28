package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.StatusMesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    long countByStatusAndRestauranteId(StatusMesa status, Long restauranteId);

    List<Mesa> findAllByStatusAndRestauranteId(StatusMesa status, Long restauranteId);

    long countByStatus(StatusMesa status);

    boolean existsByNumero(int numero);

    // NOVO: Busca todas as mesas de um restaurante específico
    List<Mesa> findByRestauranteId(Long restauranteId);

    // NOVO: Verifica se uma mesa com um certo número já existe para um restaurante específico
    boolean existsByNumeroAndRestauranteId(int numero, Long restauranteId);

}