// frontend/src/contexts/AuthContext.jsx
import React, { createContext, useState, useContext, useEffect, useCallback } from 'react'; // Adicionar useCallback
import * as authService from '../services/authService';
import * as restauranteService from '../services/restauranteService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(localStorage.getItem('authToken'));
    const [userProfile, setUserProfile] = useState(null);
    const [loadingProfile, setLoadingProfile] = useState(true);

    // Usar useCallback para memoizar a função fetchProfile
    const fetchProfile = useCallback(async (isRefreshing = false) => {
        if (!token) {
            setUserProfile(null);
            setLoadingProfile(false); // Define loading como false se não houver token
            console.log("AuthContext: No token, profile set to null, loading stopped.");
            return;
        }

        // Define loading como true no início da busca (se for refresh ou inicial)
        setLoadingProfile(true);
        console.log("AuthContext: Starting profile fetch...");

        try {
            const profileData = await restauranteService.getMeuPerfil();
            console.log(">>> AuthContext - Data RECEIVED from API:", JSON.stringify(profileData, null, 2));
            setUserProfile(profileData); // Atualiza o perfil com os novos dados
            console.log(">>> AuthContext - setUserProfile CALLED with:", JSON.stringify(profileData, null, 2));
        } catch (error) {
            console.error("AuthContext - Failed to fetch profile, logging out.", error);
            // Considerar não fazer logout automaticamente em refresh, talvez só notificar
            // logout();
            setUserProfile(null); // Limpa o perfil em caso de erro
        } finally {
            setLoadingProfile(false); // Define loading como false APÓS tentar buscar e setar o perfil
            console.log("AuthContext: Profile fetch finished.");
        }
    }, [token]); // Depende apenas do token

    // Efeito para buscar o perfil quando o token muda ou na carga inicial
    useEffect(() => {
        console.log("AuthContext: useEffect [token] triggered. Token:", token ? "exists" : "does not exist");
        fetchProfile(); // Busca o perfil inicial
    }, [fetchProfile]); // Agora depende da função memoizada

    const login = async (email, senha) => {
        try {
             const data = await authService.login(email, senha);
             localStorage.setItem('authToken', data.token);
             setToken(data.token); // Atualiza o token, disparando o useEffect acima
             console.log("AuthContext: Login successful, token updated.");
             // fetchProfile será chamado pelo useEffect
        } catch(error) {
             console.error("AuthContext: Login error:", error);
             logout();
             throw error;
        }
    };

    const logout = () => {
        console.log("AuthContext: Starting logout...");
        localStorage.removeItem('authToken');
        setToken(null);
        setUserProfile(null);
        setLoadingProfile(false); // Garante que o loading pare no logout
        console.log("AuthContext: Logout complete.");
    };

    // Função exposta para atualização manual
    const refreshProfileManually = useCallback(async () => {
        console.log("AuthContext: refreshProfileManually called.");
        await fetchProfile(true); // Chama fetchProfile indicando refresh manual
    }, [fetchProfile]); // Depende da função memoizada

    const authValue = {
        token,
        isLoggedIn: !!token,
        userProfile,
        loadingProfile,
        login,
        logout,
        refreshProfile: refreshProfileManually,
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