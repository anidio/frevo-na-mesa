import React, { createContext, useState, useContext, useEffect } from 'react';
import * as authService from '../services/authService';
import * as restauranteService from '../services/restauranteService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(localStorage.getItem('authToken'));
    const [userProfile, setUserProfile] = useState(null);
    const [loadingProfile, setLoadingProfile] = useState(true); // Começa true

    const fetchProfile = async (isRefreshing = false) => { // Adiciona parâmetro opcional
        if (!token) {
            setLoadingProfile(false);
            setUserProfile(null);
            console.log("AuthContext: Sem token, perfil definido como null.");
            return;
        }

        // Se for um refresh manual (botão), força o estado de loading
        if (isRefreshing) {
            setLoadingProfile(true);
        }

        console.log("AuthContext: Iniciando busca de perfil...");
        try {
            // setLoadingProfile(true); // Removido daqui, controlado pelo isRefreshing ou carga inicial
            const profileData = await restauranteService.getMeuPerfil();
            // --- LOG 1: Dados recebidos da API ---
            console.log(">>> AuthContext - Dados RECEBIDOS da API:", JSON.stringify(profileData, null, 2));
            setUserProfile(profileData);
            // --- LOG 2: Estado após setUserProfile ---
            // Este log pode mostrar o estado *antes* da atualização assíncrona do React,
            // mas confirma que setUserProfile foi chamado com os dados corretos.
            console.log(">>> AuthContext - setUserProfile CHAMADO com:", JSON.stringify(profileData, null, 2));
        } catch (error) {
            console.error("AuthContext - Falha ao buscar perfil, fazendo logout.", error);
            logout(); // Se o token for inválido ou API falhar, desloga
        } finally {
            setLoadingProfile(false); // Garante que loading termine
            console.log("AuthContext: Busca de perfil finalizada.");
        }
    };

    // Efeito para buscar o perfil quando o token muda (login/logout) ou na carga inicial
    useEffect(() => {
        console.log("AuthContext: useEffect [token] disparado. Token:", token ? "existe" : "não existe");
        fetchProfile(); // Busca o perfil inicial
    }, [token]);

    const login = async (email, senha) => {
        try {
             const data = await authService.login(email, senha);
             localStorage.setItem('authToken', data.token);
             setToken(data.token); // Atualiza o token, o que dispara o useEffect acima
             console.log("AuthContext: Login bem-sucedido, token atualizado.");
             // fetchProfile será chamado pelo useEffect
        } catch(error) {
             console.error("AuthContext: Erro no login:", error);
             logout(); // Garante limpeza em caso de erro no login
             throw error; // Re-lança o erro para o LoginPage tratar
        }
    };

    const logout = () => {
        console.log("AuthContext: Iniciando logout...");
        localStorage.removeItem('authToken');
        setToken(null);
        setUserProfile(null);
        console.log("AuthContext: Logout concluído.");
    };

    // Função exposta para atualização manual
    const refreshProfileManually = async () => {
        console.log("AuthContext: refreshProfileManually chamado.");
        await fetchProfile(true); // Chama fetchProfile indicando que é um refresh manual
    }

    const authValue = {
        token,
        isLoggedIn: !!token,
        userProfile,
        loadingProfile,
        login,
        logout,
        refreshProfile: refreshProfileManually, // Expõe a função de refresh manual
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