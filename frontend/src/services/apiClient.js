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
        
        let errorMsg = errorText || `Erro HTTP: ${response.status}`;

        // 1. Tenta parsear o JSON para buscar o código de erro customizado do backend
        try {
            const errorJson = JSON.parse(errorText);
            
            if (errorJson.errorCode && errorJson.errorCode.includes("PEDIDO_LIMIT_REACHED")) {
                // Ação crítica: Lançamos APENAS a string detectável.
                throw new Error("PEDIDO_LIMIT_REACHED");
            } else if (errorJson.message) {
                // Se for outro erro de negócio (RuntimeException), usa a mensagem de erro.
                errorMsg = errorJson.message;
            }
            
        } catch (e) {
            // Se a exceção já foi lançada no bloco try, relançamos.
            if (e.message && e.message.includes("PEDIDO_LIMIT_REACHED")) {
                 throw e; 
            }
            // Se o JSON não foi válido, a mensagem de erro será o texto HTTP padrão.
        }

        // Lançamento do erro generalizado, caso não seja o de limite.
        throw new Error(errorMsg);
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