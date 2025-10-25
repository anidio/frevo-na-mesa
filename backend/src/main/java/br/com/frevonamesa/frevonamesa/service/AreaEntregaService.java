package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.AreaEntregaDTO;
import br.com.frevonamesa.frevonamesa.model.AreaEntrega;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.AreaEntregaRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // [NOVO] Injeção do serviço de Geocodificação
    @Autowired
    private CepGeocodingService cepGeocodingService;

    /**
     * Calcula a distância Haversine.
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Raio da Terra em km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distância em km
    }

    // ... (Métodos de CRUD omitidos por brevidade) ...

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
     * [NOVA LÓGICA CONDICIONAL] Calcula a taxa de entrega.
     */
    public BigDecimal calcularTaxa(Long restauranteId, String cepCliente) {

        Optional<Restaurante> restauranteOpt = restauranteRepository.findById(restauranteId);
        if (restauranteOpt.isEmpty()) {
            throw new RuntimeException("Restaurante de ID " + restauranteId + " não encontrado.");
        }
        Restaurante restaurante = restauranteOpt.get();

        // 1. [CRÍTICO] Se o cálculo Haversine estiver DESATIVADO, retorna a Taxa Fixa e ignora o CEP.
        if (!restaurante.isCalculoHaversineAtivo()) {
            return restaurante.getTaxaEntrega();
        }


        // 2. Continua para o cálculo Haversine (Se Ativo)
        String cepRestaurante = restaurante.getCepRestaurante();

        if (cepRestaurante == null || cepRestaurante.isBlank()) {
            throw new RuntimeException("O CEP do restaurante não está configurado para cálculo de distância.");
        }

        // 3. Obtém as coordenadas (pode lançar RuntimeException se o CEP for inválido/não encontrado)
        double[] coordRestaurante = cepGeocodingService.buscarCoordenadas(cepRestaurante);
        double[] coordCliente = cepGeocodingService.buscarCoordenadas(cepCliente);

        // 4. Calcula a distância real em linha reta (Haversine)
        double distanciaKm = calcularDistanciaHaversine(
                coordRestaurante[0],
                coordRestaurante[1],
                coordCliente[0],
                coordCliente[1]
        );

        // 5. Busca e ordena as áreas de entrega pela distância máxima (ascendente)
        List<AreaEntrega> areas = areaEntregaRepository.findByRestauranteId(restauranteId);

        if (areas.isEmpty()) {
            // Se o Haversine está ativo, mas não há faixas, retorna 0.00
            return BigDecimal.ZERO;
        }

        areas.sort(Comparator.comparing(AreaEntrega::getMaxDistanceKm));

        // 6. Encontra a primeira faixa que a distância se encaixa
        Optional<AreaEntrega> areaEncontrada = areas.stream()
                .filter(area -> distanciaKm <= area.getMaxDistanceKm())
                .findFirst();

        if (areaEncontrada.isPresent()) {
            return areaEncontrada.get().getValorEntrega();
        }

        // 7. Se a distância for maior que todas as áreas configuradas
        throw new RuntimeException("Entrega indisponível para este CEP (fora de área de cobertura). Distância calculada: " + String.format("%.2f", distanciaKm) + " km.");
    }
}