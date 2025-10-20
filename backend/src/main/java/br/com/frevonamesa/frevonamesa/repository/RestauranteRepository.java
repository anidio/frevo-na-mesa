package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    // Este método será crucial para o login
    Optional<Restaurante> findByEmail(String email);

    List<Restaurante> findAllByIsDeliveryProTrueOrIsSalaoProTrue();
}