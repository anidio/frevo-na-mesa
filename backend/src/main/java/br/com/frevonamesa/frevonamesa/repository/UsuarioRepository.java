package br.com.frevonamesa.frevonamesa.repository;

import br.com.frevonamesa.frevonamesa.model.Role; // NOVO IMPORT
import br.com.frevonamesa.frevonamesa.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // NOVO IMPORT
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    // NOVO: Conta todos os usuários de um restaurante (Usado na trava de limite)
    long countByRestauranteId(Long restauranteId);

    // NOVO: Lista todos os usuários de um restaurante (Usado no painel Admin)
    List<Usuario> findAllByRestauranteId(Long restauranteId);

    // NOVO: Conta/Busca usuários por função (Usado na regra 'Não deletar o último Admin')
    List<Usuario> findAllByRestauranteIdAndRole(Long restauranteId, Role role);
}