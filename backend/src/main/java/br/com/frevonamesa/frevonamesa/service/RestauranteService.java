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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            // Garante que a entidade Restaurante associada seja carregada se for LAZY
            Restaurante restaurante = usuarioOpt.get().getRestaurante();
            return restaurante;
        }

        //    quando todos os donos de restaurante também forem 'Usuario' com Role.ADMIN)
        return restauranteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Restaurante ou Usuário não encontrado: " + email));
    }

    public Restaurante cadastrar(RestauranteDTO restauranteDTO) {
        if (restauranteRepository.findByEmail(restauranteDTO.getEmail()).isPresent() ||
                usuarioRepository.findByEmail(restauranteDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }

        String senhaCriptografada = passwordEncoder.encode(restauranteDTO.getSenha());

        Restaurante novoRestaurante = new Restaurante(
                restauranteDTO.getNome(),
                restauranteDTO.getEmail(),
                senhaCriptografada
        );

        novoRestaurante.setTipo(TipoEstabelecimento.MESAS_E_DELIVERY); // Define o tipo padrão
        novoRestaurante.setEndereco(restauranteDTO.getEndereco());
        novoRestaurante.setPlano("GRATUITO"); // Garante que comece como GRATUITO
        novoRestaurante.setDeliveryPro(false); // Garante que comece sem acesso PRO
        novoRestaurante.setSalaoPro(false); // Garante que comece sem acesso PRO
        // Os limites padrão (10 mesas, 4 usuários) e outros valores (pedidosMesAtual=0, taxaEntrega=0)
        // devem ser definidos no construtor padrão ou no construtor ajustado de Restaurante.java

        // Salva o restaurante primeiro para obter o ID
        Restaurante restauranteSalvo = restauranteRepository.save(novoRestaurante);

        // Cria o usuário ADMIN associado
        Usuario adminUser = new Usuario();
        adminUser.setNome(restauranteDTO.getNome());
        adminUser.setEmail(restauranteDTO.getEmail());
        adminUser.setSenha(senhaCriptografada); // Reutiliza a senha já criptografada
        adminUser.setRestaurante(restauranteSalvo); // Associa ao restaurante salvo
        adminUser.setRole(Role.ADMIN);

        usuarioRepository.save(adminUser);

        if (restauranteSalvo.getTipo() == TipoEstabelecimento.APENAS_MESAS || restauranteSalvo.getTipo() == TipoEstabelecimento.MESAS_E_DELIVERY) {
            IntStream.rangeClosed(1, 5).forEach(numero -> { // Cria 5 mesas iniciais
                Mesa novaMesa = new Mesa();
                novaMesa.setNumero(numero);
                novaMesa.setStatus(StatusMesa.LIVRE);
                novaMesa.setValorTotal(BigDecimal.ZERO);
                novaMesa.setPedidos(new ArrayList<>());
                novaMesa.setRestaurante(restauranteSalvo); // Associa a mesa ao restaurante
                mesaRepository.save(novaMesa);
            });
        }

        return restauranteSalvo;
    }

    // Método getPerfilLogado permanece igual, pois já retorna as flags
    public RestaurantePerfilDTO getPerfilLogado() {
        Restaurante restaurante = getRestauranteLogado();

        RestaurantePerfilDTO perfilDto = new RestaurantePerfilDTO();
        perfilDto.setId(restaurante.getId());
        perfilDto.setNome(restaurante.getNome());
        perfilDto.setEmail(restaurante.getEmail());
        perfilDto.setEndereco(restaurante.getEndereco());
        perfilDto.setTipo(restaurante.getTipo()); // Mantém o tipo para fins informativos, se necessário
        perfilDto.setImpressaoDeliveryAtivada(restaurante.isImpressaoDeliveryAtivada());
        perfilDto.setImpressaoMesaAtivada(restaurante.isImpressaoMesaAtivada());
        perfilDto.setWhatsappNumber(restaurante.getWhatsappNumber());
        perfilDto.setPlano(restaurante.getPlano());
        perfilDto.setLimiteUsuarios(restaurante.getLimiteUsuarios());
        perfilDto.setLimiteMesas(restaurante.getLimiteMesas());
        perfilDto.setLegacyFree(restaurante.isLegacyFree());
        perfilDto.setBetaTester(restaurante.isBetaTester());
        perfilDto.setDeliveryPro(restaurante.isDeliveryPro()); // Flag essencial
        perfilDto.setSalaoPro(restaurante.isSalaoPro());       // Flag essencial
        perfilDto.setTaxaEntrega(restaurante.getTaxaEntrega());

        perfilDto.setLogoUrl(restaurante.getLogoUrl());
        perfilDto.setCepRestaurante(restaurante.getCepRestaurante());

        return perfilDto;
    }

    // Método atualizarConfiguracoes permanece igual
    @Transactional
    public RestaurantePerfilDTO atualizarConfiguracoes(RestauranteSettingsDTO settingsDTO) {
        Restaurante restaurante = getRestauranteLogado();
        restaurante.setImpressaoMesaAtivada(settingsDTO.isImpressaoMesaAtivada());
        restaurante.setImpressaoDeliveryAtivada(settingsDTO.isImpressaoDeliveryAtivada());
        restaurante.setWhatsappNumber(settingsDTO.getWhatsappNumber());
        restaurante.setTaxaEntrega(settingsDTO.getTaxaEntrega());
        restauranteRepository.save(restaurante);
        return getPerfilLogado(); // Retorna o perfil atualizado
    }

    // Método atualizarPerfil permanece igual
    @Transactional
    public RestaurantePerfilDTO atualizarPerfil(RestauranteUpdateDTO dto) {
        Restaurante restaurante = getRestauranteLogado();

        restaurante.setNome(dto.getNome());
        restaurante.setEndereco(dto.getEndereco());
        restaurante.setLogoUrl(dto.getLogoUrl());
        restaurante.setCepRestaurante(dto.getCepRestaurante());

        restauranteRepository.save(restaurante);
        return getPerfilLogado(); // Retorna o perfil atualizado
    }

    // Método getStatusPlanoDetalhado permanece igual
    // A lógica de limiteAtingido já considera o plano GRATUITO e isLegacyFree
    public Map<String, Object> getStatusPlanoDetalhado() {
        Restaurante restaurante = getRestauranteLogado();

        int limitePedidosGratuito = 5; // Limite padrão do plano gratuito
        // Ajuste: O limite só se aplica se for gratuito E NÃO for legacy E NÃO for beta tester E NÃO tiver Delivery Pro
        boolean aplicaLimitePedidos = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester()
                && !restaurante.isDeliveryPro();

        int pedidosAtuais = restaurante.getPedidosMesAtual();
        int pedidosCompensados = aplicaLimitePedidos ? Math.max(0, limitePedidosGratuito - pedidosAtuais) : 0; // Ou infinito se não aplica? Melhor 0.
        boolean limiteAtingido = aplicaLimitePedidos && (pedidosAtuais >= limitePedidosGratuito);

        Map<String, Object> status = new HashMap<>();
        status.put("planoAtual", restaurante.getPlano());
        status.put("isLegacyFree", restaurante.isLegacyFree());
        status.put("isBetaTester", restaurante.isBetaTester()); // Incluir beta tester
        status.put("isDeliveryPro", restaurante.isDeliveryPro()); // Incluir flags
        status.put("isSalaoPro", restaurante.isSalaoPro());       // Incluir flags
        status.put("limiteMesas", restaurante.getLimiteMesas());
        status.put("limiteUsuarios", restaurante.getLimiteUsuarios());
        status.put("pedidosMesAtual", pedidosAtuais);
        status.put("limitePedidosGratuito", aplicaLimitePedidos ? limitePedidosGratuito : null); // Mostrar limite só se aplicável
        status.put("pedidosRestantesCompensados", pedidosCompensados);
        status.put("limiteAtingido", limiteAtingido); // Flag que indica se o limite de pedidos GRATUITO foi atingido
        // status.put("statusPagamentos", "..."); // Pode adicionar info do Stripe aqui se quiser

        return status;
    }

    // Método getCardapioPublico permanece igual
    public CardapioPublicoDTO getCardapioPublico(Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado."));

        List<Categoria> categorias = categoriaRepository.findByRestauranteId(restauranteId);
        List<Produto> produtos = produtoRepository.findByRestauranteId(restauranteId);

        CardapioPublicoDTO cardapioDTO = new CardapioPublicoDTO();
        cardapioDTO.setNomeRestaurante(restaurante.getNome());
        cardapioDTO.setEnderecoRestaurante(restaurante.getEndereco());
        cardapioDTO.setTaxaEntrega(restaurante.getTaxaEntrega());
        cardapioDTO.setLogoUrl(restaurante.getLogoUrl());

        List<CategoriaCardapioDTO> categoriasDTO = categorias.stream().map(categoria -> {
            CategoriaCardapioDTO categoriaDTO = new CategoriaCardapioDTO();
            categoriaDTO.setNome(categoria.getNome());

            List<ProdutoCardapioDTO> produtosDaCategoria = produtos.stream()
                    .filter(produto -> produto.getCategoria() != null && produto.getCategoria().getId().equals(categoria.getId()))
                    .map(produto -> {
                        ProdutoCardapioDTO produtoDTO = new ProdutoCardapioDTO();
                        produtoDTO.setId(produto.getId());
                        produtoDTO.setNome(produto.getNome());
                        produtoDTO.setDescricao(produto.getDescricao());
                        produtoDTO.setPreco(produto.getPreco());
                        produtoDTO.setImageUrl(produto.getImageUrl());
                        return produtoDTO;
                    }).collect(Collectors.toList());

            categoriaDTO.setProdutos(produtosDaCategoria);
            return categoriaDTO;
        }).collect(Collectors.toList());

        cardapioDTO.setCategorias(categoriasDTO);
        return cardapioDTO;
    }
}