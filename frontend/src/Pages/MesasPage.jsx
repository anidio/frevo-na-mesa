// src/Pages/MesasPage.jsx

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import MesaCard from '../components/MesaCard';
import apiClient from '../services/apiClient'; // Importa o novo apiClient

const GarcomIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
    </svg>
);

const MesasPage = () => {
  const [mesas, setMesas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [novoNumero, setNovoNumero] = useState('');
  const [novoNome, setNovoNome] = useState('');
  const navigate = useNavigate();

  const fetchData = async () => {
    try {
      // USA O API CLIENT PARA A CHAMADA GET
      const data = await apiClient.get('/api/mesas');
      data.sort((a, b) => a.numero - b.numero);
      setMesas(data);
    } catch (error) {
      console.error("Erro ao buscar mesas:", error);
      toast.error("Você precisa estar logado para ver as mesas.");
      // Futuramente, redirecionaremos para a página de login aqui
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAdicionarMesa = async () => {
    if (!novoNumero || isNaN(parseInt(novoNumero))) {
      toast.warn('Por favor, insira um número de mesa válido.');
      return;
    }
    try {
      
      await apiClient.post('/api/mesas', { 
        numero: parseInt(novoNumero),
        nomeCliente: novoNome });

      toast.success('Mesa adicionada com sucesso!');
      setIsModalOpen(false);
      setNovoNumero('');
      fetchData(); 
    } catch (error) {
      console.error("Erro de comunicação ao adicionar mesa:", error);
      toast.error(`Erro ao adicionar mesa: ${error.message}`);
    }
  };

  const mesasLivres = mesas.filter(m => m.status === 'LIVRE').length;
  const mesasOcupadas = mesas.filter(m => m.status === 'OCUPADA').length;
  const totalEmAberto = mesas
    .filter(m => m.status === 'OCUPADA')
    .reduce((acc, mesa) => acc + mesa.valorTotal, 0);

  if (loading) {
    return <div className="p-8 text-center">Carregando mesas...</div>;
  }

  return (
    <>
      <div className="w-full p-4 md:p-8 max-w-7xl mx-auto">
        <div className="text-center mb-8 text-tema-primary">
            <div className="flex justify-center items-center gap-2">
                <GarcomIcon />
                <h1 className="text-3xl font-bold">Frevo Garçom</h1>
            </div>
            <p className="mt-1 text-gray-500 dark:text-gray-400">Selecione uma mesa para começar o atendimento.</p>
        </div>
        
        <div className="flex justify-end mb-4">
            <button
                onClick={() => setIsModalOpen(true)}
                className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors flex items-center gap-2"
            >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" /></svg>
                Adicionar Mesa
            </button>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-6">
          {mesas.map(mesa => (
            <MesaCard key={mesa.id} mesa={mesa} onClick={() => navigate(`/mesas/${mesa.id}`)} />
          ))}
        </div>

        <div className="mt-12 p-4 bg-white dark:bg-tema-surface-dark rounded-lg shadow-md flex flex-wrap justify-around items-center gap-4 border border-gray-200 dark:border-gray-700">
          <div className="text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">Mesas Livres</p>
            <p className="text-2xl font-bold text-green-600 dark:text-green-400">{mesasLivres}</p>
          </div>
          <div className="text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">Mesas Ocupadas</p>
            <p className="text-2xl font-bold text-yellow-600 dark:text-yellow-400">{mesasOcupadas}</p>
          </div>
          <div className="text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">Total em Aberto</p>
            <p className="text-2xl font-bold text-tema-accent">
              R$ {totalEmAberto.toFixed(2).replace('.', ',')}
            </p>
          </div>
        </div>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
          <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-sm">
            <h2 className="text-2xl font-bold text-gray-800 dark:text-tema-text-dark mb-4">Adicionar Nova Mesa</h2>
            <div>
              <label htmlFor="table-number" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Número da Mesa</label>
              <input type="number" id="table-number" value={novoNumero} onChange={(e) => setNovoNumero(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md shadow-sm focus:outline-none focus:ring-orange-500 focus:border-orange-500" placeholder="Ex: 11" autoFocus />
            </div>
            <div>
              <label htmlFor="table-name" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Nome do Cliente</label>
              <input type="text" id="table-name" value={novoNome} onChange={(e) => setNovoNome(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md shadow-sm focus:outline-none focus:ring-orange-500 focus:border-orange-500" placeholder="Ex: nome" autoFocus />
            </div>
            <div className="mt-6 flex justify-end gap-4">
              <button onClick={() => setIsModalOpen(false)} className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">Cancelar</button>
              <button onClick={handleAdicionarMesa} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">Salvar Mesa</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default MesasPage;