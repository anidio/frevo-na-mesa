// src/components/Footer.jsx

import React from 'react';

// Ícone do Instagram
const InstagramIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <rect width="20" height="20" x="2" y="2" rx="5" ry="5" strokeWidth="2"></rect>
    <path d="M16 11.37A4 4 0 1112.63 8 4 4 0 0116 11.37zm1.5-4.87h.01" strokeWidth="2"></path>
  </svg>
);

const Footer = () => {
  return (
    <footer className="bg-gray-100 text-gray-600 py-8 px-4">
      <div className="container mx-auto text-center">
        
        {/* Branding da Empresa */}
        <p className="text-sm font-semibold">
          Este é um produto <span className="font-bold text-orange-500">Vértice Digital</span>.
        </p>
        <p className="text-xs mt-1">
          Criamos soluções inovadoras, de sistemas para restaurantes a ferramentas de conformidade e hardware.
        </p>

        {/* Link para o Instagram */}
        <div className="mt-4">
          <a 
            href="https://www.instagram.com/nomedaempresa" 
            target="_blank" 
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-800 transition-colors"
          >
            <InstagramIcon />
            <span className="font-bold">Siga-nos no Instagram!</span>
          </a>
        </div>

        {/* Informações de Contato */}
        <div className="mt-4 border-t border-gray-300 pt-4">
          <p className="text-xs">
            Contato: contato@verticedigital.com | (81) 99999-8888
          </p>
          <p className="text-xs mt-1">
            © 2025 Vértice Digital. Todos os direitos reservados.
          </p>
        </div>

      </div>
    </footer>
  );
};

export default Footer;