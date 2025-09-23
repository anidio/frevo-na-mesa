package br.com.frevonamesa.frevonamesa.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WhatsappService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void enviarMensagemTemplate(String phoneNumberId, String apiToken, String numeroDestino, String templateName, Map<String, String> parametros) {
        if (phoneNumberId == null || phoneNumberId.isEmpty() || apiToken == null || apiToken.isEmpty()) {
            System.err.println("Atenção: Credenciais do WhatsApp não configuradas para o restaurante. Mensagem não enviada.");
            return;
        }

        String apiUrl = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

        // Remove caracteres não-numéricos e adiciona o código do país se não estiver presente (ex: 5581...)
        String numeroLimpo = numeroDestino.replaceAll("[^0-9]", "");
        if (numeroLimpo.length() == 9) {
            numeroLimpo = "55" + numeroLimpo;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);

        // Constrói os parâmetros do template
        List<Map<String, Object>> components = List.of(
                Map.of(
                        "type", "body",
                        "parameters", parametros.entrySet().stream()
                                .map(entry -> Map.of("type", "text", "text", entry.getValue()))
                                .collect(Collectors.toList())
                )
        );

        // Constrói o payload da requisição
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", numeroLimpo);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        template.put("language", Collections.singletonMap("code", "pt_BR"));
        template.put("components", components);

        payload.put("template", template);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(apiUrl, request, String.class);
            System.out.println("Mensagem template '" + templateName + "' enviada para " + numeroDestino);
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem para " + numeroDestino + ": " + e.getMessage());
        }
    }
}