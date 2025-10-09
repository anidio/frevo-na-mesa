package br.com.frevonamesa.frevonamesa.dto;

import br.com.frevonamesa.frevonamesa.model.Role;
import lombok.Data;

@Data
public class UsuarioDTO {
    private Long id;
    private String nome;
    private String email;
    private String senha; // A senha será criptografada pelo service
    private Role role;
}