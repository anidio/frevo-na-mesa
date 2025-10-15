package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.RestaurantePerfilDTO;
import br.com.frevonamesa.frevonamesa.dto.RestauranteSettingsDTO;
import br.com.frevonamesa.frevonamesa.dto.RestauranteUpdateDTO; // NOVO IMPORT
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // NOVO ENDPOINT
    @PutMapping("/perfil")
    public ResponseEntity<RestaurantePerfilDTO> atualizarPerfil(@RequestBody RestauranteUpdateDTO updateDTO) {
        try {
            RestaurantePerfilDTO perfilAtualizado = restauranteService.atualizarPerfil(updateDTO);
            return ResponseEntity.ok(perfilAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/configuracoes")
    public ResponseEntity<RestaurantePerfilDTO> atualizarConfiguracoes(@RequestBody RestauranteSettingsDTO settingsDTO) {
        try {
            RestaurantePerfilDTO perfilAtualizado = restauranteService.atualizarConfiguracoes(settingsDTO);
            return ResponseEntity.ok(perfilAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}