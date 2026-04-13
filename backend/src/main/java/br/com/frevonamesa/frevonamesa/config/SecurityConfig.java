package br.com.frevonamesa.frevonamesa.config;

import br.com.frevonamesa.frevonamesa.config.jwt.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Autowired; // Necessário
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @org.springframework.beans.factory.annotation.Value("${APP_CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    /**
     * Exclui o Webhook, H2 e a rota raiz (que não precisam de CORS).
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/", // Rota principal
                "/h2-console/**", // Console H2
                "/api/financeiro/webhook/stripe" // Webhook (CRÍTICO: Deve ser ignorado para evitar 403)
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // Rotas ABERTAS / PÚBLICAS (MOVEMOS DE VOLTA AQUI para forçar o filtro CORS)
                        // OPTIONS é o preflight do CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/publico/**").permitAll()

                        // Rotas Autenticadas Gerais
                        .requestMatchers("/api/restaurante/meu-perfil").authenticated()
                        .requestMatchers("/api/categorias/**", "/api/produtos/**", "/api/adicionais/**").authenticated()
                        .requestMatchers("/api/mesas/**", "/api/pedidos/**").authenticated()
                        .requestMatchers("/api/caixa/dashboard").authenticated()

                        // Rotas Exclusivas ADMIN (RBAC mais granular nos Controllers com @PreAuthorize)
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/restaurante/configuracoes").hasRole("ADMIN")
                        .requestMatchers("/api/restaurante/perfil").hasRole("ADMIN")
                        .requestMatchers("/api/caixa/fechar").hasRole("ADMIN")
                        .requestMatchers("/api/relatorios/**").hasRole("ADMIN")
                        .requestMatchers("/api/areas-entrega/**").hasRole("ADMIN")

                        // Rotas Financeiras - ADMIN tem acesso a tudo
                        .requestMatchers("/api/financeiro/status-plano").hasRole("ADMIN")
                        .requestMatchers("/api/financeiro/upgrade/**").hasRole("ADMIN")
                        .requestMatchers("/api/financeiro/portal-session").hasRole("ADMIN")

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
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CORREÇÃO FINAL DO CORS: Define explicitamente as origens necessárias para o desenvolvimento local
        List<String> origins = new ArrayList<>();

        // 1. Origem Local (essencial para o seu dev)
        origins.add("http://localhost:5173");

        if (!allowedOrigins.equals("*")) {
            origins.add(allowedOrigins);
        } else {
            configuration.addAllowedOriginPattern("*"); // Fallback de segurança
        }

        // 2. Você pode adicionar a origem de produção aqui se necessário, ex: origins.add("https://seusite.com");

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica a configuração CORS para todas as rotas da API
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}