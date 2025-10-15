package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.AreaEntregaDTO;
import br.com.frevonamesa.frevonamesa.model.AreaEntrega;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.AreaEntregaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Comparator; // NOVO IMPORT

@Service
public class AreaEntregaService {

    @Autowired
    private AreaEntregaRepository areaEntregaRepository;

    @Autowired
    private RestauranteService restauranteService;

    // SIMULAÇÃO: No ambiente de produção, esta função chamaria uma API externa
    // (Ex: Google Maps Distance Matrix) para calcular a distância REAL do Restaurante ao CEP.
    private Double simularDistanciaKm(Long restauranteId, String cepCliente) {
        // Lógica de simulação baseada no tamanho do CEP para fins de demonstração
        // Em um cenário real, o CEP seria geocodificado para Lat/Long e a distância calculada.

        String cepNormalizado = cepCliente.replaceAll("[^0-9]", "");

        if (cepNormalizado.length() < 8) return -2.0; // CEP Inválido

        // Simula uma lógica simples de distância baseada nos 3 primeiros dígitos do CEP.
        // O valor 50 é um chute.
        try {
            int base = Integer.parseInt(cepNormalizado.substring(0, 3));
            return (double) ((base % 10) + 1) / 2.0; // Distância entre 0.5 e 5 km (simulação)
        } catch (NumberFormatException e) {
            return 10.0; // Distância padrão em caso de erro na simulação
        }
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
        // ALTERADO: Agora salva a distância máxima em KM
        novaArea.setMaxDistanceKm(dto.getMaxDistanceKm());
        novaArea.setValorEntrega(dto.getValorEntrega());
        novaArea.setValorMinimoPedido(dto.getValorMinimoPedido());
        novaArea.setRestaurante(restaurante);

        return areaEntregaRepository.save(novaArea);
    }

    // NOVO: Método de atualização para faixas de distância
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

    // NOVO: Método para deletar (necessário para o frontend)
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
     * Lógica CRÍTICA ATUALIZADA: Calcula a taxa de entrega baseada na DISTÂNCIA REAL (simulada).
     * @param restauranteId ID do restaurante.
     * @param cepCliente CEP fornecido pelo cliente.
     * @return BigDecimal da taxa de entrega ou -1.00 se a entrega for indisponível.
     */
    public BigDecimal calcularTaxa(Long restauranteId, String cepCliente) {
        Double distanciaKm = simularDistanciaKm(restauranteId, cepCliente);

        // Retorna um valor negativo se o CEP for inválido na simulação
        if (distanciaKm == -2.0) {
            return new BigDecimal("-2.00");
        }

        List<AreaEntrega> faixas = areaEntregaRepository.findByRestauranteId(restauranteId);

        // Ordena as faixas pela distância máxima para garantir que a lógica funcione corretamente
        faixas.sort(Comparator.comparing(AreaEntrega::getMaxDistanceKm));

        // 1. Procura a faixa que abrange a distância
        for (AreaEntrega faixa : faixas) {
            if (distanciaKm <= faixa.getMaxDistanceKm()) {
                return faixa.getValorEntrega();
            }
        }

        // 2. Se nenhuma faixa for encontrada (distância maior que todas as faixas), assume que a entrega não é possível
        return new BigDecimal("-1.00");
    }
}