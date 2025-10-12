import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import * as financeiroService from '../services/financeiroService';
import { useAuth } from '../contexts/AuthContext';

// Ícone de Cartão
const CardIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3v-6a3 3 0 00-3-3H6a3 3 0 00-3 3v6a3 3 0 003 3z" /></svg>
);

const FinanceiroPage = () => {
    const { refreshProfile } = useAuth();
    const [statusPlano, setStatusPlano] = useState(null);
    const [loading, setLoading] = useState(true);

    const fetchData = async () => {
        try {
            const data = await apiClient.get('/api/financeiro/status-plano');
            setStatusPlano(data);
        } catch (error) {
            toast.error("Não foi possível carregar os dados financeiros.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handlePayPerUse = async () => {
        try {
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos(); 
            window.open(paymentUrl, '_blank');
            toast.info("Redirecionando para o Mercado Pago. Após o pagamento, atualize o seu perfil.");
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento.");
        }
    };
    
    const handleUpgradePro = async () => {
        try {
            const upgradeUrl = await financeiroService.iniciarUpgradePro(); 
            window.open(upgradeUrl, '_blank');
            toast.info("Redirecionando para a Assinatura PRO. Após o pagamento, atualize o seu perfil.");
        } catch (error) {
            toast.error(error.message || "Erro ao iniciar o Upgrade.");
        }
    };
    
    const handleAtualizarDados = async () => {
        await refreshProfile();
        fetchData();
        toast.info("Dados atualizados com sucesso!");
    }


    if (loading) return <div className="p-8 text-center">Carregando dados financeiros...</div>;
    if (!statusPlano) return <div className="p-8 text-center text-red-500">Erro ao carregar o status do plano.</div>;

    const { 
        planoAtual, limiteMesas, limiteUsuarios, 
        pedidosMesAtual, limitePedidosGratuito, pedidosRestantesCompensados, 
        limiteAtingido, isLegacyFree
    } = statusPlano;

    const isGratuito = planoAtual === 'GRATUITO';
    const isPlanoDeliveryPro = planoAtual === 'DELIVERY_PRO';
    const corPlano = isPlanoDeliveryPro ? 'text-green-600' : 'text-orange-500';

    return (
        <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark flex items-center gap-3">
                    <CardIcon /> Gerenciamento Financeiro
                </h1>
                <button 
                    onClick={handleAtualizarDados} 
                    className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors"
                >
                    Atualizar Dados
                </button>
            </div>

            <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-4">
                <h2 className={`text-2xl font-extrabold ${corPlano} border-b dark:border-gray-700 pb-2`}>
                    Plano Atual: {planoAtual} {isLegacyFree && ' (Cliente Piloto)'}
                </h2>
                
                {/* Status de Limites */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
                    <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Limite de Mesas</p>
                        <p className="font-bold text-xl text-tema-text dark:text-tema-text-dark mt-1">{limiteMesas} / {isPlanoDeliveryPro ? '50+' : '10'} </p>
                    </div>
                    <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Limite de Usuários</p>
                        <p className="font-bold text-xl text-tema-text dark:text-tema-text-dark mt-1">{limiteUsuarios} / {isPlanoDeliveryPro ? 'Ilimitado' : '3'}</p>
                    </div>
                    <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Pedidos no Mês</p>
                        <p className={`font-bold text-xl mt-1 ${limiteAtingido ? 'text-red-500' : 'text-tema-success'}`}>{pedidosMesAtual} / {isPlanoDeliveryPro ? 'Ilimitado' : limitePedidosGratuito}</p>
                    </div>
                </div>

                {/* Pedidos Compensados */}
                {isGratuito && pedidosRestantesCompensados > 0 && (
                     <div className="p-4 bg-yellow-100 border border-yellow-400 text-yellow-700 rounded-lg text-sm font-semibold">
                        ✅ Você tem {pedidosRestantesCompensados} pedidos extras liberados pelo Pay-per-Use.
                    </div>
                )}
                
                {/* Opções de Ação */}
                {isGratuito && !isLegacyFree && (
                    <div className="pt-4 border-t dark:border-gray-700 space-y-4">
                        <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark">Libere mais capacidade:</h3>
                        
                        <button 
                            onClick={handleUpgradePro}
                            className="w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-md"
                        >
                            Assinar Plano Delivery PRO (Vendas Ilimitadas)
                        </button>
                         <button 
                            onClick={handlePayPerUse}
                            className="w-full py-3 rounded-lg font-bold text-gray-800 bg-yellow-400 hover:bg-yellow-500 transition-colors"
                        >
                            Comprar Pacote de 10 Pedidos Extras
                        </button>
                    </div>
                )}
            </div>
            
            {/* Seção de Pagamentos Futuros (Placeholder para Stripe/MP Assinaturas) */}
            <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700">
                 <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark border-b dark:border-gray-700 pb-2 mb-4">
                    Gerenciamento de Assinatura (Futuro)
                </h2>
                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">
                    Nesta seção, futuramente você poderá gerenciar a recorrência da sua assinatura PRO e visualizar pagamentos pendentes.
                </p>
            </div>
        </div>
    );
};

export default FinanceiroPage;