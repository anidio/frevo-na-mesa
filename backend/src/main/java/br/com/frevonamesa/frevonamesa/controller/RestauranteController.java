package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.RestaurantePerfilDTO;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurante")
public class RestauranteController {

    @Autowired
    private RestauranteService restauranteService;

    @GetMapping("/meu-perfil")
    public ResponseEntity<RestaurantePerfilDTO> getMeuPerfil() {
        RestaurantePerfilDTO perfil = restauranteService.getPerfilLogado();
        return ResponseEntity.ok(perfil);
    }
}