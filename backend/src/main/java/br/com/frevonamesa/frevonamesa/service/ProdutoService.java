package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.ProdutoDTO;
import br.com.frevonamesa.frevonamesa.model.Categoria;
import br.com.frevonamesa.frevonamesa.model.Produto;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.CategoriaRepository;
import br.com.frevonamesa.frevonamesa.repository.ProdutoRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private RestauranteService restauranteService;

    public List<Produto> listarTodos() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        return produtoRepository.findByRestauranteId(restaurante.getId());
    }

    public void deletarProduto(Long id) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto com ID " + id + " não encontrado."));

        // Validação de segurança: Garante que o produto pertence ao restaurante logado
        if (!produto.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Este produto não pertence ao seu restaurante.");
        }
        produtoRepository.deleteById(id);
    }

    @Transactional
    public Produto criarProduto(ProdutoDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada!"));

        if (!categoria.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Categoria inválida.");
        }

        Produto novoProduto = new Produto();
        novoProduto.setNome(dto.getNome());
        novoProduto.setDescricao(dto.getDescricao());
        novoProduto.setPreco(dto.getPreco());
        novoProduto.setImageUrl(dto.getImageUrl());
        novoProduto.setCategoria(categoria); // ATUALIZADO
        novoProduto.setRestaurante(restaurante);
        return produtoRepository.save(novoProduto);
    }

    public Produto atualizarProduto(Long id, ProdutoDTO dto) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Produto produtoExistente = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto com ID " + id + " não encontrado."));

        if (!produtoExistente.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada!"));

        if (!categoria.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado: Categoria inválida.");
        }

        produtoExistente.setNome(dto.getNome());
        produtoExistente.setDescricao(dto.getDescricao());
        produtoExistente.setPreco(dto.getPreco());
        produtoExistente.setImageUrl(dto.getImageUrl());
        produtoExistente.setCategoria(categoria);
        return produtoRepository.save(produtoExistente);
    }
}