// backend/src/main/java/br/com/frevonamesa/frevonamesa/config/SecurityConfig.java

package br.com.frevonamesa.frevonamesa.config;

import br.com.frevonamesa.frevonamesa.config.jwt.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer; // Correção no nome do import
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Rotas Abertas
                        .requestMatchers("/api/auth/**", "/h2-console/**", "/api/publico/**", "/api/financeiro/webhook/stripe").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Permite pre-flight requests

                        // Rotas Autenticadas Gerais
                        .requestMatchers("/api/restaurante/meu-perfil").authenticated()
                        .requestMatchers("/api/categorias/**", "/api/produtos/**", "/api/adicionais/**").authenticated() // Permitido para qualquer autenticado (ADMIN fará a trava no controller)
                        .requestMatchers("/api/mesas/**", "/api/pedidos/**").authenticated() // Rotas de Garçom/Caixa
                        .requestMatchers("/api/caixa/dashboard").authenticated() // Dashboard do Caixa

                        // Rotas Exclusivas ADMIN (RBAC mais granular nos Controllers com @PreAuthorize)
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/restaurante/configuracoes").hasRole("ADMIN")
                        .requestMatchers("/api/restaurante/perfil").hasRole("ADMIN") // Atualizar perfil geral
                        .requestMatchers("/api/caixa/fechar").hasRole("ADMIN") // Fechar caixa é Admin
                        .requestMatchers("/api/relatorios/**").hasRole("ADMIN") // Relatórios são Admin
                        .requestMatchers("/api/areas-entrega/**").hasRole("ADMIN") // Gerenciar áreas é Admin

                        // Rotas Financeiras - ADMIN tem acesso a tudo
                        .requestMatchers("/api/financeiro/status-plano").hasRole("ADMIN")
                        .requestMatchers("/api/financeiro/upgrade/**").hasRole("ADMIN") // Todos os upgrades
                        .requestMatchers("/api/financeiro/portal-session").hasRole("ADMIN") // <<< NOVO ENDPOINT DO PORTAL

                        // Rota Financeira - PayPerUse (Admin ou Caixa)
                        .requestMatchers("/api/financeiro/iniciar-pagamento").hasAnyRole("ADMIN", "CAIXA")

                        // Qualquer outra requisição precisa estar autenticada
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        // Permite H2 Console em iframe (apenas para dev/local)
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)); // Use sameOrigin em vez de disable para mais segurança

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Garante que as origins da variável de ambiente sejam usadas
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // Permite todos os headers comuns
        configuration.setAllowCredentials(true); // Necessário para cookies/tokens

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica a configuração CORS para todas as rotas da API
        source.registerCorsConfiguration("/api/**", configuration);
        // Pode ser necessário registrar para /h2-console/** se acessado de origem diferente
        // source.registerCorsConfiguration("/h2-console/**", configuration);
        return source;
    }
}