import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import AuthStatus from './AuthStatus';
import ThemeToggle from './ThemeToggle';
import { useAuth } from '../contexts/AuthContext'; // IMPORTADO

// Ícone da sombrinha para o header
const FrevoUmbrellaIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-tema-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 12v8a2 2 0 0 0 2 2h0a2 2 0 0 0 2-2v-1" />
        <path d="M12 12h-8a4 4 0 0 1 4-4 4 4 0 0 1 4 4Z" fill="#38bdf8" stroke="#0284c7" />
        <path d="M12 12h8a4 4 0 0 0-4-4 4 4 0 0 0-4 4Z" fill="#fbbf24" stroke="#f59e0b" />
        <path d="m12 12-4 4" stroke="#f43f5e" />
        <path d="m12 12 4 4" stroke="#4ade80" />
        <path d="M12 2v6" />
    </svg>
);

const MenuIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-tema-text dark:text-tema-text-dark" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
    </svg>
);

const CloseIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-tema-text dark:text-tema-text-dark" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
);

const Header = () => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const { userProfile, isLoggedIn } = useAuth(); // Usar o hook de autenticação

    // Determina se o Whitelabel está ativo (usuário logado E tem plano PRO/PREMIUM ativo)
    const isWhitelabelActive = isLoggedIn && (userProfile?.isDeliveryPro || userProfile?.isSalaoPro);
    const restauranteNome = userProfile?.nome || 'Frevo na Mesa';

    const renderBranding = () => {
        if (isWhitelabelActive) {
            // Se Whitelabel está ativo, mostra apenas o nome do restaurante
            return (
                <span className="text-xl font-bold text-tema-text dark:text-tema-text-dark">
                    {restauranteNome}
                </span>
            );
        }
        
        // Se plano GRATUITO ou deslogado, mostra o branding completo
        return (
            <>
                <FrevoUmbrellaIcon />
                <span className="text-xl font-bold text-tema-text dark:text-tema-text-dark">
                    Frevo na Mesa
                </span>
            </>
        );
    }

    return (
        <header className="bg-white/70 dark:bg-tema-surface-dark/70 backdrop-blur-sm shadow-sm sticky top-0 z-40 border-b border-gray-200 dark:border-gray-700">
            <div className="container mx-auto px-4 py-3 flex justify-between items-center">
                <Link to="/" className="flex items-center gap-2">
                    {renderBranding()}
                </Link>
                
                {/* Menu em telas grandes (md+) */}
                <div className="hidden md:flex items-center gap-4">
                    <ThemeToggle />
                    <AuthStatus />
                </div>

                {/* Ícone de menu (hambúrguer) em telas pequenas */}
                <div className="md:hidden">
                    <button onClick={() => setIsMenuOpen(!isMenuOpen)}>
                        {isMenuOpen ? <CloseIcon /> : <MenuIcon />}
                    </button>
                </div>

                {/* Menu suspenso para telas pequenas */}
                {isMenuOpen && (
                    <div className="absolute top-full right-4 mt-2 p-4 bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl border dark:border-gray-700 md:hidden z-50">
                        <div className="space-y-4">
                            <div className="flex justify-end">
                                <ThemeToggle />
                            </div>
                            <div className="border-t border-gray-200 dark:border-gray-700 pt-4">
                                <AuthStatus />
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </header>
    );
};

export default Header;