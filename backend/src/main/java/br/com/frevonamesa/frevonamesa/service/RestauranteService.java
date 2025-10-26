package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.*;
import br.com.frevonamesa.frevonamesa.model.*;
import br.com.frevonamesa.frevonamesa.repository.*;
import jakarta.persistence.EntityManager; // Importar EntityManager
import jakarta.persistence.PersistenceContext; // Importar PersistenceContext
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
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

    // Adicionar um logger para a classe
    private static final Logger logger = LoggerFactory.getLogger(RestauranteService.class);

    // --- INJEÇÃO DO EntityManager (para o flush) ---
    @PersistenceContext // Ou @Autowired, dependendo da configuração
    private EntityManager entityManager;
    // --- FIM DA INJEÇÃO ---

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
        logger.debug("Tentando obter restaurante logado para o email: {}", email); // Log

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Restaurante restaurante = usuarioOpt.get().getRestaurante();
            logger.debug("Restaurante encontrado através do Usuario: ID {}", restaurante.getId()); // Log
            return restaurante;
        }

        logger.debug("Usuário não encontrado, tentando buscar diretamente na tabela Restaurante."); // Log
        return restauranteRepository.findByEmail(email)
                .map(rest -> {
                    logger.debug("Restaurante encontrado diretamente: ID {}", rest.getId()); // Log
                    return rest;
                })
                .orElseThrow(() -> {
                    logger.error("Nenhum Restaurante ou Usuário encontrado para o email: {}", email); // Log de erro
                    return new UsernameNotFoundException("Restaurante ou Usuário não encontrado: " + email);
                });
    }

    public Restaurante cadastrar(RestauranteDTO restauranteDTO) {
        logger.info("Iniciando cadastro para o email: {}", restauranteDTO.getEmail()); // Log
        if (restauranteRepository.findByEmail(restauranteDTO.getEmail()).isPresent() ||
                usuarioRepository.findByEmail(restauranteDTO.getEmail()).isPresent()) {
            logger.warn("Tentativa de cadastro com email já existente: {}", restauranteDTO.getEmail()); // Log
            throw new RuntimeException("Email já cadastrado.");
        }

        String senhaCriptografada = passwordEncoder.encode(restauranteDTO.getSenha());

        Restaurante novoRestaurante = new Restaurante(
                restauranteDTO.getNome(),
                restauranteDTO.getEmail(),
                senhaCriptografada
        );

        novoRestaurante.setTipo(TipoEstabelecimento.MESAS_E_DELIVERY);
        novoRestaurante.setEndereco(restauranteDTO.getEndereco());
        novoRestaurante.setPlano("GRATUITO");
        novoRestaurante.setDeliveryPro(false);
        novoRestaurante.setSalaoPro(false);

        logger.debug("Salvando novo restaurante..."); // Log
        Restaurante restauranteSalvo = restauranteRepository.save(novoRestaurante);
        logger.info("Restaurante salvo com ID: {}", restauranteSalvo.getId()); // Log

        logger.debug("Criando usuário ADMIN associado..."); // Log
        Usuario adminUser = new Usuario();
        adminUser.setNome(restauranteDTO.getNome());
        adminUser.setEmail(restauranteDTO.getEmail());
        adminUser.setSenha(senhaCriptografada);
        adminUser.setRestaurante(restauranteSalvo);
        adminUser.setRole(Role.ADMIN);
        usuarioRepository.save(adminUser);
        logger.info("Usuário ADMIN criado para o restaurante ID: {}", restauranteSalvo.getId()); // Log

        if (restauranteSalvo.getTipo() == TipoEstabelecimento.APENAS_MESAS || restauranteSalvo.getTipo() == TipoEstabelecimento.MESAS_E_DELIVERY) {
            logger.debug("Criando mesas iniciais para o restaurante ID: {}", restauranteSalvo.getId()); // Log
            IntStream.rangeClosed(1, 5).forEach(numero -> {
                Mesa novaMesa = new Mesa();
                novaMesa.setNumero(numero);
                novaMesa.setStatus(StatusMesa.LIVRE);
                novaMesa.setValorTotal(BigDecimal.ZERO);
                novaMesa.setPedidos(new ArrayList<>());
                novaMesa.setRestaurante(restauranteSalvo);
                mesaRepository.save(novaMesa);
            });
            logger.debug("Mesas iniciais criadas."); // Log
        }

        return restauranteSalvo;
    }

    public RestaurantePerfilDTO getPerfilLogado() {
        logger.debug("Iniciando getPerfilLogado..."); // Log
        Restaurante restaurante = getRestauranteLogado();
        logger.debug("Restaurante obtido para perfil: ID {}", restaurante.getId()); // Log

        RestaurantePerfilDTO perfilDto = new RestaurantePerfilDTO();
        // ... (outras atribuições idênticas a antes) ...
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
        perfilDto.setBetaTester(restaurante.isBetaTester());
        perfilDto.setDeliveryPro(restaurante.isDeliveryPro());
        perfilDto.setSalaoPro(restaurante.isSalaoPro());
        perfilDto.setTaxaEntrega(restaurante.getTaxaEntrega());
        perfilDto.setLogoUrl(restaurante.getLogoUrl());
        perfilDto.setCepRestaurante(restaurante.getCepRestaurante());

        // --- AJUSTE AO LER ---
        // Lombok/JPA geralmente criam `isCampo()` para boolean `campo`
        boolean valorLidoDoBanco = restaurante.isCalculoHaversineAtivo();
        logger.info("Valor de calculoHaversineAtivo LIDO do Restaurante (ID {}): {}", restaurante.getId(), valorLidoDoBanco);
        perfilDto.setCalculoHaversineAtivo(valorLidoDoBanco); // Usa o setter correspondente no DTO
        // --- FIM DO AJUSTE ---

        perfilDto.setStripeSubscriptionId(restaurante.getStripeSubscriptionId());
        perfilDto.setStripeCustomerId(restaurante.getStripeCustomerId());

        logger.debug("Perfil DTO montado: {}", perfilDto); // Log
        return perfilDto;
    }

    @Transactional
    public RestaurantePerfilDTO atualizarConfiguracoes(RestauranteSettingsDTO settingsDTO) {
        logger.info("Iniciando atualização de configurações..."); // Log
        Restaurante restaurante = getRestauranteLogado();
        logger.debug("Restaurante (ID {}) encontrado para atualização.", restaurante.getId()); // Log

        // --- LOGS E AJUSTE AO SALVAR ---
        // Lombok geralmente cria `isCampo()` para boolean `campo` no DTO também
        boolean valorRecebido = settingsDTO.isCalculoHaversineAtivo(); // Leitura do DTO
        logger.info("Valor recebido no DTO para calculoHaversineAtivo: {}", valorRecebido);
        logger.debug("Valor ATUAL de calculoHaversineAtivo no Restaurante ANTES do set: {}", restaurante.isCalculoHaversineAtivo());
        restaurante.setCalculoHaversineAtivo(valorRecebido); // Usa o setter correto na entidade
        logger.info("Valor de calculoHaversineAtivo no Restaurante APÓS o set: {}", restaurante.isCalculoHaversineAtivo());
        // --- FIM DOS AJUSTES ---

        // Atualiza os outros campos
        restaurante.setImpressaoMesaAtivada(settingsDTO.isImpressaoMesaAtivada());
        restaurante.setImpressaoDeliveryAtivada(settingsDTO.isImpressaoDeliveryAtivada());
        restaurante.setWhatsappNumber(settingsDTO.getWhatsappNumber());
        restaurante.setTaxaEntrega(settingsDTO.getTaxaEntrega());


        logger.debug("Tentando salvar as alterações no restaurante..."); // Log
        Restaurante restauranteSalvo = restauranteRepository.save(restaurante);
        logger.info("Restaurante (ID {}) salvo.", restauranteSalvo.getId()); // Log

        // --- Adicionar Flush Explícito (mantido da sugestão anterior) ---
        try {
            entityManager.flush(); // Força a sincronização com o banco
            logger.info("EntityManager flushed successfully após salvar configurações.");
        } catch (Exception e) {
            logger.error("Erro ao fazer flush do EntityManager após salvar configurações: {}", e.getMessage(), e);
        }
        // --- FIM DO Flush ---

        // --- LOG APÓS SALVAR E FLUSH ---
        Restaurante restauranteAposSave = restauranteRepository.findById(restauranteSalvo.getId()).orElse(null);
        if (restauranteAposSave != null) {
            logger.info("Valor de calculoHaversineAtivo lido do banco IMEDIATAMENTE APÓS save/flush: {}", restauranteAposSave.isCalculoHaversineAtivo());
        } else {
            logger.error("Erro ao re-buscar restaurante após salvar!");
        }
        // --- FIM DO LOG APÓS SALVAR E FLUSH ---

        logger.debug("Retornando o perfil atualizado..."); // Log
        return getPerfilLogado(); // Retorna o perfil atualizado
    }

    @Transactional
    public RestaurantePerfilDTO atualizarPerfil(RestauranteUpdateDTO dto) {
        // ... (código existente com logs) ...
        logger.info("Iniciando atualização de perfil..."); // Log
        Restaurante restaurante = getRestauranteLogado();
        logger.debug("Restaurante (ID {}) encontrado para atualização de perfil.", restaurante.getId()); // Log

        restaurante.setNome(dto.getNome());
        restaurante.setEndereco(dto.getEndereco());
        restaurante.setLogoUrl(dto.getLogoUrl());
        restaurante.setCepRestaurante(dto.getCepRestaurante());

        logger.debug("Tentando salvar as alterações de perfil..."); // Log
        Restaurante restauranteSalvo = restauranteRepository.save(restaurante);
        try { // Adiciona flush aqui também por segurança
            entityManager.flush();
            logger.info("EntityManager flushed successfully após salvar perfil.");
        } catch (Exception e) {
            logger.error("Erro ao fazer flush do EntityManager após salvar perfil: {}", e.getMessage(), e);
        }
        logger.info("Perfil do Restaurante (ID {}) salvo com sucesso.", restauranteSalvo.getId()); // Log

        logger.debug("Retornando o perfil atualizado..."); // Log
        return getPerfilLogado(); // Retorna o perfil atualizado
    }

    // Método getStatusPlanoDetalhado permanece igual
    public Map<String, Object> getStatusPlanoDetalhado() {
        // ... (código existente sem alterações) ...
        Restaurante restaurante = getRestauranteLogado();

        int limitePedidosGratuito = 5; // Limite padrão do plano gratuito
        boolean aplicaLimitePedidos = restaurante.getPlano().equals("GRATUITO")
                && !restaurante.isLegacyFree()
                && !restaurante.isBetaTester()
                && !restaurante.isDeliveryPro();

        int pedidosAtuais = restaurante.getPedidosMesAtual();
        int pedidosCompensados = aplicaLimitePedidos ? Math.max(0, limitePedidosGratuito - pedidosAtuais) : 0;
        boolean limiteAtingido = aplicaLimitePedidos && (pedidosAtuais >= limitePedidosGratuito);

        Map<String, Object> status = new HashMap<>();
        status.put("planoAtual", restaurante.getPlano());
        status.put("isLegacyFree", restaurante.isLegacyFree());
        status.put("isBetaTester", restaurante.isBetaTester());
        status.put("isDeliveryPro", restaurante.isDeliveryPro());
        status.put("isSalaoPro", restaurante.isSalaoPro());
        status.put("limiteMesas", restaurante.getLimiteMesas());
        status.put("limiteUsuarios", restaurante.getLimiteUsuarios());
        status.put("pedidosMesAtual", pedidosAtuais);
        status.put("limitePedidosGratuito", aplicaLimitePedidos ? limitePedidosGratuito : null);
        status.put("pedidosRestantesCompensados", pedidosCompensados);
        status.put("limiteAtingido", limiteAtingido);

        return status;
    }

    // Método getCardapioPublico permanece igual
    public CardapioPublicoDTO getCardapioPublico(Long restauranteId) {
        // ... (código existente sem alterações, mas lembre-se de adicionar
        //      `calculoHaversineAtivo` ao CardapioPublicoDTO se precisar dele lá) ...
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado."));

        List<Categoria> categorias = categoriaRepository.findByRestauranteId(restauranteId);
        List<Produto> produtos = produtoRepository.findByRestauranteId(restauranteId);

        CardapioPublicoDTO cardapioDTO = new CardapioPublicoDTO();
        cardapioDTO.setNomeRestaurante(restaurante.getNome());
        cardapioDTO.setEnderecoRestaurante(restaurante.getEndereco());
        cardapioDTO.setTaxaEntrega(restaurante.getTaxaEntrega());
        cardapioDTO.setLogoUrl(restaurante.getLogoUrl());

        // Se o DTO tiver o campo:
        // cardapioDTO.setCalculoHaversineAtivo(restaurante.isCalculoHaversineAtivo());

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