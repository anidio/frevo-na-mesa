// frontend/src/services/apiClient.js

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// FUNÇÃO CHAVE: Pega o token do localStorage e prepara o cabeçalho
const getAuthHeaders = () => {
    const token = localStorage.getItem('authToken');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
};

const handleResponse = async (response) => {
    if (!response.ok) {
        const errorText = await response.text();
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
    const text = await response.text();
    return text;
};

// Objeto apiClient agora usa getAuthHeaders() automaticamente
const apiClient = {
    get: async (endpoint) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'GET',
            headers: getAuthHeaders(), // IMPORTANTE
        });
        return handleResponse(response);
    },

    post: async (endpoint, body) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'POST',
            headers: getAuthHeaders(), // IMPORTANTE
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },

    patch: async (endpoint, body) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'PATCH',
            headers: getAuthHeaders(), // IMPORTANTE
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },

    put: async (endpoint, body) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'PUT',
            headers: getAuthHeaders(), // IMPORTANTE
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },

    delete: async (endpoint) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'DELETE',
            headers: getAuthHeaders(), // IMPORTANTE
        });
        return handleResponse(response);
    },
};

export default apiClient;