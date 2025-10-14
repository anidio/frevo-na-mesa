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
import java.util.stream.Collectors;

@Service
public class AreaEntregaService {

    @Autowired
    private AreaEntregaRepository areaEntregaRepository;

    @Autowired
    private RestauranteService restauranteService;

    // Helper: Normaliza CEP (remove hífens e pontos)
    public String normalizarCep(String cep) {
        // Garante que o CEP não é nulo antes de tentar normalizar
        if (cep == null) return "";
        return cep.replaceAll("[^0-9]", "");
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
        novaArea.setCepInicial(dto.getCepInicial());
        novaArea.setCepFinal(dto.getCepFinal());
        novaArea.setValorEntrega(dto.getValorEntrega());
        novaArea.setValorMinimoPedido(dto.getValorMinimoPedido());
        novaArea.setRestaurante(restaurante);

        return areaEntregaRepository.save(novaArea);
    }

    // Você pode adicionar métodos de atualizar e deletar aqui

    /**
     * Lógica CRÍTICA: Calcula a taxa de entrega baseada no CEP fornecido.
     * @param restauranteId ID do restaurante.
     * @param cepCliente CEP fornecido pelo cliente.
     * @return BigDecimal da taxa de entrega ou -1.00 se a entrega for indisponível.
     */
    public BigDecimal calcularTaxa(Long restauranteId, String cepCliente) {
        String cepNormalizado = normalizarCep(cepCliente);

        // Se o CEP tiver menos de 8 dígitos (após normalização), é inválido para busca.
        if (cepNormalizado.length() < 8) {
            // Retorna um valor negativo específico para ser tratado como 'CEP Inválido'
            return new BigDecimal("-2.00");
        }

        List<AreaEntrega> areas = areaEntregaRepository.findByRestauranteId(restauranteId);

        // 1. Procura a área que abrange o CEP
        for (AreaEntrega area : areas) {
            String inicio = normalizarCep(area.getCepInicial());
            String fim = normalizarCep(area.getCepFinal());

            // Lógica de range: (cep >= inicio) AND (cep <= fim)
            // A comparação lexicográfica funciona para CEPs.
            if (cepNormalizado.compareTo(inicio) >= 0 && cepNormalizado.compareTo(fim) <= 0) {
                return area.getValorEntrega();
            }
        }

        // 2. Se nenhuma área for encontrada, assume que a entrega não é possível
        return new BigDecimal("-1.00");
    }
}