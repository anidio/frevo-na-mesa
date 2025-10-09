package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // 4. Importar BigDecimal
import java.util.ArrayList; // 5. Importar ArrayList
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // 6. Importar IntStream

@Service
@Transactional
public class RestauranteService {

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Restaurante getRestauranteLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Tenta buscar o Usuario
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            return usuarioOpt.get().getRestaurante(); // Retorna o restaurante do Usuario
        }

        // 2. Fallback (Para o código legado, deve ser removido no futuro)
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante não encontrado: " + email));
    }

    public Restaurante cadastrar(RestauranteDTO restauranteDTO) {
        if (restauranteRepository.findByEmail(restauranteDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }
        if (usuarioRepository.findByEmail(restauranteDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }

        String senhaCriptografada = passwordEncoder.encode(restauranteDTO.getSenha());

        Restaurante novoRestaurante = new Restaurante(
                restauranteDTO.getNome(),
                restauranteDTO.getEmail(),
                senhaCriptografada,
                restauranteDTO.getTipo()
        );
        novoRestaurante.setEndereco(restauranteDTO.getEndereco());
        Restaurante restauranteSalvo = restauranteRepository.save(novoRestaurante);

        Usuario adminUser = new Usuario();
        adminUser.setNome(restauranteDTO.getNome());
        adminUser.setEmail(restauranteDTO.getEmail()); // O email do restaurante é o email do primeiro ADMIN
        adminUser.setSenha(restauranteSalvo.getSenha()); // A senha já está criptografada
        adminUser.setRestaurante(restauranteSalvo);
        adminUser.setRole(Role.ADMIN);

        usuarioRepository.save(adminUser);

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
        Restaurante restaurante = getRestauranteLogado();

        RestaurantePerfilDTO perfilDto = new RestaurantePerfilDTO();
        perfilDto.setId(restaurante.getId());
        perfilDto.setNome(restaurante.getNome());
        perfilDto.setEmail(restaurante.getEmail());
        perfilDto.setEndereco(restaurante.getEndereco());
        perfilDto.setTipo(restaurante.getTipo());
        perfilDto.setImpressaoDeliveryAtivada(restaurante.isImpressaoDeliveryAtivada());
        perfilDto.setImpressaoMesaAtivada(restaurante.isImpressaoMesaAtivada());
        perfilDto.setWhatsappNumber(restaurante.getWhatsappNumber());
        perfilDto.setPlano(restaurante.getPlano());
        perfilDto.setLimiteUsuarios(restaurante.getLimiteUsuarios());
        perfilDto.setLimiteMesas(restaurante.getLimiteMesas());
        perfilDto.setLegacyFree(restaurante.isLegacyFree());

        return perfilDto;
    }


    @Transactional
    public RestaurantePerfilDTO atualizarConfiguracoes(RestauranteSettingsDTO settingsDTO) {
        Restaurante restaurante = getRestauranteLogado();
        restaurante.setImpressaoMesaAtivada(settingsDTO.isImpressaoMesaAtivada());
        restaurante.setImpressaoDeliveryAtivada(settingsDTO.isImpressaoDeliveryAtivada());
        restaurante.setWhatsappNumber(settingsDTO.getWhatsappNumber()); // Corrigido
        restauranteRepository.save(restaurante);
        return getPerfilLogado();
    }

    public CardapioPublicoDTO getCardapioPublico(Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado."));

        List<Categoria> categorias = categoriaRepository.findByRestauranteId(restauranteId);
        List<Produto> produtos = produtoRepository.findByRestauranteId(restauranteId);

        CardapioPublicoDTO cardapioDTO = new CardapioPublicoDTO();
        cardapioDTO.setNomeRestaurante(restaurante.getNome());
        cardapioDTO.setEnderecoRestaurante(restaurante.getEndereco());

        List<CategoriaCardapioDTO> categoriasDTO = categorias.stream().map(categoria -> {
            CategoriaCardapioDTO categoriaDTO = new CategoriaCardapioDTO();
            categoriaDTO.setNome(categoria.getNome());

            List<ProdutoCardapioDTO> produtosDaCategoria = produtos.stream()
                    .filter(produto -> produto.getCategoria().getId().equals(categoria.getId()))
                    .map(produto -> {
                        ProdutoCardapioDTO produtoDTO = new ProdutoCardapioDTO();
                        produtoDTO.setId(produto.getId());
                        produtoDTO.setNome(produto.getNome());
                        produtoDTO.setDescricao(produto.getDescricao());
                        produtoDTO.setPreco(produto.getPreco());
                        return produtoDTO;
                    }).collect(Collectors.toList());

            categoriaDTO.setProdutos(produtosDaCategoria);
            return categoriaDTO;
        }).collect(Collectors.toList());

        cardapioDTO.setCategorias(categoriasDTO);
        return cardapioDTO;
    }
}