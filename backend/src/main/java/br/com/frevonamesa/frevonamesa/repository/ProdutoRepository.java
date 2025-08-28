package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByRestauranteId(Long restauranteId);
}
