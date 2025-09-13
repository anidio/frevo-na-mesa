import React, { createContext, useState, useContext, useEffect } from 'react';
import * as authService from '../services/authService';
import * as restauranteService from '../services/restauranteService'; 

// Cria o Contexto
const AuthContext = createContext(null);

// Cria o Provedor do Contexto
export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(null);
    const [userProfile, setUserProfile] = useState(null); 
    const [loadingProfile, setLoadingProfile] = useState(true);

    useEffect(() => {
        const fetchProfile = async () => {
            // Se não tem token, não faz nada e para de carregar
            if (!token) {
                setLoadingProfile(false);
                return;
            }
            
            try {
                const profileData = await restauranteService.getMeuPerfil();
                setUserProfile(profileData);
            } catch (error) {
                console.error("Falha ao buscar perfil, fazendo logout.", error);
                logout();
            } finally {
                setLoadingProfile(false);
            }
        };

        fetchProfile();
    }, [token]);


    // Função de Login
    const login = async (email, senha) => {
        try {
            const data = await authService.login(email, senha);
            setToken(data.token);
            localStorage.setItem('authToken', data.token); // Salva o token
            const profileData = await restauranteService.getMeuPerfil();
            setUserProfile(profileData);
            
            setToken(data.token);
        } catch (error) {
            console.error("Erro no login:", error);
            localStorage.removeItem('authToken');
            setToken(null);
            setUserProfile(null);
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
        userProfile,     
        loadingProfile, 
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