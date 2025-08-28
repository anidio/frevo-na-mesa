import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-toastify';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await login(email, senha);
            toast.success('Login realizado com sucesso!');
            navigate('/'); // Redireciona para a Home após o login
        } catch (error) {
            toast.error(error.message || 'Email ou senha inválidos.');
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-orange-50">
            <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-lg shadow-md">
                <h2 className="text-2xl font-bold text-center text-gray-800">Login - Frevo na Mesa</h2>
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            className="w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm focus:ring-orange-500 focus:border-orange-500"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Senha</label>
                        <input
                            type="password"
                            value={senha}
                            onChange={(e) => setSenha(e.target.value)}
                            required
                            className="w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm focus:ring-orange-500 focus:border-orange-500"
                        />
                    </div>
                    <button type="submit" className="w-full py-2 px-4 font-semibold text-white bg-orange-600 rounded-md hover:bg-orange-700">
                        Entrar
                    </button>
                </form>
                <p className="text-sm text-center text-gray-600">
                    Não tem uma conta?{' '}
                    <Link to="/registrar" className="font-medium text-orange-600 hover:underline">
                        Cadastre-se
                    </Link>
                </p>
            </div>
        </div>
    );
};

export default LoginPage;