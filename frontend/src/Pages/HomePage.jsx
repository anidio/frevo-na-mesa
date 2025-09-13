import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const LampiaoHatIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 14h14a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2z" />
    <path d="M6 14c.8-3.33 3.33-5.83 6-6.5 2.67.67 5.17 3.17 6 6.5" />
    <path d="m10 10 1-1 1 1" />
    <path d="m14 10 1-1 1 1" />
  </svg>
);

const CactusIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 8v13" />
    <path d="M15.5 8a3.5 3.5 0 0 0-7 0v13" />
    <path d="M6 8a6 6 0 0 1 12 0" />
    <path d="M8 4a2 2 0 0 0-2-2h0a2 2 0 0 0-2 2v2" />
  </svg>
);

const SmartphoneIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
        <line x1="12" y1="18" x2="12.01" y2="18"></line>
    </svg>
);

const ProfileCard = ({ title, description, features, icon, linkText, linkTo  }) => (
    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg transition-all duration-300 transform hover:scale-105 border border-gray-200 dark:border-gray-700 flex flex-col">
        <div className="flex-grow">
            <div className="flex justify-center mb-4 text-tema-primary">{icon}</div>
            <h3 className="text-xl font-semibold text-center mb-2 text-tema-text dark:text-tema-text-dark">{title}</h3>
            <p className="text-sm text-center text-tema-text-muted dark:text-tema-text-muted-dark mb-4">{description}</p>
            <ul className="list-disc list-inside text-tema-text-muted dark:text-tema-text-muted-dark text-sm space-y-1 marker:text-tema-primary">
                {features.map((feature, index) => (
                    <li key={index}>{feature}</li>
                ))}
            </ul>
        </div>
        <Link 
            to={linkTo} 
            className="block w-full text-center mt-6 py-2 px-4 rounded-lg bg-tema-primary text-white font-bold transition-colors hover:bg-opacity-80"
        >
            {linkText}
        </Link>
    </div>
);

const HomePage = () => {
  const { isLoggedIn, userProfile, loadingProfile } = useAuth();

  const renderProfileCards = () => {
    if (isLoggedIn && loadingProfile) {
      return <p className="text-tema-text-muted">Carregando seu perfil...</p>;
    }

    if (isLoggedIn && userProfile) {
        switch (userProfile.tipo) {
            case 'APENAS_MESAS':
              return (
                <>
                  <ProfileCard title="App do Garçom" description="Gerencie mesas e faça pedidos." features={["Layout visual das mesas", "Cardápio completo", "Envio para a cozinha"]} icon={<LampiaoHatIcon />} linkText="Acessar Painel" linkTo="/mesas" />
                  <ProfileCard title="App do Caixa" description="Feche contas e veja relatórios." features={["Controle de contas", "Processar pagamentos", "Relatório de vendas"]} icon={<CactusIcon />} linkText="Acessar Painel" linkTo="/caixa" />
                </>
              );
            case 'APENAS_DELIVERY':
              return (
                <div className="md:col-span-2 lg:col-span-3 flex justify-center">
                    <div className="w-full max-w-sm">
                        <ProfileCard title="Painel de Delivery" description="Receba e gerencie seus pedidos." features={["Painel Kanban", "Notificações de pedidos", "Controle de status"]} icon={<SmartphoneIcon />} linkText="Acessar Painel" linkTo="/delivery" />
                    </div>
                </div>
              );
            case 'MESAS_E_DELIVERY':
              return (
                <>
                  <ProfileCard title="App do Garçom" description="Gerencie mesas e faça pedidos." features={["Layout visual das mesas", "Cardápio completo", "Envio para a cozinha"]} icon={<LampiaoHatIcon />} linkText="Acessar Painel" linkTo="/mesas" />
                  <ProfileCard title="Painel de Delivery" description="Receba e gerencie seus pedidos." features={["Painel Kanban", "Notificações de pedidos", "Controle de status"]} icon={<SmartphoneIcon />} linkText="Acessar Painel" linkTo="/delivery" />
                  <ProfileCard title="App do Caixa" description="Feche contas e veja relatórios." features={["Controle de contas", "Processar pagamentos", "Relatório de vendas"]} icon={<CactusIcon />} linkText="Acessar Painel" linkTo="/caixa" />
                </>
              );
            default:
              return <p className="text-tema-text-muted">Nenhum perfil de negócio configurado.</p>;
          }
    }

    return (
      <>
        <ProfileCard
          title="Gestão de Salão"
          description="Controle mesas e pedidos com uma interface ágil para garçons."
          features={["Layout visual das mesas", "Cardápio completo organizado", "Envio rápido para cozinha", "Controle de contas"]}
          icon={<LampiaoHatIcon />}
          linkText="Saiba Mais"
          linkTo="/"
        />
        <ProfileCard
          title="Cardápio Digital e Delivery"
          description="Venda online com seu próprio site e receba pedidos no painel."
          features={["Link exclusivo para seu cardápio", "Receba pedidos ilimitados", "Fácil de compartilhar", "Aumente suas vendas"]}
          icon={<SmartphoneIcon />}
          linkText="Saiba Mais"
          linkTo="/"
        />
        <ProfileCard
          title="Frente de Caixa (PDV)"
          description="Feche contas, gerencie pagamentos e acompanhe as vendas do dia."
          features={["Visão geral das contas", "Múltiplas formas de pagamento", "Relatório de vendas diário", "Fechamento de caixa"]}
          icon={<CactusIcon />}
          linkText="Saiba Mais"
          linkTo="/"
        />
      </>
    );
  };

  return (
    <div className="w-full p-8 max-w-6xl mx-auto">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-extrabold mb-2 text-tema-text dark:text-tema-text-dark">A gestão completa para seu negócio</h1>
        <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Do pedido na mesa ao delivery, tudo em um só lugar.</p>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 mb-12">
        {renderProfileCards()}
      </div>
    </div>
  );
};

export default HomePage;