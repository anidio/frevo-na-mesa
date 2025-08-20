// src/pages/CaixaPage.jsx

import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify'; // Importa a função toast
import MesaCard from '../components/MesaCard';
import PagamentoModal from '../components/PagamentoModal';

// --- ÍCONES ---
const HeaderIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
  </svg>
);
const ClockIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);
const PaymentIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
  </svg>
);
const SummaryClockIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);
const SummaryDollarIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8v1m0 8v1m-6-9h12a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2v-8a2 2 0 012-2z" />
  </svg>
);
const SummaryCheckIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);


// --- COMPONENTE INTERNO ---
const DashboardCard = ({ icon, title, value, colorClass }) => (
  <div className="bg-white p-4 rounded-lg shadow-md">
    <div className="flex items-center text-sm text-gray-500 font-semibold gap-2">
      {icon}
      <span>{title}</span>
    </div>
    <p className={`text-3xl font-bold mt-2 text-left ${colorClass}`}>{value}</p>
  </div>
);

// --- COMPONENTE PRINCIPAL DA PÁGINA ---
const CaixaPage = () => {
  const [dashboardData, setDashboardData] = useState(null);
  const [mesas, setMesas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mesaParaPagamento, setMesaParaPagamento] = useState(null);

  const fetchData = async () => {
    try {
      const [dashboardRes, mesasRes] = await Promise.all([
        fetch('http://localhost:8080/api/caixa/dashboard'),
        fetch('http://localhost:8080/api/mesas')
      ]);
      const dashboard = await dashboardRes.json();
      const todasAsMesas = await mesasRes.json();
      setDashboardData(dashboard);
      setMesas(todasAsMesas);
    } catch (error) {
      console.error("Erro ao buscar dados do caixa:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const intervalId = setInterval(fetchData, 15000);
    return () => clearInterval(intervalId);
  }, []);

  const handleAbrirModal = (mesa) => {
    if (mesa.status === 'OCUPADA') {
      setMesaParaPagamento(mesa);
    } else if (mesa.status === 'PAGA') {
      // DE: alert(...)
      // PARA:
      toast.info(`A Mesa ${mesa.numero} já foi paga e está aguardando liberação do garçom.`);
    }
  };

  const handleCloseModal = () => {
    setMesaParaPagamento(null);
  };

  const handleConfirmarPagamento = async (mesaId, tipoPagamento) => {
    try {
      const response = await fetch(`http://localhost:8080/api/mesas/${mesaId}/pagar`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tipoPagamento: tipoPagamento }),
      });

      if (response.ok) {
        // DE: alert(...)
        // PARA:
        toast.success(`Pagamento da Mesa ${mesaId} (${tipoPagamento}) confirmado com sucesso!`);
        handleCloseModal();
        fetchData();
      } else {
        // DE: alert(...)
        // PARA:
        toast.error('Erro ao processar o pagamento.');
      }
    } catch (error) {
      console.error("Erro ao confirmar pagamento:", error);
      // DE: alert(...)
      // PARA:
      toast.error('Erro de comunicação com o servidor.');
    }
  };

  if (loading || !dashboardData) {
    return <div className="p-8 text-center"><p>Carregando dashboard do caixa...</p></div>;
  }

  const mesasOcupadas = mesas.filter(m => m.status === 'OCUPADA');
  const mesasPagas = mesas.filter(m => m.status === 'PAGA');

  return (
    <div className="w-full p-4 md:p-8 max-w-7xl mx-auto space-y-8">
      <div className="text-center text-orange-600">
        <div className="flex justify-center items-center gap-2">
          <HeaderIcon />
          <h1 className="text-3xl font-bold">Frevo Caixa</h1>
        </div>
        <p className="mt-1 text-gray-500">Gerencie pagamentos e acompanhe o movimento.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <DashboardCard icon={<SummaryClockIcon />} title="Mesas Abertas" value={dashboardData.mesasAbertas} colorClass="text-yellow-600" />
        <DashboardCard icon={<SummaryDollarIcon />} title="Em Aberto" value={`R$ ${dashboardData.totalEmAberto.toFixed(2).replace('.', ',')}`} colorClass="text-orange-600" />
        <DashboardCard icon={<SummaryCheckIcon />} title="Mesas Pagas" value={dashboardData.mesasPagas} colorClass="text-green-600" />
      </div>

      <div className="space-y-4">
        <h2 className="text-2xl font-semibold text-gray-700 flex items-center gap-2">
            <ClockIcon />
            Mesas com Contas Abertas ({mesasOcupadas.length})
        </h2>
        {mesasOcupadas.length > 0 ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {mesasOcupadas.map(mesa => <MesaCard key={mesa.id} mesa={mesa} onClick={() => handleAbrirModal(mesa)} />)}
          </div>
        ) : (
          <div className="bg-white p-6 rounded-lg text-center text-gray-500">Nenhuma mesa aberta no momento.</div>
        )}
      </div>

      <div className="space-y-4">
        <h2 className="text-2xl font-semibold text-gray-700 flex items-center gap-2">
            <PaymentIcon />
            Mesas Pagas - Aguardando Liberação ({mesasPagas.length})
        </h2>
        {mesasPagas.length > 0 ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {mesasPagas.map(mesa => <MesaCard key={mesa.id} mesa={mesa} onClick={() => handleAbrirModal(mesa)} />)}
          </div>
        ) : (
          <div className="bg-white p-6 rounded-lg text-center text-gray-500">Nenhuma mesa aguardando liberação.</div>
        )}
      </div>

      {mesaParaPagamento && (
        <PagamentoModal 
          mesa={mesaParaPagamento}
          onClose={handleCloseModal}
          onConfirm={handleConfirmarPagamento}
        />
      )}
    </div>
  );
};

export default CaixaPage;