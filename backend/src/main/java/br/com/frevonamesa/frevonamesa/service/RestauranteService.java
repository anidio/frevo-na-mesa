package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.RestauranteDTO;
import br.com.frevonamesa.frevonamesa.model.Mesa; // 1. Importar Mesa
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.StatusMesa; // 2. Importar StatusMesa
import br.com.frevonamesa.frevonamesa.repository.MesaRepository; // 3. Importar MesaRepository
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal; // 4. Importar BigDecimal
import java.util.ArrayList; // 5. Importar ArrayList
import java.util.stream.IntStream; // 6. Importar IntStream

@Service
public class RestauranteService {

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MesaRepository mesaRepository; // 7. Injetar o repositório de mesas

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

        // Salva o novo restaurante primeiro para que ele tenha um ID
        Restaurante restauranteSalvo = restauranteRepository.save(novoRestaurante);

        // 8. LÓGICA ADICIONADA: Cria 10 mesas padrão para o novo restaurante
        IntStream.rangeClosed(1, 10).forEach(numero -> {
            Mesa novaMesa = new Mesa();
            novaMesa.setNumero(numero);
            novaMesa.setStatus(StatusMesa.LIVRE);
            novaMesa.setValorTotal(BigDecimal.ZERO);
            novaMesa.setPedidos(new ArrayList<>());
            novaMesa.setRestaurante(restauranteSalvo); // Associa ao restaurante que acabamos de salvar
            mesaRepository.save(novaMesa);
        });

        return restauranteSalvo;
    }
}