// src/services/apiClient.js

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const getAuthHeaders = () => {
    const token = localStorage.getItem('authToken');
    // Se não houver token, não envia o cabeçalho Authorization
    if (!token) {
        return {
            'Content-Type': 'application/json',
        };
    }
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
};

const handleResponse = async (response) => {
    if (!response.ok) {
        const errorText = await response.text();
        // Se o erro for um JSON (comum em erros de validação), tenta extrair a mensagem
        try {
            const errorJson = JSON.parse(errorText);
            throw new Error(errorJson.message || errorText);
        } catch (e) {
            throw new Error(errorText || `Erro HTTP: ${response.status}`);
        }
    }

    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return response.json();
    }
    // Retorna o texto da resposta se não for JSON (ex: "Restaurante registrado com sucesso!")
    const text = await response.text();
    return text;
};

const apiClient = {
    get: async (endpoint) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        return handleResponse(response);
    },

    post: async (endpoint, body) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },

    patch: async (endpoint, body) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'PATCH',
            headers: getAuthHeaders(),
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },

    // MÉTODO PUT PARA ATUALIZAR RECURSOS
    put: async (endpoint, body) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },

    delete: async (endpoint) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'DELETE',
            headers: getAuthHeaders(),
        });
        return handleResponse(response);
    },
};

export default apiClient;