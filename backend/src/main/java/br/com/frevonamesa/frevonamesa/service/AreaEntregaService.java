package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.AreaEntregaDTO;
import br.com.frevonamesa.frevonamesa.model.AreaEntrega;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.AreaEntregaRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;

@Service
public class AreaEntregaService {

    @Autowired
    private AreaEntregaRepository areaEntregaRepository;

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

    // COMENTADO: A chave da API do Google Maps não será usada por enquanto
    //@Value("${google.maps.api.key}")
    //private String googleMapsApiKey;

    // COMENTADO: Os métodos de integração externa também serão desativados
    // private final RestTemplate restTemplate = new RestTemplate();
    // private static final String GOOGLE_GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    /**
     * COMENTADO: O cálculo Haversine não é mais usado na taxa fixa.
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * COMENTADO: A busca de coordenadas foi desativada.
     */
    private double[] fetchCoordinates(String cep) {
        // Implementação removida/comentada para desativar a dependência externa.
        return null;
    }


    /**
     * COMENTADO: O cálculo de distância real foi desativado.
     */
    private Double obterDistanciaRealKm(String cepRestaurante, String cepCliente) {
        // Implementação removida/comentada para desativar a dependência externa.
        return 0.0;
    }


    public List<AreaEntrega> listarTodas() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        return areaEntregaRepository.findByRestauranteId(restaurante.getId());
    }

    @Transactional
    public AreaEntrega criar(AreaEntregaDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        AreaEntrega novaArea = new AreaEntrega();
        novaArea.setNome(dto.getNome());
        novaArea.setMaxDistanceKm(dto.getMaxDistanceKm());
        novaArea.setValorEntrega(dto.getValorEntrega());
        novaArea.setValorMinimoPedido(dto.getValorMinimoPedido());
        novaArea.setRestaurante(restaurante);

        return areaEntregaRepository.save(novaArea);
    }

    @Transactional
    public AreaEntrega atualizar(Long id, AreaEntregaDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        AreaEntrega existente = areaEntregaRepository.findById(id).orElseThrow(() -> new RuntimeException("Faixa de Distância não encontrada"));

        if (!existente.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        existente.setNome(dto.getNome());
        existente.setMaxDistanceKm(dto.getMaxDistanceKm());
        existente.setValorEntrega(dto.getValorEntrega());
        existente.setValorMinimoPedido(dto.getValorMinimoPedido());

        return areaEntregaRepository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        AreaEntrega existente = areaEntregaRepository.findById(id).orElseThrow(() -> new RuntimeException("Faixa de Distância não encontrada"));

        if (!existente.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        areaEntregaRepository.delete(existente);
    }


    /**
     * LÓGICA FINAL: Revertido para retornar a taxa FIXA cadastrada.
     * As faixas de distância (AreaEntrega) podem ser mantidas, mas serão ignoradas para o cálculo do frete.
     */
    public BigDecimal calcularTaxa(Long restauranteId, String cepCliente) {

        Optional<Restaurante> restauranteOpt = restauranteRepository.findById(restauranteId);
        if (restauranteOpt.isEmpty()) {
            throw new RuntimeException("Restaurante de ID " + restauranteId + " não encontrado.");
        }
        Restaurante restaurante = restauranteOpt.get();

        // Retorna a taxa fixa que está salva no modelo do Restaurante.
        return restaurante.getTaxaEntrega();
    }
}