import React from 'react';
import { Link } from 'react-router-dom';

// Reutilizando os ícones de AdminPage.jsx
const UsersIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20h10m-2-9a4 4 0 11-8 0 4 4 0 018 0zM7 10h.01M17 10h.01" />
    </svg>
);
const UserSettingsIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5.121 17.804A13.935 13.935 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 9a2 2 0 002-2v-2c0-1.1-.9-2-2-2h-2c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h2z" /></svg>
);
const MapPinIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
        <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
);
const SettingsIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
);

const HubCard = ({ to, title, description, icon }) => (
    <Link to={to} className="block bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg transition-all duration-300 transform hover:scale-105 border border-gray-200 dark:border-gray-700 hover:border-tema-primary dark:hover:border-tema-primary">
        <div className="flex justify-center mb-4 text-tema-primary">{icon}</div>
        <h3 className="text-xl font-semibold text-center mb-2 text-tema-text dark:text-tema-text-dark">{title}</h3>
        <p className="text-sm text-center text-tema-text-muted dark:text-tema-text-muted-dark">{description}</p>
    </Link>
);


const SettingsHubPage = () => {
    return (
        <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-6">
            <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark text-center">Gestão Administrativa</h1>
            <p className="text-center text-tema-text-muted dark:text-tema-text-muted-dark">Ajuste as informações principais e a operação do seu restaurante.</p>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-4">
                
                <HubCard 
                    to="/admin/perfil" 
                    title="Perfil e Endereço" 
                    description="Atualize nome, endereço, CEP e logo do restaurante." 
                    icon={<UserSettingsIcon />} 
                />
                
                <HubCard 
                    to="/admin/usuarios" 
                    title="Gerenciar Usuários" 
                    description="Cadastre Garçons e Caixas e gerencie os acessos do Staff." 
                    icon={<UsersIcon />} 
                />
                
                <HubCard 
                    to="/admin/areas-entrega" 
                    title="Faixas de Entrega (KM)" 
                    description="Defina as áreas de cobertura, distâncias e preços de frete." 
                    icon={<MapPinIcon />} 
                />
                
                <HubCard 
                    to="/admin/configuracoes" 
                    title="Ajustes Operacionais" 
                    description="Configure impressão de pedidos, Valor Fixo da Taxa e Webhook do WhatsApp." 
                    icon={<SettingsIcon />} 
                />
            </div>
            <div className="flex justify-center pt-8">
                <Link to="/admin" className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">
                    Voltar ao Admin
                </Link>
            </div>
        </div>
    );
};

export default SettingsHubPage;