import React from 'react';
import { Link } from 'react-router-dom';

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Ícones e Componentes específicos desta página
// Ícone principal do Frevo na Mesa
const FrevoUmbrellaIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 12v8a2 2 0 0 0 2 2h0a2 2 0 0 0 2-2v-1" />
    <path d="M12 12h-8a4 4 0 0 1 4-4 4 4 0 0 1 4 4Z" fill="#38bdf8" stroke="#0284c7" />
    <path d="M12 12h8a4 4 0 0 0-4-4 4 4 0 0 0-4 4Z" fill="#fbbf24" stroke="#f59e0b" />
    <path d="m12 12-4 4" stroke="#f43f5e" />
    <path d="m12 12 4 4" stroke="#4ade80" />
    <path d="M12 2v6" />
  </svg>
);

// Ícone para o App do Garçom
const LampiaoHatIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 14h14a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2z" />
    <path d="M6 14c.8-3.33 3.33-5.83 6-6.5 2.67.67 5.17 3.17 6 6.5" />
    <path d="m10 10 1-1 1 1" />
    <path d="m14 10 1-1 1 1" />
  </svg>
);

// Ícone para o App do Caixa
const CactusIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 8v13" />
    <path d="M15.5 8a3.5 3.5 0 0 0-7 0v13" />
    <path d="M6 8a6 6 0 0 1 12 0" />
    <path d="M8 4a2 2 0 0 0-2-2h0a2 2 0 0 0-2 2v2" />
  </svg>
);

const ProfileCard = ({ title, description, features, icon, linkText, linkTo  }) => (
  <div className="bg-white p-6 rounded-xl shadow-lg transition-all duration-300 transform hover:scale-105 hover:ring-2 hover:ring-orange-400 hover:ring-opacity-75">
    <div className="flex justify-center mb-4 text-orange-500 text-4xl">{icon}</div>
    <h3 className="text-xl font-semibold text-center mb-2 text-gray-800">{title}</h3>
    <p className="text-sm text-center text-gray-500 mb-4">{description}</p>
    <ul className="list-disc list-inside text-gray-600 text-sm space-y-1 marker:text-orange-500">
      {features.map((feature, index) => (
        <li key={index}>{feature}</li>
      ))}
    </ul>
    <Link 
      to={linkTo} 
      className="block  w-full text-center mt-6 py-2 px-4 rounded-lg bg-orange-500 text-white font-bold transition-colors hover:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2"
    >
      {linkText}
    </Link>
  </div>
);

const HomePage = () => {
  return (
    <div className="w-full p-8 max-w-4xl mx-auto">
      {/* Seção do cabeçalho */}
      <div className="text-center mb-12">
        <div className="flex justify-center mb-2 text-orange-500 text-5xl hover:drop-shadow-glow-orange">
          <FrevoUmbrellaIcon />
        </div>
        <h1 className="text-4xl font-extrabold text-orange-600 mb-2">Frevo na Mesa</h1>
        <p className="text-lg text-gray-600">Sistema completo de PDV para restaurantes e bares</p>
        <p className="text-md text-gray-500 mt-2">Escolha seu perfil para começar</p>
      </div>

      {/* Cards de Perfil */}
      <div className="flex flex-col md:flex-row justify-center items-center md:space-x-8 space-y-8 md:space-y-0 mb-12">
        <ProfileCard
          title="App do Garçom"
          description="Gerencie mesas, faça pedidos e envie para a cozinha"
          features={[
            "Layout visual das mesas",
            "Cardápio completo organizado",
            "Múltiplos pedidos por mesa",
            "Interface touch otimizada",
          ]}
          icon={<LampiaoHatIcon />}
          linkText="Acessar como Garçom"
          linkTo="/mesas"
        />
        <ProfileCard
          title="App do Caixa"
          description="Visualize contas, processe pagamentos e feche mesas"
          features={[
            "Visão geral das contas abertas",
            "Detalhamento de pedidos",
            "Processamento de pagamentos",
            "Relatório de vendas diário",
          ]}
          icon={<CactusIcon />}
          linkText="Acessar como Caixa"
          linkTo="/caixa"
        />
      </div>
    </div>
  );
};

export default HomePage;