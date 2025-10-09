package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.dto.UsuarioDTO;
import br.com.frevonamesa.frevonamesa.model.Usuario;
import br.com.frevonamesa.frevonamesa.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Rota para o painel Admin: Lista Garçons, Caixas, e Admins
    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioService.listarUsuariosDoRestaurante();
    }

    // Rota para o Admin criar novo usuário (Garçom/Caixa) - Dispara a regra de limite
    @PostMapping
    public ResponseEntity<?> criarUsuario(@RequestBody UsuarioDTO dto) {
        try {
            Usuario novoUsuario = usuarioService.criarUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
        } catch (RuntimeException e) {
            // Retorna 400 Bad Request com a mensagem de erro (ex: Limite de usuários atingido!)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Rota para o Admin deletar um usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarUsuario(@PathVariable Long id) {
        try {
            usuarioService.deletarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}