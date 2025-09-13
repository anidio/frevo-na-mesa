import React, { createContext, useState, useContext, useEffect } from 'react';

const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
    // Inicializa o tema buscando no localStorage ou usando 'light' como padrão
    const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light');

    useEffect(() => {
        const root = window.document.documentElement;
        
        root.classList.remove('light', 'dark'); // Limpa classes antigas
        root.classList.add(theme); // Adiciona a classe do tema atual ('light' ou 'dark')
        
        localStorage.setItem('theme', theme); // Salva a preferência do usuário
    }, [theme]);

    const toggleTheme = () => {
        setTheme(prevTheme => (prevTheme === 'light' ? 'dark' : 'light'));
    };

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme }}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = () => useContext(ThemeContext);