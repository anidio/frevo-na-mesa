package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.dto.ProdutoDTO;
import br.com.frevonamesa.frevonamesa.model.Produto;
import br.com.frevonamesa.frevonamesa.repository.ProdutoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public void deletarProduto(Long id) {
        // Verifica se o produto existe antes de tentar deletar
        if (!produtoRepository.existsById(id)) {
            throw new RuntimeException("Produto com ID " + id + " não encontrado.");
        }
        produtoRepository.deleteById(id);
    }

    @Transactional
    public Produto criarProduto(ProdutoDTO dto) {
        Produto novoProduto = new Produto();
        novoProduto.setNome(dto.getNome());
        novoProduto.setDescricao(dto.getDescricao());
        novoProduto.setPreco(dto.getPreco());
        novoProduto.setCategoria(dto.getCategoria());
        return produtoRepository.save(novoProduto);
    }

    public Produto atualizarProduto(Long id, ProdutoDTO dto) {
        // Busca o produto existente no banco de dados
        Produto produtoExistente = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto com ID " + id + " não encontrado."));

        // Atualiza os campos do produto com os dados do DTO
        produtoExistente.setNome(dto.getNome());
        produtoExistente.setDescricao(dto.getDescricao());
        produtoExistente.setPreco(dto.getPreco());
        produtoExistente.setCategoria(dto.getCategoria());

        // O .save() do JPA funciona tanto para criar (se o ID é nulo) quanto para atualizar (se o ID existe)
        return produtoRepository.save(produtoExistente);
    }
}
