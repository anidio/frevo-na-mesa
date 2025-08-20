package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.StatusMesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.frevonamesa.frevonamesa.model.Mesa;

@Repository
public interface  MesaRepository extends JpaRepository<Mesa, Long> {
    long countByStatus(StatusMesa status);

    boolean existsByNumero(int numero);
}
