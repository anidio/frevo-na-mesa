import React from 'react';
import { Link } from 'react-router-dom';
import AuthStatus from './AuthStatus';
import ThemeToggle from './ThemeToggle';

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


const Header = () => {
    return (
        <header className="bg-white/70 dark:bg-tema-surface-dark/70 backdrop-blur-sm shadow-sm sticky top-0 z-40 border-b border-gray-200 dark:border-gray-700">
            <div className="container mx-auto px-4 py-3 flex justify-between items-center">
                <Link to="/" className="flex items-center gap-2">
                    <FrevoUmbrellaIcon />
                    <span className="text-xl font-bold text-tema-text dark:text-tema-text-dark">
                        Frevo na Mesa
                    </span>
                </Link>
                <div className="flex items-center gap-4">
                    <ThemeToggle />
                    <AuthStatus />
                </div>
            </div>
        </header>
    );
};

export default Header;