import React, { createContext, useState, useContext, useEffect } from 'react';
import * as authService from '../services/authService';
import * as restauranteService from '../services/restauranteService'; 

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(localStorage.getItem('authToken'));
    const [userProfile, setUserProfile] = useState(null); 
    const [loadingProfile, setLoadingProfile] = useState(true);

    const fetchProfile = async () => {
        if (!token) {
            setLoadingProfile(false);
            setUserProfile(null);
            return;
        }
        
        try {
            setLoadingProfile(true);
            const profileData = await restauranteService.getMeuPerfil();
            setUserProfile(profileData);
        } catch (error) {
            console.error("Falha ao buscar perfil, fazendo logout.", error);
            logout(); // Se o token for inválido, desloga
        } finally {
            setLoadingProfile(false);
        }
    };

    useEffect(() => {
        fetchProfile();
    }, [token]);

    const login = async (email, senha) => {
        const data = await authService.login(email, senha);
        localStorage.setItem('authToken', data.token);
        localStorage.setItem('userEmailForRefresh', email);
        localStorage.setItem('userPasswordForRefresh', senha);
        setToken(data.token); 
    };

    const logout = () => {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userEmailForRefresh');
        localStorage.removeItem('userPasswordForRefresh');
        setToken(null);
        setUserProfile(null);
    };

    const authValue = {
        token,
        isLoggedIn: !!token,
        userProfile,     
        loadingProfile, 
        login,
        logout,
        refreshProfile: fetchProfile, // NOVA FUNÇÃO EXPOSTA
    };

    return (
        <AuthContext.Provider value={authValue}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    return useContext(AuthContext);
};