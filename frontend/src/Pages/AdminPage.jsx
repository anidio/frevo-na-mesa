import React from 'react';
import { Link } from 'react-router-dom';

const MenuBookIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>);
const ChartBarIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>);
const SettingsIcon = () => (<svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>);

const AdminCard = ({ to, title, description, icon }) => (
    <Link to={to} className="block bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg transition-all duration-300 transform hover:scale-105 border border-gray-200 dark:border-gray-700 hover:border-tema-primary dark:hover:border-tema-primary">
        <div className="flex justify-center mb-4 text-tema-primary">{icon}</div>
        <h3 className="text-xl font-semibold text-center mb-2 text-tema-text dark:text-tema-text-dark">{title}</h3>
        <p className="text-sm text-center text-tema-text-muted dark:text-tema-text-muted-dark">{description}</p>
    </Link>
);

const AdminPage = () => {
    return (
        <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
            <div className="text-center mb-12">
                <h1 className="text-4xl font-extrabold text-tema-text dark:text-tema-text-dark mb-2">Painel Administrativo</h1>
                <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Gerencie os recursos e visualize os dados do sistema.</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                <AdminCard to="/admin/cardapio" title="Gerenciar Cardápio" description="Adicione, edite ou remova produtos do cardápio." icon={<MenuBookIcon />} />
                <AdminCard to="/admin/relatorios" title="Relatórios de Vendas" description="Visualize o faturamento do dia, mesas e delivery." icon={<ChartBarIcon />} />
                <AdminCard to="/admin/configuracoes" title="Configurações" description="Ajuste as preferências de impressão e do sistema." icon={<SettingsIcon />} />
            </div>
        </div>
    );
};

export default AdminPage;