import React, { createContext, useState, useContext, useEffect } from 'react';
import * as authService from '../services/authService';

// Cria o Contexto
const AuthContext = createContext(null);

// Cria o Provedor do Contexto
export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(null);

    // Ao carregar a aplicação, verifica se existe um token no localStorage
    useEffect(() => {
        const storedToken = localStorage.getItem('authToken');
        if (storedToken) {
            setToken(storedToken);
        }
    }, []);

    // Função de Login
    const login = async (email, senha) => {
        try {
            const data = await authService.login(email, senha);
            setToken(data.token);
            localStorage.setItem('authToken', data.token); // Salva o token
        } catch (error) {
            console.error("Erro no login:", error);
            throw error; // Propaga o erro para o componente de UI tratar
        }
    };

    // Função de Logout
    const logout = () => {
        setToken(null);
        localStorage.removeItem('authToken'); // Remove o token
    };

    const authValue = {
        token,
        isLoggedIn: !!token, // Converte o token (string ou null) para um booleano
        login,
        logout,
    };

    return (
        <AuthContext.Provider value={authValue}>
            {children}
        </AuthContext.Provider>
    );
};

// Hook customizado para facilitar o uso do contexto
export const useAuth = () => {
    return useContext(AuthContext);
};