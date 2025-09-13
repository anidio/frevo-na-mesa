import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PainelDelivery from '../components/PainelDelivery';
import PedidoDetalhesModal from '../components/PedidoDetalhesModal'; // NOVO
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext'; // NOVO

// Ícone da impressora
const PrinterIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm7-5a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2h2" /></svg>;

// NOVO: Componente para a Fila de Impressão de Delivery
const FilaImpressaoDelivery = ({ pedidos, onImprimir }) => {
    if (!pedidos || pedidos.length === 0) {
        return null; // Não mostra nada se não houver pedidos
    }
    return (
        <div>
            <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">
                Fila de Impressão ({pedidos.length})
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {pedidos.map(pedido => (
                    <div key={pedido.id} className="bg-white dark:bg-tema-surface-dark p-3 rounded-lg shadow-sm border-2 flex justify-between items-center animate-pulse-attention">
                        <div>
                            <p className="font-bold text-tema-text dark:text-tema-text-dark">Pedido #{pedido.id}</p>
                            <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">{pedido.nomeClienteDelivery}</p>
                        </div>
                        <button onClick={() => onImprimir(pedido)} className="bg-tema-primary text-white font-bold py-2 px-3 rounded-lg hover:bg-opacity-80 flex items-center gap-2 text-sm">
                            <PrinterIcon /> Imprimir
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
};

const DeliveryPage = () => {
  const { userProfile } = useAuth(); // NOVO
  const [pedidos, setPedidos] = useState({});
  const [pedidosParaImprimir, setPedidosParaImprimir] = useState([]); // NOVO
  const [pedidosFinalizados, setPedidosFinalizados] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pedidoSelecionado, setPedidoSelecionado] = useState(null); // NOVO

  const fetchPedidos = async () => {
    try {
      const promises = [
          apiClient.get('/api/pedidos/delivery'),
          apiClient.get('/api/pedidos/delivery/finalizados')
      ];
      // Só busca a fila de impressão se estiver ativa
      if (userProfile?.impressaoDeliveryAtivada) {
          promises.push(apiClient.get('/api/pedidos/delivery/pendentes'));
      }
      
      const results = await Promise.all(promises);
      setPedidos(results[0]);
      setPedidosFinalizados(results[1]);
      if (userProfile?.impressaoDeliveryAtivada) {
          setPedidosParaImprimir(results[2]);
      }

    } catch (error) {
      console.error('Erro ao buscar pedidos:', error);
      toast.error('Erro ao carregar pedidos de delivery');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (userProfile) { // Só busca dados quando o perfil do usuário carregar
        fetchPedidos();
        const interval = setInterval(fetchPedidos, 15000); // Atualiza a cada 15s
        return () => clearInterval(interval);
    }
  }, [userProfile]); // Roda o efeito quando o userProfile mudar

  const handleStatusChange = async (pedidoId, novoStatus) => {
    try {
      await apiClient.patch(`/api/pedidos/delivery/${pedidoId}/status`, { status: novoStatus });
      toast.success('Status do pedido atualizado!');
      fetchPedidos();
    } catch (error) {
      console.error('Erro ao atualizar status:', error);
      toast.error('Erro ao atualizar status do pedido');
    }
  };

  const handleImprimirPedido = async (pedido) => {
    // ... (lógica de impressão permanece a mesma, mas chama o endpoint correto)
    try {
      await apiClient.patch(`/api/pedidos/delivery/${pedido.id}/imprimir`);
      toast.success(`Pedido #${pedido.id} impresso e movido para 'Em Preparo'.`);
      fetchPedidos();
    } catch (error) {
      toast.error("Não foi possível processar a impressão.");
    }
  };

  return (
    <>
      <div className="w-full p-8 max-w-7xl mx-auto space-y-8">
        <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Painel de Delivery</h1>
            <Link to="/delivery/novo" className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors">
                Novo Pedido
            </Link>
        </div>

        {loading ? (
            <div className="text-center py-8"><p className="text-tema-text-muted dark:text-tema-text-muted-dark">Carregando pedidos...</p></div>
        ) : (
          <>
            {userProfile?.impressaoDeliveryAtivada && <FilaImpressaoDelivery pedidos={pedidosParaImprimir} onImprimir={handleImprimirPedido} />}
            <PainelDelivery pedidos={pedidos} onStatusChange={handleStatusChange} />

            <div>
              <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">Últimos Pedidos Entregues</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {pedidosFinalizados.length > 0 ? (
                  pedidosFinalizados.map(p => (
                    <div key={p.id} onClick={() => setPedidoSelecionado(p)} className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-md border dark:border-gray-700 cursor-pointer hover:border-tema-primary">
                        <div className="flex justify-between items-start">
                            <div>
                                <p className="font-semibold text-tema-text dark:text-tema-text-dark">Pedido #{p.id} - {p.nomeClienteDelivery}</p>
                                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">{new Date(p.dataHora).toLocaleTimeString('pt-BR')}</p>
                            </div>
                            <p className="font-bold text-lg text-tema-success">R$ {p.total.toFixed(2).replace('.', ',')}</p>
                        </div>
                    </div>
                  ))
                ) : (
                  <p className="text-tema-text-muted dark:text-tema-text-muted-dark text-center py-4 lg:col-span-3">Nenhum pedido finalizado recentemente.</p>
                )}
              </div>
            </div>
          </>
        )}
      </div>
      <PedidoDetalhesModal pedido={pedidoSelecionado} onClose={() => setPedidoSelecionado(null)} />
    </>
  );
};

export default DeliveryPage;