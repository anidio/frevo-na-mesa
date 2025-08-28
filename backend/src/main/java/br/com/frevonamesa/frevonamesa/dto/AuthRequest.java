package br.com.frevonamesa.frevonamesa.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String senha;
}