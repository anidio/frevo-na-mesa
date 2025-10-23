import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom'; // Link não está sendo usado, mas mantido por precaução
import { toast } from 'react-toastify';
import PagamentoModal from '../components/PagamentoModal';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';

// --- Ícones (sem alterações) ---
const HeaderIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>);
const ClockIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>);
const PaymentIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" /></svg>);
const SummaryClockIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>);
const SummaryDollarIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8v1m0 8v1m-6-9h12a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2v-8a2 2 0 012-2z" /></svg>);
const SummaryCheckIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>);
const PrinterIcon = () => ( <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm7-5a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2h2" /></svg>);

const DashboardCard = ({ icon, title, value, colorClass }) => (
    <div className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div className="flex items-center text-sm text-tema-text-muted dark:text-tema-text-muted-dark font-semibold gap-2">
            {icon} <span>{title}</span>
        </div>
        <p className={`text-3xl font-bold mt-2 text-left ${colorClass}`}>{value}</p>
    </div>
);

const FilaDeImpressao = ({ pedidos, onImprimir }) => {
    if (!pedidos || pedidos.length === 0) {
        return <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg text-center text-tema-text-muted dark:text-tema-text-muted-dark border border-gray-200 dark:border-gray-700">Nenhum novo pedido para imprimir.</div>;
    }
    return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {pedidos.map(pedido => (
                <div key={pedido.id} className="bg-white dark:bg-tema-surface-dark p-3 rounded-lg shadow-sm border-2 flex justify-between items-center animate-pulse-border">
                    <div>
                        <p className="font-bold text-tema-text dark:text-tema-text-dark">Mesa {pedido.mesa.numero}</p>
                        <ul className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark list-disc pl-4 mt-1">
                            {pedido.itens.map(item => ( <li key={item.id}>{item.quantidade}x {item.produto.nome}</li> ))}
                        </ul>
                    </div>
                    <button onClick={() => onImprimir(pedido)} className="bg-tema-primary text-white font-bold py-2 px-3 rounded-lg hover:bg-opacity-80 flex items-center gap-2 text-sm">
                        <PrinterIcon /> Imprimir
                    </button>
                </div>
            ))}
        </div>
    );
};

const CaixaPage = () => {
    const { userProfile, loadingProfile } = useAuth();
    const [dashboardData, setDashboardData] = useState(null);
    const [mesas, setMesas] = useState([]);
    const [loading, setLoading] = useState(true);
    const [mesaParaPagamento, setMesaParaPagamento] = useState(null);
    const [pedidosParaImprimir, setPedidosParaImprimir] = useState([]);

    // **AJUSTE AQUI:** Usa as flags para decidir se busca dados de mesa
    const isGratuito = userProfile?.plano === 'GRATUITO';
    const mostrarModuloSalao = userProfile?.isSalaoPro || isGratuito;

    const fetchData = async () => {
        try {
            const promises = [apiClient.get('/api/caixa/dashboard')];

            // **AJUSTE AQUI:** Condição baseada em mostrarModuloSalao
            if (userProfile && mostrarModuloSalao) {
                promises.push(apiClient.get('/api/mesas'));
                // Busca pedidos pendentes apenas se a impressão estiver ativa E o módulo salão estiver visível
                if (userProfile.impressaoMesaAtivada) {
                    promises.push(apiClient.get('/api/pedidos/mesa/pendentes'));
                }
            }

            const results = await Promise.all(promises);
            setDashboardData(results[0]);

            // **AJUSTE AQUI:** Condição baseada em mostrarModuloSalao
            if (userProfile && mostrarModuloSalao) {
                setMesas(results[1] || []); // Garante que mesas seja um array
                if (userProfile.impressaoMesaAtivada && results.length > 2) {
                    setPedidosParaImprimir(results[2] || []); // Garante que seja array
                } else {
                     setPedidosParaImprimir([]); // Zera se a impressão não estiver ativa ou não buscou
                }
            } else {
                 setMesas([]); // Zera se o módulo não deve ser mostrado
                 setPedidosParaImprimir([]);
            }

        } catch (error) {
            console.error("Erro ao buscar dados do caixa:", error);
            toast.error("Não foi possível carregar os dados do caixa.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // Roda fetchData apenas se o perfil estiver carregado
        if (!loadingProfile && userProfile) {
            fetchData();
            // Configura o intervalo apenas depois da primeira busca
            const intervalId = setInterval(fetchData, 15000);
            return () => clearInterval(intervalId); // Limpa o intervalo ao desmontar
        } else if (!loadingProfile && !userProfile) {
             // Se terminou de carregar e não tem perfil (não logado?)
             setLoading(false); // Para o loading da página
             toast.error("Usuário não autenticado."); // Ou redireciona para login
        }
    }, [userProfile, loadingProfile]); // Re-executa se userProfile ou loadingProfile mudar

    // Exibe loading enquanto o perfil está carregando OU enquanto os dados do caixa estão carregando
    if (loadingProfile || loading) {
        return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</div>;
    }

    // Se não for mostrar o módulo salão (ex: plano Delivery Pro puro), exibe mensagem
    if (!mostrarModuloSalao) {
         return (
             <div className="w-full p-4 md:p-8 max-w-7xl mx-auto space-y-8 text-center">
                 <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">Módulo Caixa</h1>
                 <p className="text-tema-text-muted dark:text-tema-text-muted-dark">
                     Este módulo requer o plano Salão PDV ou Premium.
                     <Link to="/admin/financeiro" className="text-tema-primary ml-2 hover:underline">Ver planos</Link>
                 </p>
            </div>
         );
    }

    // Se o módulo deve ser mostrado, mas os dados não carregaram
    if (!dashboardData) {
        return <div className="p-8 text-center text-tema-accent dark:text-red-400">Não foi possível carregar os dados do painel.</div>
    }

    const mesasOcupadas = mesas.filter(m => m.status === 'OCUPADA');
    const mesasPagas = mesas.filter(m => m.status === 'PAGA');

    // Funções handleImprimirPedido, handleAbrirModal, handleCloseModal, handleConfirmarPagamento
    // permanecem EXATAMENTE as mesmas que você forneceu.
    const handleImprimirPedido = async (pedido) => {
        const conteudoImpressao = `
            <html><head><title>Pedido Mesa ${pedido.mesa.numero}</title>
            <style>
                body { font-family: 'Courier New', Courier, monospace; font-size: 12px; width: 280px; margin: 0; padding: 10px; color: #000; }
                .header { text-align: center; font-weight: bold; margin-bottom: 10px; font-size: 14px; }
                .item-line { display: flex; justify-content: space-between; margin-bottom: 5px; }
                .obs { font-style: italic; font-size: 10px; margin-left: 10px; }
                .divider { border-top: 1px dashed #000; margin: 10px 0; }
            </style></head><body>
                <div class="header">
                    PEDIDO - MESA ${pedido.mesa.numero}
                    ${pedido.mesa.nomeCliente ? `<br>Cliente: ${pedido.mesa.nomeCliente}` : ''}
                    <br>${new Date(pedido.dataHora).toLocaleString('pt-BR')}
                </div>
                <div class="divider"></div>
                ${pedido.itens.map(item => `
                    <div class="item-line"><span>${item.quantidade}x ${item.produto.nome}</span></div>
                    ${item.observacao ? `<div class="obs">Obs: ${item.observacao}</div>` : ''}
                `).join('')}
            </body></html>
        `;
        const janelaImpressao = window.open('', '_blank');
        janelaImpressao.document.write(conteudoImpressao);
        janelaImpressao.document.close();
        janelaImpressao.onload = function() {
            janelaImpressao.print();
            janelaImpressao.onafterprint = function() { janelaImpressao.close(); };
        };
        try {
            await apiClient.patch(`/api/pedidos/mesa/${pedido.id}/confirmar`);
            toast.success(`Pedido da Mesa ${pedido.mesa.numero} enviado para impressão.`);
            fetchData();
        } catch (error) {
            toast.error("Não foi possível marcar o pedido como impresso.");
        }
    };

    const handleAbrirModal = (mesa) => {
        if (mesa.status === 'OCUPADA') setMesaParaPagamento(mesa);
        else if (mesa.status === 'PAGA') toast.info(`A Mesa ${mesa.numero} já foi paga e está aguardando liberação do garçom.`);
    };

    const handleCloseModal = () => setMesaParaPagamento(null);

    const handleConfirmarPagamento = async (mesaId, tipoPagamento) => {
        try {
            await apiClient.patch(`/api/mesas/${mesaId}/pagar`, { tipoPagamento });
            toast.success(`Pagamento da Mesa ${mesaId} (${tipoPagamento}) confirmado com sucesso!`);
            handleCloseModal();
            fetchData();
        } catch (error) {
            console.error("Erro ao confirmar pagamento:", error);
            toast.error('Erro de comunicação com o servidor.');
        }
    };

    return (
        <div className="w-full p-4 md:p-8 max-w-7xl mx-auto space-y-8">
            <div className="text-center">
                <div className="flex justify-center items-center gap-2 text-tema-primary"><HeaderIcon /><h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Frevo Caixa</h1></div>
                <p className="mt-1 text-tema-text-muted dark:text-tema-text-muted-dark">Gerencie pagamentos e acompanhe o movimento.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <DashboardCard icon={<SummaryClockIcon />} title="Mesas Abertas" value={dashboardData.mesasAbertas} colorClass="text-orange-500" />
                <DashboardCard icon={<SummaryDollarIcon />} title="Em Aberto" value={`R$ ${dashboardData.totalEmAberto.toFixed(2).replace('.', ',')}`} colorClass="text-tema-accent" />
                <DashboardCard icon={<SummaryCheckIcon />} title="Mesas Pagas" value={dashboardData.mesasPagas} colorClass="text-tema-success" />
            </div>

            {/* **AJUSTE AQUI:** Renderização condicional baseada em mostrarModuloSalao */}
            {mostrarModuloSalao && (
                <>
                    {userProfile.impressaoMesaAtivada && (
                        <div className="space-y-4">
                            <h2 className="text-xl font-semibold text-tema-text dark:text-tema-text-dark flex items-center gap-2">
                                <PrinterIcon /> Fila de Impressão ({pedidosParaImprimir.length})
                            </h2>
                            <FilaDeImpressao pedidos={pedidosParaImprimir} onImprimir={handleImprimirPedido} />
                        </div>
                    )}

                    <div className="space-y-4">
                        <h2 className="text-xl font-semibold text-tema-text dark:text-tema-text-dark flex items-center gap-2">
                            <ClockIcon /> Mesas com Contas Abertas ({mesasOcupadas.length})
                        </h2>
                        {mesasOcupadas.length > 0 ? (
                            <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-3">
                                {mesasOcupadas.map(mesa => (
                                    <div key={mesa.id} onClick={() => handleAbrirModal(mesa)} className="bg-white dark:bg-tema-surface-dark border border-gray-200 dark:border-gray-700 rounded-lg p-3 text-center cursor-pointer hover:border-tema-primary transition-colors">
                                        <p className="font-bold text-tema-text dark:text-tema-text-dark truncate">Mesa {mesa.numero}</p>
                                        {mesa.nomeCliente && <p className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark truncate" title={mesa.nomeCliente}>{mesa.nomeCliente}</p>}
                                        <p className="font-semibold text-sm text-tema-text dark:text-tema-text-dark">R$ {mesa.valorTotal.toFixed(2).replace('.', ',')}</p>
                                    </div>
                                ))}
                            </div>
                        ) : (<div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg text-center text-tema-text-muted dark:text-tema-text-muted-dark border dark:border-gray-700">Nenhuma mesa aberta no momento.</div>)}
                    </div>

                    <div className="space-y-4">
                        <h2 className="text-xl font-semibold text-tema-text dark:text-tema-text-dark flex items-center gap-2">
                            <PaymentIcon /> Mesas Pagas - Aguardando Liberação ({mesasPagas.length})
                        </h2>
                        {mesasPagas.length > 0 ? (
                            <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-3">
                                {mesasPagas.map(mesa => (
                                    <div key={mesa.id} onClick={() => handleAbrirModal(mesa)} className="bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-3 text-center cursor-pointer hover:border-gray-400 transition-colors">
                                        <p className="font-bold text-gray-600 dark:text-gray-300 truncate">Mesa {mesa.numero}</p>
                                        {mesa.nomeCliente && <p className="text-xs text-gray-500 dark:text-gray-400 truncate" title={mesa.nomeCliente}>{mesa.nomeCliente}</p>}
                                    </div>
                                ))}
                            </div>
                        ) : (<div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg text-center text-tema-text-muted dark:text-tema-text-muted-dark border dark:border-gray-700">Nenhuma mesa aguardando liberação.</div>)}
                    </div>
                </>
            )}

            {mesaParaPagamento && (<PagamentoModal mesa={mesaParaPagamento} onClose={handleCloseModal} onConfirm={handleConfirmarPagamento} />)}
        </div>
    );
};

export default CaixaPage;