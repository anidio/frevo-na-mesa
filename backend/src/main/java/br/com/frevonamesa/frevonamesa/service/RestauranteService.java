package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.RestauranteDTO;
import br.com.frevonamesa.frevonamesa.dto.RestaurantePerfilDTO;
import br.com.frevonamesa.frevonamesa.dto.RestauranteSettingsDTO;
import br.com.frevonamesa.frevonamesa.model.Mesa; // 1. Importar Mesa
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.model.StatusMesa; // 2. Importar StatusMesa
import br.com.frevonamesa.frevonamesa.model.TipoEstabelecimento;
import br.com.frevonamesa.frevonamesa.repository.CategoriaRepository;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository; // 3. Importar MesaRepository
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private MesaRepository mesaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado com o email: " + email));
    }

    public Restaurante cadastrar(RestauranteDTO restauranteDTO) {
        if (restauranteRepository.findByEmail(restauranteDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }

        String senhaCriptografada = passwordEncoder.encode(restauranteDTO.getSenha());

        Restaurante novoRestaurante = new Restaurante(
                restauranteDTO.getNome(),
                restauranteDTO.getEmail(),
                senhaCriptografada,
                restauranteDTO.getTipo()
        );

        // Salva o novo restaurante primeiro para que ele tenha um ID
        Restaurante restauranteSalvo = restauranteRepository.save(novoRestaurante);

        if (restauranteSalvo.getTipo() == TipoEstabelecimento.APENAS_MESAS || restauranteSalvo.getTipo() == TipoEstabelecimento.MESAS_E_DELIVERY) {
            IntStream.rangeClosed(1, 5).forEach(numero -> {
                Mesa novaMesa = new Mesa();
                novaMesa.setNumero(numero);
                novaMesa.setStatus(StatusMesa.LIVRE);
                novaMesa.setValorTotal(BigDecimal.ZERO);
                novaMesa.setPedidos(new ArrayList<>());
                novaMesa.setRestaurante(restauranteSalvo);
                mesaRepository.save(novaMesa);
            });
        }

        return restauranteSalvo;
    }

    public RestaurantePerfilDTO getPerfilLogado() {
        // Pega o email do usuário autenticado no sistema
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Restaurante restaurante = restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado: " + email));

        // Converte a entidade para o DTO de perfil
        RestaurantePerfilDTO perfilDto = new RestaurantePerfilDTO();
        perfilDto.setId(restaurante.getId());
        perfilDto.setNome(restaurante.getNome());
        perfilDto.setEmail(restaurante.getEmail());
        perfilDto.setTipo(restaurante.getTipo());
        perfilDto.setImpressaoDeliveryAtivada(restaurante.isImpressaoDeliveryAtivada());
        perfilDto.setImpressaoMesaAtivada(restaurante.isImpressaoMesaAtivada());

        return perfilDto;
    }

    @Transactional
    public RestaurantePerfilDTO atualizarConfiguracoes(RestauranteSettingsDTO settingsDTO) {
        Restaurante restaurante = getRestauranteLogado();
        restaurante.setImpressaoMesaAtivada(settingsDTO.isImpressaoMesaAtivada());
        restaurante.setImpressaoDeliveryAtivada(settingsDTO.isImpressaoDeliveryAtivada());
        restauranteRepository.save(restaurante);
        return getPerfilLogado(); // Retorna o perfil atualizado
    }
}