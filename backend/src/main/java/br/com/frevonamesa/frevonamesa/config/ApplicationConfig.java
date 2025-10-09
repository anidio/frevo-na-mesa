package br.com.frevonamesa.frevonamesa.config;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.Usuario;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import br.com.frevonamesa.frevonamesa.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Configuration
public class ApplicationConfig {

    private final RestauranteRepository restauranteRepository;
    private final UsuarioRepository usuarioRepository;

    public ApplicationConfig(RestauranteRepository restauranteRepository, UsuarioRepository usuarioRepository) {
        this.restauranteRepository = restauranteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Tenta carregar o usuário da nova tabela (Garçom/Caixa/Admin)
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(username);
            if (usuarioOpt.isPresent()) {
                return usuarioOpt.get(); // Retorna a nova Entidade Usuario que implementa UserDetails
            }

            // Opcional: Manter o login do Restaurante (dono) na tabela antiga, mas
            // no futuro, o dono também será um Usuario Admin
            Restaurante restaurante = restauranteRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));

            return new org.springframework.security.core.userdetails.User( // ATENÇÃO: Mudança para User do Spring
                    restaurante.getEmail(),
                    restaurante.getSenha(),
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) // Define ROLE_ADMIN para o Restaurante
            );
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}