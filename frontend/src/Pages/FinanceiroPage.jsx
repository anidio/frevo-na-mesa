import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import * as financeiroService from '../services/financeiroService';
import { useAuth } from '../contexts/AuthContext';
// Importa o modal de upgrade (que agora tem todos os novos planos)
import UpgradeModal from '../components/UpgradeModal'; 

// Ícone de Cartão
const CardIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3v-6a3 3 0 00-3-3H6a3 3 0 00-3 3v6a3 3 0 003 3z" /></svg>
);
// Ícone de Check/Successo
const CheckIcon = () => (<svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-tema-success" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" /></svg>);

const FinanceiroPage = () => {
    const { userProfile, refreshProfile } = useAuth(); // Certifique-se de que userProfile está sendo puxado
    const [statusPlano, setStatusPlano] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false); // NOVO: Estado para o modal de upgrade

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
        if (userProfile) {
            fetchData();
        }
    }, [userProfile]); // Recarrega se o perfil (e plano) mudar

    const handlePayPerUse = async () => {
        try {
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos(); 
            window.open(paymentUrl, '_blank');
            toast.info("Redirecionando para o Mercado Pago. Após o pagamento, atualize o seu perfil.");
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento.");
        }
    };
    
    // REMOVEMOS handleUpgradePro - O MODAL CUIDA DISSO

    const handleAtualizarDados = async () => {
        await refreshProfile();
        fetchData();
        toast.info("Dados atualizados com sucesso!");
    }


    if (loading) return <div className="p-8 text-center">Carregando dados financeiros...</div>;
    if (!statusPlano || !userProfile) return <div className="p-8 text-center text-red-500">Erro ao carregar o status do plano.</div>;

    const { 
        planoAtual, limiteMesas, limiteUsuarios, 
        pedidosMesAtual, limitePedidosGratuito, pedidosRestantesCompensados, 
        limiteAtingido, isLegacyFree
    } = statusPlano;

    const isGratuito = planoAtual === 'GRATUITO';
    // Novas flags modulares (puxadas do userProfile que é atualizado no refreshProfile)
    const isDeliveryPro = userProfile.isDeliveryPro || false;
    const isSalaoPro = userProfile.isSalaoPro || false; 
    
    const corPlano = isDeliveryPro || isSalaoPro ? 'text-green-600' : 'text-orange-500';

    return (
        <>
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
                
                {/* Status dos Módulos */}
                <div className="pt-2">
                    <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark mb-2">Módulos Ativos:</h3>
                    <div className="space-y-1 text-sm">
                        <div className={`flex justify-between items-center p-2 rounded ${isSalaoPro ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'}`}>
                            <span className="font-semibold text-tema-text dark:text-tema-text-dark">Gestão de Salão (Mesas)</span>
                            {isSalaoPro ? <span className="flex items-center gap-1 text-tema-success"><CheckIcon/> ATIVO</span> : <span className="text-red-500">Limitado ({limiteMesas} mesas)</span>}
                        </div>
                        <div className={`flex justify-between items-center p-2 rounded ${isDeliveryPro ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'}`}>
                            <span className="font-semibold text-tema-text dark:text-tema-text-dark">Delivery Próprio</span>
                            {isDeliveryPro ? <span className="flex items-center gap-1 text-tema-success"><CheckIcon/> ATIVO</span> : <span className="text-red-500">Limitado ({limitePedidosGratuito} pedidos)</span>}
                        </div>
                    </div>
                </div>

                {/* Pedidos Compensados */}
                {isGratuito && pedidosRestantesCompensados > 0 && (
                     <div className="p-4 bg-yellow-100 border border-yellow-400 text-yellow-700 rounded-lg text-sm font-semibold">
                        ✅ Você tem {pedidosRestantesCompensados} pedidos extras liberados pelo Pay-per-Use.
                    </div>
                )}
                
                {/* Opções de Ação (Substituímos o botão único pelo call to action modular) */}
                {isGratuito && !isLegacyFree && (
                    <div className="pt-4 border-t dark:border-gray-700 space-y-4">
                        <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark">Libere mais capacidade:</h3>
                        
                        {/* NOVO BOTÃO QUE ABRE O MODAL MODULAR */}
                        <button 
                            onClick={() => setIsUpgradeModalOpen(true)}
                            className="w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-md"
                        >
                            Ver Planos PRO e Assinar
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
            
            {/* Seção de Limites Gerais (Manter para contexto) */}
             <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700">
                <h3 className="text-xl font-bold text-tema-text dark:text-tema-text-dark border-b dark:border-gray-700 pb-2 mb-4">
                    Visão Geral dos Limites (Gratuito)
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
                    <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Mesas</p>
                        <p className="font-bold text-xl text-tema-text dark:text-tema-text-dark mt-1">{limiteMesas} / 10</p>
                    </div>
                    <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Usuários</p>
                        <p className="font-bold text-xl text-tema-text dark:text-tema-text-dark mt-1">{limiteUsuarios} / 3</p>
                    </div>
                     <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Pedidos Mês</p>
                        <p className={`font-bold text-xl mt-1 ${limiteAtingido ? 'text-red-500' : 'text-tema-success'}`}>{pedidosMesAtual} / {limitePedidosGratuito}</p>
                    </div>
                </div>
            </div>

        </div>
        {/* Renderiza o modal de Upgrade/Pagamento Modular */}
        {isUpgradeModalOpen && (
            <UpgradeModal 
                onClose={() => setIsUpgradeModalOpen(false)} 
                limiteAtual={limitePedidosGratuito}
                refreshProfile={refreshProfile}
                // onPedidoAceito não é necessário para upgrade de plano, mas é bom mantê-lo para compatibilidade se o fluxo for alterado
            />
        )}
        </>
    );
};

export default FinanceiroPage;