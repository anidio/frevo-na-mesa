package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.RestauranteDTO;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RestauranteService {

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // O método loadUserByUsername foi removido daqui e colocado na ApplicationConfig

    public Restaurante cadastrar(RestauranteDTO restauranteDTO) {
        if (restauranteRepository.findByEmail(restauranteDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }

        String senhaCriptografada = passwordEncoder.encode(restauranteDTO.getSenha());

        Restaurante novoRestaurante = new Restaurante(
                restauranteDTO.getNome(),
                restauranteDTO.getEmail(),
                senhaCriptografada
        );

        return restauranteRepository.save(novoRestaurante);
    }
}