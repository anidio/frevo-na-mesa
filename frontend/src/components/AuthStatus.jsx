import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-toastify';

const AuthStatus = () => {
    const { isLoggedIn, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        toast.info('Você foi desconectado.');
        navigate('/');
    };

    // Layout para botões de autenticação em telas grandes e pequenas
    const authButtonsLayoutClass = "flex items-center gap-3";
    const mobileMenuLayoutClass = "flex flex-col gap-3";

    if (isLoggedIn) {
        return (
            <div className="flex items-center gap-4">
                <span className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark font-semibold">
                    Olá!
                </span>
                <button
                    onClick={handleLogout}
                    className="bg-tema-accent text-white font-bold py-2 px-4 rounded-full hover:bg-opacity-80 transition-colors text-sm"
                >
                    Sair
                </button>
            </div>
        );
    }

    // AQUI ESTÁ A CORREÇÃO: Usamos flex-col para mobile e md:flex-row para desktop
    return (
        <div className="flex flex-col md:flex-row items-center gap-3">
            <Link
                to="/login"
                className="bg-gray-200 dark:bg-tema-surface-dark text-tema-primary dark:text-tema-text-dark font-bold py-2 px-5 rounded-full hover:bg-gray-300 dark:hover:bg-gray-700 transition-colors"
            >
                Login
            </Link>
            <Link
                to="/registrar"
                className="bg-tema-primary text-white font-bold py-2 px-5 rounded-full hover:bg-opacity-80 transition-colors"
            >
                Registrar
            </Link>
        </div>
    );
};

export default AuthStatus;