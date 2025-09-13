import apiClient from './apiClient';

export const getMeuPerfil = () => {
    return apiClient.get('/api/restaurante/meu-perfil');
};