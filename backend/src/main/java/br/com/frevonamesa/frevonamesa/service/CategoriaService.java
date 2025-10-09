package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Categoria;
import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.CategoriaRepository;
import br.com.frevonamesa.frevonamesa.repository.ProdutoRepository;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private RestauranteService restauranteService;

    public List<Categoria> listarTodas() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        return categoriaRepository.findByRestauranteId(restaurante.getId());
    }

    @Transactional
    public Categoria criar(Categoria categoria) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        categoria.setRestaurante(restaurante);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria atualizar(Long id, Categoria categoriaAtualizada) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Categoria categoriaExistente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

        if (!categoriaExistente.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        categoriaExistente.setNome(categoriaAtualizada.getNome());
        return categoriaRepository.save(categoriaExistente);
    }

    @Transactional
    public void deletar(Long id) {
        Restaurante restaurante = restauranteService.getRestauranteLogado();
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

        if (!categoria.getRestaurante().getId().equals(restaurante.getId())) {
            throw new SecurityException("Acesso negado.");
        }

        // Verifica se a categoria está em uso antes de deletar
        if (produtoRepository.existsByCategoriaId(id)) {
            throw new RuntimeException("Não é possível excluir a categoria, pois ela está associada a produtos.");
        }

        categoriaRepository.delete(categoria);
    }
}