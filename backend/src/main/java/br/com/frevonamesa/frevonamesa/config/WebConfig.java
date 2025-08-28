package br.com.frevonamesa.frevonamesa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // A anotação @Value continua lendo das suas variáveis de ambiente
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Aplica a todos os endpoints que começam com /api
                .allowedOrigins(allowedOrigins) // Permite requisições das origens configuradas
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Permite todos estes métodos HTTP
                .allowedHeaders("*") // Permite todos os tipos de cabeçalhos na requisição
                .allowCredentials(true) // Permite o envio de credenciais (como cookies ou tokens de autorização)
                .maxAge(3600); // Define o tempo que o navegador pode manter a resposta "pre-flight" em cache (em segundos)
    }
}