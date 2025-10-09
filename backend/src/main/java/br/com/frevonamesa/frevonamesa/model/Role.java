package br.com.frevonamesa.frevonamesa.model;

public enum Role {
    ADMIN,   // O dono do restaurante (acesso total)
    CAIXA,   // Acesso limitado a pagamentos e relatórios
    GARCOM   // Acesso apenas a mesas e pedidos
}