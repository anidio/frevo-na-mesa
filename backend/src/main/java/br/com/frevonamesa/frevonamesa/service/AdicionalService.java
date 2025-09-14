package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Adicional;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.AdicionalRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdicionalService {

    @Autowired
    private AdicionalRepository adicionalRepository;
    @Autowired
    private RestauranteRepository restauranteRepository;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado"));
    }

    public List<Adicional> listarPorRestaurante() {
        return adicionalRepository.findByRestauranteId(getRestauranteLogado().getId());
    }

    public Adicional salvar(Adicional adicional) {
        adicional.setRestaurante(getRestauranteLogado());
        return adicionalRepository.save(adicional);
    }

    public Adicional atualizar(Long id, Adicional adicional) {
        Adicional existente = adicionalRepository.findById(id).orElseThrow(() -> new RuntimeException("Adicional não encontrado"));
        if (!existente.getRestaurante().getId().equals(getRestauranteLogado().getId())) {
            throw new SecurityException("Acesso negado.");
        }
        existente.setNome(adicional.getNome());
        existente.setPreco(adicional.getPreco());
        return adicionalRepository.save(existente);
    }

    public void deletar(Long id) {
        Adicional existente = adicionalRepository.findById(id).orElseThrow(() -> new RuntimeException("Adicional não encontrado"));
        if (!existente.getRestaurante().getId().equals(getRestauranteLogado().getId())) {
            throw new SecurityException("Acesso negado.");
        }
        adicionalRepository.deleteById(id);
    }
}