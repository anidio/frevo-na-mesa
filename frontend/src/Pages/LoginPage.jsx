import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-toastify';
import Header from '../components/Header';
import Footer from '../components/Footer';

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
            navigate('/');
        } catch (error) {
            toast.error(error.message || 'Email ou senha inválidos.');
        }
    };

    return (
        <div className="flex flex-col min-h-screen bg-tema-fundo dark:bg-tema-fundo-dark">
            <Header />
            <div className="flex-grow flex items-center justify-center p-4">
                <div className="w-full max-w-md p-8 space-y-6 bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl border dark:border-gray-700">
                    <h2 className="text-2xl font-bold text-center text-tema-text dark:text-tema-text-dark">Login - Frevo na Mesa</h2>
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Email</label>
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                className="w-full px-3 py-2 mt-1 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:ring-tema-primary focus:border-tema-primary dark:bg-gray-800 dark:text-white"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Senha</label>
                            <input
                                type="password"
                                value={senha}
                                onChange={(e) => setSenha(e.target.value)}
                                required
                                className="w-full px-3 py-2 mt-1 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:ring-tema-primary focus:border-tema-primary dark:bg-gray-800 dark:text-white"
                            />
                        </div>
                        <button type="submit" className="w-full py-2 px-4 font-semibold text-white bg-tema-primary rounded-md hover:bg-opacity-80 transition-colors">
                            Entrar
                        </button>
                    </form>
                    <p className="text-sm text-center text-tema-text-muted dark:text-tema-text-muted-dark">
                        Não tem uma conta?{' '}
                        <Link to="/registrar" className="font-medium text-tema-primary hover:underline">
                            Cadastre-se
                        </Link>
                    </p>
                </div>
            </div>
            <Footer />
        </div>
    );
};

export default LoginPage;