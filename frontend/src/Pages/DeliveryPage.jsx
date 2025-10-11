import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PainelDelivery from '../components/PainelDelivery';
import PedidoDetalhesModal from '../components/PedidoDetalhesModal';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';
import UpgradeModal from '../components/UpgradeModal'; 

const DeliveryPage = () => {
  const { userProfile, refreshProfile } = useAuth(); 
  const [pedidos, setPedidos] = useState({});
  const [pedidosFinalizados, setPedidosFinalizados] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pedidoSelecionado, setPedidoSelecionado] = useState(null);
  const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false); 
  const [pedidoRetidoId, setPedidoRetidoId] = useState(null); // ESTADO CRÍTICO: Armazena ID do pedido retido
  
  // LÓGICA DE MONETIZAÇÃO
  const LIMITE_PEDIDOS_GRATUITO = 5;
  const isPlanoGratuito = userProfile?.plano === 'GRATUITO'; 
  const isLegacyFree = userProfile?.isLegacyFree;
  const pedidosAtuais = userProfile?.pedidosMesAtual || 0;
  // O limite é atingido se o plano for GRATUITO, não for LEGACY e o contador >= limite
  const isLimiteAtingido = !isLegacyFree && isPlanoGratuito && pedidosAtuais >= LIMITE_PEDIDOS_GRATUITO;


  const fetchPedidos = async () => {
    try {
      const [data, finalizadosData] = await Promise.all([
          apiClient.get('/api/pedidos/delivery'),
          apiClient.get('/api/pedidos/delivery/finalizados')
      ]);
      setPedidos(data);
      setPedidosFinalizados(finalizadosData);
    } catch (error) {
      console.error('Erro ao buscar pedidos:', error);
      toast.error('Erro ao carregar pedidos de delivery');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (userProfile) {
        fetchPedidos();
    }
    const interval = setInterval(fetchPedidos, 15000);
    return () => clearInterval(interval);
  }, [userProfile]);
  
  const handleStatusChange = async (pedidoId, novoStatus) => {
    try {
      await apiClient.patch(`/api/pedidos/delivery/${pedidoId}/status`, { status: novoStatus });
      toast.success('Status do pedido atualizado!');
      fetchPedidos();
    } catch (error) {
      toast.error(error.message || 'Erro ao atualizar status do pedido');
    }
  };

  const handleImprimirPedido = async (pedido) => {
    const conteudoImpressao = `
      <html><head><title>Pedido Delivery #${pedido.id}</title>
        <style>
          body { font-family: 'Courier New', Courier, monospace; font-size: 12px; width: 280px; margin: 0; padding: 10px; color: #000; }
          .header { text-align: center; font-weight: bold; margin-bottom: 10px; font-size: 14px; }
          .divider { border-top: 1px dashed #000; margin: 10px 0; }
        </style></head><body>
          <div class="header">
            PEDIDO DELIVERY #${pedido.id}<br>
            Cliente: ${pedido.nomeClienteDelivery}<br>
            ${new Date(pedido.dataHora).toLocaleString('pt-BR')}
          </div>
          <div class="divider"></div>
          ${pedido.itens.map(item => `<div>${item.quantidade}x ${item.produto.nome}</div>`).join('')}
        </body></html>
    `;
    const janelaImpressao = window.open('', '_blank');
    janelaImpressao.document.write(conteudoImpressao);
    janelaImpressao.document.close();
    janelaImpressao.focus();
    janelaImpressao.print();
    janelaImpressao.onafterprint = () => janelaImpressao.close();
    
    try {
      // Endpoint antigo 'imprimir' agora só marca o pedido, não muda o status
      await apiClient.patch(`/api/pedidos/delivery/${pedido.id}/imprimir`);
      toast.success(`Pedido #${pedido.id} marcado como impresso.`);
      fetchPedidos(); // Atualiza para mostrar o botão de imprimir desativado
    } catch (error) {
      toast.error("Não foi possível marcar o pedido como impresso.");
    }
  };

  // NOVO: Handler para abrir o modal de upgrade/pagamento
  const handleOpenUpgradeModal = (pedidoId) => {
    // 1. Armazena o ID do pedido retido (pode ser null se for o botão Novo Pedido)
    setPedidoRetidoId(pedidoId);
    // 2. Abre o modal
    setIsUpgradeModalOpen(true);
  };
  
  // NOVO: Ação executada pelo modal após o pagamento bem-sucedido (compensação)
  const handleAceitarPedidoAposPagamento = async () => {
      // 1. Fecha o modal
      setIsUpgradeModalOpen(false);
      
      if (pedidoRetidoId) {
          try {
              // CORREÇÃO FINAL: Chamamos o endpoint específico de ACEITAÇÃO PÓS-PAGAMENTO
              // O Back-end sabe que status deve ser aplicado (PENDENTE ou EM_PREPARO).
              await apiClient.patch(`/api/pedidos/delivery/${pedidoRetidoId}/aceitar-retido`, {}); 

              toast.success(`Pedido #${pedidoRetidoId} aceito com sucesso após o pagamento!`);
          } catch (error) {
              console.error("Erro ao aceitar pedido retido:", error);
              toast.error(error.message || "Erro ao aceitar o pedido retido. Tente atualizar a página.");
          } finally {
              // 2. Limpa o ID do pedido
              setPedidoRetidoId(null); 
          }
      } 
      // 3. Recarrega o painel (fundamental para atualizar o status do card)
      fetchPedidos(); 
  };


  return (
    <>
      <div className="w-full p-8 max-w-7xl mx-auto space-y-8">
        <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Painel de Delivery</h1>
            
            <Link 
              to={isLimiteAtingido ? "#" : "/delivery/novo"} 
              className={`font-bold py-2 px-4 rounded-lg transition-colors flex items-center gap-2 ${isLimiteAtingido ? 'bg-red-600 text-white cursor-not-allowed' : 'bg-tema-primary text-white hover:bg-opacity-80'}`}
              onClick={(e) => {
                if (isLimiteAtingido) {
                    e.preventDefault(); 
                    // Passamos null para o ID, pois estamos abrindo pelo botão geral
                    handleOpenUpgradeModal(null); 
                    toast.error("Limite de pedidos atingido. Faça o upgrade.");
                }
              }}
            >
                {isLimiteAtingido ? 'LIMITE ATINGIDO' : 'Novo Pedido'}
            </Link>
        </div>

        {loading ? (
            <div className="text-center py-8"><p className="text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</p></div>
        ) : (
          <>
            <PainelDelivery 
              pedidos={pedidos} 
              onStatusChange={handleStatusChange} 
              onImprimir={handleImprimirPedido}
              userProfile={userProfile}
              onOpenUpgradeModal={handleOpenUpgradeModal} // PROP PASSADA AO PAINEL
            />
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
      
      {/* Renderiza o modal de Upgrade/Pagamento */}
      {isUpgradeModalOpen && (
          <UpgradeModal 
              onClose={() => setIsUpgradeModalOpen(false)} 
              limiteAtual={LIMITE_PEDIDOS_GRATUITO}
              refreshProfile={refreshProfile}
              onPedidoAceito={handleAceitarPedidoAposPagamento} // CALLBACK PASSADO
          />
      )}
    </>
  );
};

export default DeliveryPage;