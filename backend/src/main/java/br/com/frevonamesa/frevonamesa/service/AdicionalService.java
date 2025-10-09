package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Adicional;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.AdicionalRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdicionalService {

    @Autowired
    private AdicionalRepository adicionalRepository;
    @Autowired
    private RestauranteService restauranteService; 

    public List<Adicional> listarPorRestaurante() {
        Restaurante restaurante = restauranteService.getRestauranteLogado(); // CHAMA O SERVIÇO CENTRAL
        return adicionalRepository.findByRestauranteId(restaurante.getId());
    }

    public Adicional salvar(Adicional adicional) {
        Restaurante restaurante = restauranteService.getRestauranteLogado(); // CHAMA O SERVIÇO CENTRAL
        adicional.setRestaurante(restaurante);
        return adicionalRepository.save(adicional);
    }

    public Adicional atualizar(Long id, Adicional adicional) {
        Restaurante restaurante = restauranteService.getRestauranteLogado(); // CHAMA O SERVIÇO CENTRAL
        Adicional existente = adicionalRepository.findById(id).orElseThrow(() -> new RuntimeException("Adicional não encontrado"));

        // VERIFICAÇÃO DE SEGURANÇA CORRIGIDA: Usa 'restaurante'
        if (!existente.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        existente.setNome(adicional.getNome());
        existente.setPreco(adicional.getPreco());
        return adicionalRepository.save(existente);
    }

    public void deletar(Long id) {
        Restaurante restaurante = restauranteService.getRestauranteLogado(); // CHAMA O SERVIÇO CENTRAL
        Adicional existente = adicionalRepository.findById(id).orElseThrow(() -> new RuntimeException("Adicional não encontrado"));

        // VERIFICAÇÃO DE SEGURANÇA CORRIGIDA: Usa 'restaurante'
        if (!existente.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }
        adicionalRepository.deleteById(id);
    }
}