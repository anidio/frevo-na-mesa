// src/pages/AdminPage.jsx

import React from 'react';
import { Link } from 'react-router-dom';

// Ícones para os cards
const MenuBookIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
    </svg>
);

const ChartBarIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
    </svg>
);

// Componente reutilizável para os cards, similar ao da HomePage
const AdminCard = ({ to, title, description, icon }) => (
    <Link to={to} className="block bg-white p-6 rounded-xl shadow-lg transition-transform transform hover:scale-105 hover:shadow-orange-200">
        <div className="flex justify-center mb-4 text-orange-500">{icon}</div>
        <h3 className="text-xl font-semibold text-center mb-2 text-gray-800">{title}</h3>
        <p className="text-sm text-center text-gray-500">{description}</p>
    </Link>
);


const AdminPage = () => {
    return (
        <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
            <div className="text-center mb-12">
                <h1 className="text-4xl font-extrabold text-gray-800 mb-2">Painel Administrativo</h1>
                <p className="text-lg text-gray-600">Gerencie os recursos e visualize os dados do sistema.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <AdminCard 
                    to="/admin/cardapio"
                    title="Gerenciar Cardápio"
                    description="Adicione, edite ou remova produtos do cardápio do restaurante."
                    icon={<MenuBookIcon />}
                />
                <AdminCard 
                    to="/admin/relatorios"
                    title="Relatórios de Vendas"
                    description="Visualize o faturamento do dia, detalhes por mesa e formas de pagamento."
                    icon={<ChartBarIcon />}
                />
            </div>
        </div>
    );
};

export default AdminPage;