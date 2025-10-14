// src/components/Footer.jsx

import React from 'react';
import { useAuth } from '../contexts/AuthContext'; // IMPORTAR AUTH

// Ícone do Instagram atualizado para herdar a cor do texto
const InstagramIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <rect width="20" height="20" x="2" y="2" rx="5" ry="5" strokeWidth="2"></rect>
    <path d="M16 11.37A4 4 0 1112.63 8 4 4 0 0116 11.37zm1.5-4.87h.01" strokeWidth="2"></path>
  </svg>
);

const Footer = () => {
    const { userProfile, isLoggedIn } = useAuth();
    
    // Whitelabel é ativo se qualquer plano PRO/PREMIUM estiver ativo
    const isWhitelabelActive = isLoggedIn && (userProfile?.isDeliveryPro || userProfile?.isSalaoPro);

    // Se o Whitelabel estiver ativo, o footer deve ser mínimo ou oculto
    if (isWhitelabelActive) {
        return (
            <footer className="bg-gray-100 text-gray-600 py-4 px-4 dark:bg-tema-fundo-dark dark:text-tema-text-muted-dark">
                <div className="container mx-auto text-center">
                    <p className="text-xs mt-1">© 2025 Todos os direitos reservados.</p>
                </div>
            </footer>
        );
    }
    
    // Conteúdo original para planos GRATUITOS/deslogados
    return (
        <footer className="bg-gray-100 text-gray-600 py-8 px-4 dark:bg-tema-fundo-dark dark:text-tema-text-muted-dark">
            <div className="container mx-auto text-center">
                
                {/* Branding da Empresa */}
                <p className="text-sm font-semibold text-tema-text dark:text-tema-text-dark">
                    Este é um produto <span className="font-bold text-tema-primary dark:text-tema-link-dark">MangueBit Code</span>.
                </p>
                <p className="text-xs mt-1">
                    Criamos tecnologia sob medida para simplificar processos, aumentar resultados e impulsionar o seu negócio. Fale com a gente e descubra como podemos ajudar você.
                </p>

                {/* Link para o Instagram atualizado */}
                <div className="mt-4">
                    <a 
                        href="https://www.instagram.com/nomedaempresa" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-2 p-2 rounded-lg transition-colors border-2 border-transparent hover:border-tema-primary dark:hover:border-tema-link-dark text-tema-primary dark:text-tema-link-dark"
                    >
                        <InstagramIcon />
                        <span className="font-bold">Siga-nos no Instagram!</span>
                    </a>
                </div>

                {/* Informações de Contato */}
                <div className="mt-4 border-t border-gray-300 dark:border-gray-700 pt-4">
                    <p className="text-xs">
                        Contato: contato@manguebitcode.com | (81) 99999-8888
                    </p>
                    <p className="text-xs mt-1">
                        © 2025 MagueBit Code. Todos os direitos reservados.
                    </p>
                </div>

            </div>
        </footer>
    );
};

export default Footer;