package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.UsuarioDTO;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.Role;
import br.com.frevonamesa.frevonamesa.model.Usuario;
import br.com.frevonamesa.frevonamesa.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RestauranteService restauranteService;
    @Autowired private PasswordEncoder passwordEncoder;

    // Método para listar todos os usuários do restaurante logado (para o painel Admin)
    public List<Usuario> listarUsuariosDoRestaurante() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        // Assume-se que o UsuarioRepository terá o método findAllByRestauranteId
        return usuarioRepository.findAllByRestauranteId(restaurante.getId());
    }

    @Transactional
    public Usuario criarUsuario(UsuarioDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        // --- VERIFICAÇÃO DE LIMITE (REGRA #1 DO PLANO GRATUITO) ---
        long usuariosAtuais = usuarioRepository.countByRestauranteId(restaurante.getId());

        // Se o restaurante não for Legacy e o número atual for maior ou igual ao limite do plano, trava.
        // NOTE: O limiteUsuarios é definido no modelo Restaurante
        if (!restaurante.isSalaoPro() && !restaurante.isLegacyFree() && !restaurante.isBetaTester() && usuariosAtuais >= restaurante.getLimiteUsuarios()) {
            throw new RuntimeException("Limite de " + restaurante.getLimiteUsuarios() + " usuários ativos atingido! Atualize para o plano Salão PDV.");
        }
        // --- FIM VERIFICAÇÃO DE LIMITE ---

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.getNome());
        novoUsuario.setEmail(dto.getEmail());
        novoUsuario.setRole(dto.getRole());
        novoUsuario.setRestaurante(restaurante);
        novoUsuario.setSenha(passwordEncoder.encode(dto.getSenha()));

        return usuarioRepository.save(novoUsuario);
    }

    @Transactional
    public void deletarUsuario(Long usuarioId) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Garante que apenas o ADMIN (Restaurante) possa deletar seus próprios usuários.
        if (!usuario.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        // Não deve ser permitido deletar o último ADMIN (o dono do restaurante)
        if (usuario.getRole() == Role.ADMIN) {
            long adminCount = usuarioRepository.findAllByRestauranteIdAndRole(restaurante.getId(), Role.ADMIN).size();
            if (adminCount <= 1) {
                throw new RuntimeException("Não é possível deletar o último usuário administrador.");
            }
        }

        usuarioRepository.delete(usuario);
    }
}