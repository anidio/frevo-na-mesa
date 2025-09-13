import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PainelDelivery from '../components/PainelDelivery';
import apiClient from '../services/apiClient';

const DeliveryPage = () => {
  const [pedidos, setPedidos] = useState({});
  const [loading, setLoading] = useState(true);

  const fetchPedidos = async () => {
    try {
      setLoading(true);
      const data = await apiClient.get('/api/pedidos/delivery');
      setPedidos(data);
    } catch (error) {
      console.error('Erro ao buscar pedidos de delivery:', error);
      toast.error('Erro ao carregar pedidos de delivery');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPedidos();
    // Configurar um intervalo para atualizar os pedidos a cada 30 segundos
    const interval = setInterval(fetchPedidos, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleStatusChange = async (pedidoId, novoStatus) => {
    try {
      await apiClient.put(`/api/pedidos/${pedidoId}/status`, { status: novoStatus });
      toast.success('Status do pedido atualizado!');
      fetchPedidos(); // Recarrega os pedidos para mostrar o status atualizado
    } catch (error) {
      console.error('Erro ao atualizar status:', error);
      toast.error('Erro ao atualizar status do pedido');
    }
  };

  return (
    <div className="w-full p-8 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Painel de Delivery</h1>
        <Link
          to="/delivery/novo"
          className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors"
        >
          Novo Pedido
        </Link>
      </div>

      {loading ? (
        <div className="text-center py-8">
          <p className="text-tema-text-muted dark:text-tema-text-muted-dark">Carregando pedidos...</p>
        </div>
      ) : (
        <PainelDelivery pedidos={pedidos} onStatusChange={handleStatusChange} />
      )}
    </div>
  );
};

export default DeliveryPage;