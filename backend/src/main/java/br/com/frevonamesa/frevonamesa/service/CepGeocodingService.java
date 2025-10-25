package br.com.frevonamesa.frevonamesa.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CepGeocodingService {

    private final RestTemplate restTemplate;
    private static final String GEOLOCATION_API_URL = "https://cep.awesomeapi.com.br/json/{cep}";
    private final Map<String, double[]> cacheCoordenadas = new HashMap<>();

    public CepGeocodingService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Busca as coordenadas (Latitude e Longitude) para um dado CEP.
     */
    public double[] buscarCoordenadas(String cep) {
        String cepLimpo = cep.replaceAll("\\D", "");

        if (cepLimpo.length() != 8) {
            throw new RuntimeException("CEP inválido. Deve conter 8 dígitos.");
        }

        if (cacheCoordenadas.containsKey(cepLimpo)) {
            return cacheCoordenadas.get(cepLimpo);
        }

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                    GEOLOCATION_API_URL,
                    JsonNode.class,
                    cepLimpo
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode body = response.getBody();
                String latStr = body.get("lat").asText();
                String lngStr = body.get("lng").asText();

                double latitude = Double.parseDouble(latStr);
                double longitude = Double.parseDouble(lngStr);

                double[] coordenadas = {latitude, longitude};

                cacheCoordenadas.put(cepLimpo, coordenadas);
                return coordenadas;
            }

            throw new RuntimeException("CEP não encontrado ou erro na API externa.");

        } catch (Exception e) {
            System.err.println("Erro ao buscar coordenadas para o CEP " + cepLimpo + ": " + e.getMessage());
            throw new RuntimeException("Erro ao buscar CEP. Verifique o número digitado.");
        }
    }
}