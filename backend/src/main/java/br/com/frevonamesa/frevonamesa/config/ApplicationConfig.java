package br.com.frevonamesa.frevonamesa.config;

import br.com.frevonamesa.frevonamesa.model.Restaurante; // Importe o modelo Restaurante
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User; // Importe o User do Spring
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList; // Importe ArrayList

@Configuration
public class ApplicationConfig {

    private final RestauranteRepository restauranteRepository;

    public ApplicationConfig(RestauranteRepository restauranteRepository) {
        this.restauranteRepository = restauranteRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // CORREÇÃO: Agora estamos convertendo o Restaurante em um UserDetails
        return username -> {
            Restaurante restaurante = restauranteRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));

            return new User(restaurante.getEmail(), restaurante.getSenha(), new ArrayList<>());
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