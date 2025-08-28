// src/main/java/br/com/frevonamesa/frevonamesa/config/DataInitializer.java

package br.com.frevonamesa.frevonamesa.config;

import br.com.frevonamesa.frevonamesa.model.Mesa;
import br.com.frevonamesa.frevonamesa.model.Produto;
import br.com.frevonamesa.frevonamesa.model.StatusMesa;
import br.com.frevonamesa.frevonamesa.repository.MesaRepository;
import br.com.frevonamesa.frevonamesa.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Override
    public void run(String... args) throws Exception {

//        // Ele só executa esta lógica se a tabela de produtos estiver completamente vazia.
//        if (produtoRepository.count() == 0) {
//            produtoRepository.saveAll(Arrays.asList(
//                    new Produto("Bruschetta", "Torrada italiana com tomate e manjericão", new BigDecimal("18.90"), "Entradas"),
//                    new Produto("Coxinha de Frango", "Coxinha tradicional mineira", new BigDecimal("8.50"), "Entradas"),
//                    new Produto("Pastéis Variados", "Porção de pastéis de carne, queijo e palmito", new BigDecimal("24.90"), "Entradas"),
//                    new Produto("Filé à Parmegiana", "Filé empanado com molho e queijo", new BigDecimal("45.90"), "Pratos"),
//                    new Produto("Feijoada Completa", "Feijoada tradicional com acompanhamentos", new BigDecimal("38.90"), "Pratos"),
//                    new Produto("Salmão Grelhado", "Salmão com legumes na chapa", new BigDecimal("52.90"), "Pratos"),
//                    new Produto("Pudim de Leite", "Pudim de leite condensado com calda de caramelo", new BigDecimal("12.00"), "Sobremesas"),
//                    new Produto("Suco Natural", "Laranja, abacaxi ou morango", new BigDecimal("7.00"), "Bebidas"),
//                    new Produto("Refrigerante Lata", "Coca-cola, Guaraná ou Soda", new BigDecimal("5.50"), "Bebidas")
//            ));
//        }
//
//        if (mesaRepository.count() == 0) {
//            IntStream.rangeClosed(1, 10).forEach(numero -> {
//                Mesa novaMesa = new Mesa();
//                novaMesa.setNumero(numero);
//                novaMesa.setStatus(StatusMesa.LIVRE);
//                novaMesa.setValorTotal(BigDecimal.ZERO);
//                novaMesa.setPedidos(new ArrayList<>());
//                mesaRepository.save(novaMesa);
//            });
//        }
    }
}