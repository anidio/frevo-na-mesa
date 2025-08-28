package br.com.frevonamesa.frevonamesa.controller;

import br.com.frevonamesa.frevonamesa.config.jwt.JwtUtil;
import br.com.frevonamesa.frevonamesa.dto.AuthRequest;
import br.com.frevonamesa.frevonamesa.dto.AuthResponse;
import br.com.frevonamesa.frevonamesa.dto.RestauranteDTO;
import br.com.frevonamesa.frevonamesa.service.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // MUDANÇA AQUI
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService; // MUDANÇA AQUI

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarRestaurante(@RequestBody RestauranteDTO restauranteDTO) {
        try {
            restauranteService.cadastrar(restauranteDTO);
            return ResponseEntity.ok("Restaurante registrado com sucesso!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> criarTokenDeAutenticacao(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getSenha())
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Email ou senha inválidos");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail()); // MUDANÇA AQUI
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}