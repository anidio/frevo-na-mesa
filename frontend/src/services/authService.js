const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const login = async (email, senha) => {
    const response = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, senha }),
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Falha no login');
    }

    return response.json();
};

export const registrar = async (nome, email, senha, endereco) => {
    const response = await fetch(`${API_URL}/api/auth/registrar`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ nome, email, senha, endereco }),
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Falha no registro');
    }

    return response.text();
};