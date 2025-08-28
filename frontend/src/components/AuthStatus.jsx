import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-toastify';

const AuthStatus = () => {
    const { isLoggedIn, logout, token } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        toast.info('Você foi desconectado.');
        navigate('/');
    };

    // Se o usuário estiver logado, mostra a saudação e o botão de Sair
    if (isLoggedIn) {
        return (
            <div className="flex items-center gap-4">
                <span className="text-sm text-gray-600 font-semibold">
                    Olá!
                </span>
                <button
                    onClick={handleLogout}
                    className="bg-red-500 text-white font-bold py-2 px-4 rounded-full hover:bg-red-600 transition-colors text-sm"
                >
                    Sair
                </button>
            </div>
        );
    }

    // Se estiver deslogado, mostra os botões de Login e Registrar
    return (
        <div className="flex items-center gap-3">
            <Link
                to="/login"
                className="bg-white text-orange-600 font-bold py-2 px-5 rounded-full hover:bg-orange-100 transition-colors border border-orange-200"
            >
                Login
            </Link>
            <Link
                to="/registrar"
                className="bg-orange-500 text-white font-bold py-2 px-5 rounded-full hover:bg-orange-600 transition-colors"
            >
                Registrar
            </Link>
        </div>
    );
};

export default AuthStatus;