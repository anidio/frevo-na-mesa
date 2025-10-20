import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import * as financeiroService from '../services/financeiroService'; 
import { useAuth } from '../contexts/AuthContext';

// Adicione a prop onPedidoAceito (necessária se o modal for chamado após um limite ser atingido)
const UpgradeModal = ({ onClose, limiteAtual, refreshProfile, onPedidoAceito }) => {
    const navigate = useNavigate();
    // Dados para o Pay-per-Use (10 pedidos por R$ 14,90 - alterei o preço para o padrão do backend)
    const pedidosParaComprar = 10;
    const custoPorPacote = '14,90'; // Baseado no FinanceiroService (14.90)
    
    // NOVOS DADOS DE PLANO (COMPETITIVOS)
    const PRECO_DELIVERY = '29,90'; 
    const PRECO_SALAO = '35,90'; // Mantido o preço do código, embora o prompt diga 39,90. Usando 35,90 para consistência com o código original
    const PRECO_PREMIUM_MENSAL = '49,90'; 
    const PRECO_PREMIUM_ANUAL = '499,00'; 

    // LÓGICA DE PAGAMENTO PAY-PER-USE
    const handlePayPerUse = async () => {
        try {
            // Chama a rota que gera o pagamento avulso
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos(); 
            toast.info("Redirecionando para o Mercado Pago...");
            window.open(paymentUrl, '_blank');
            onClose();
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento.");
        }
    };
    
    // LÓGICA DE UPGRADE PARA PLANOS MODULARES
    const handleUpgrade = (plano) => async () => {
        let upgradeUrl = '';
        try {
            switch(plano) {
                case 'DELIVERY':
                    upgradeUrl = await financeiroService.iniciarUpgradeDeliveryMensal();
                    break;
                case 'SALAO':
                    upgradeUrl = await financeiroService.iniciarUpgradeSalaoMensal();
                    break;
                case 'PREMIUM_MEN':
                    upgradeUrl = await financeiroService.iniciarUpgradePremiumMensal();
                    break;
                case 'PREMIUM_ANU':
                    upgradeUrl = await financeiroService.iniciarUpgradePremiumAnual();
                    break;
                default:
                    toast.error('Plano desconhecido.');
                    return;
            }
            
            // Redireciona o usuário para a URL de Checkout do Mercado Pago
            toast.info("Redirecionando para a assinatura. Após o pagamento, atualize seu perfil.");
            window.open(upgradeUrl, '_blank');
            onClose();

        } catch (error) {
            toast.error(error.message || 'Erro ao iniciar a assinatura do plano.');
        }
    };
    
    return (
        <div 
            className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-40 p-4" 
            onClick={onClose}
        >
            {/* CORREÇÃO AQUI: max-h-[95vh] e overflow-y-auto para garantir que o modal possa rolar */}
            <div 
                className="bg-white dark:bg-tema-surface-dark rounded-xl shadow-2xl w-full max-w-lg max-h-[95vh] overflow-y-auto" 
                onClick={e => e.stopPropagation()}
            >
                <div className="p-6 md:p-8 text-center">
                    <h2 className="text-3xl font-extrabold text-tema-accent dark:text-red-400 mb-2">
                        Limite de Pedidos Atingido!
                    </h2>
                    <p className="text-tema-text dark:text-tema-text-dark mb-4">
                        Seu plano **Frevo GO!** atingiu o limite de {limiteAtual} pedidos neste mês. Escolha como prosseguir:
                    </p>

                    {/* OPÇÕES DE UPGRADE MODULAR */}
                    <div className="mt-6 space-y-4">
                        <h3 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">Planos Ilimitados</h3>
                        
                        {/* Delivery PRO - R$ 29,90 */}
                        <div className="border-2 border-gray-300 dark:border-gray-700 p-4 rounded-lg">
                            <h3 className="text-xl font-bold text-tema-primary mb-1">
                                🚀 Frevo DELIVERY PRO
                            </h3>
                            <p className="text-sm text-gray-700 dark:text-gray-300 mb-4">
                                Pedidos Ilimitados para Delivery. Ideal para Dark Kitchens.
                            </p>
                            <button 
                                onClick={handleUpgrade('DELIVERY')}
                                className="w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-lg"
                            >
                                Assinar por R$ {PRECO_DELIVERY}/mês
                            </button>
                        </div>
                        
                        {/* Salão PDV - R$ 35,90 */}
                        <div className="border-2 border-gray-300 dark:border-gray-700 p-4 rounded-lg">
                            <h3 className="text-xl font-bold text-tema-primary mb-1">
                                🍽️ Frevo SALÃO PDV
                            </h3>
                            <p className="text-sm text-gray-700 dark:text-gray-300 mb-4">
                                Mesas e Usuários Ilimitados. Solução completa para Salões e Bares.
                            </p>
                            <button 
                                onClick={handleUpgrade('SALAO')}
                                className="w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-lg"
                            >
                                Assinar por R$ {PRECO_SALAO}/mês
                            </button>
                        </div>

                        {/* Premium - R$ 49,90 Mensal / R$ 499,00 Anual */}
                        <div className="border-2 border-tema-success bg-tema-success/10 p-4 rounded-lg shadow-md">
                            <h3 className="text-xl font-bold text-tema-success mb-1">
                                👑 Frevo PREMIUM (Delivery + Salão)
                            </h3>
                            <p className="text-sm text-gray-700 dark:text-gray-300 mb-4">
                                Acesso total aos módulos Delivery e Mesas.
                            </p>
                            <div className="space-y-2">
                                <button 
                                    onClick={handleUpgrade('PREMIUM_MEN')}
                                    className="w-full py-3 rounded-lg font-bold text-white bg-tema-success hover:bg-opacity-90 transition-colors shadow-lg"
                                >
                                    Mensal: R$ {PRECO_PREMIUM_MENSAL}
                                </button>
                                <button 
                                    onClick={handleUpgrade('PREMIUM_ANU')}
                                    className="w-full py-3 rounded-lg font-bold text-tema-success border border-tema-success bg-white dark:bg-tema-surface-dark hover:bg-gray-100 transition-colors shadow-lg"
                                >
                                    Anual: R$ {PRECO_PREMIUM_ANUAL} (Economize 2 meses)
                                </button>
                            </div>
                        </div>


                        {/* Opção 4: Pay-per-Use */}
                        <div className="border border-gray-300 dark:border-gray-700 p-4 rounded-lg">
                            <h3 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-1">
                                💳 Opção 5: Pagar Pedidos Extras
                            </h3>
                            <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mb-4">
                                Adicione mais {pedidosParaComprar} pedidos por R$ {custoPorPacote}.
                            </p>
                            <button 
                                onClick={handlePayPerUse}
                                className="w-full py-3 rounded-lg font-bold text-gray-800 bg-yellow-400 hover:bg-yellow-500 transition-colors"
                            >
                                Pagar e Liberar {pedidosParaComprar} Pedidos
                            </button>
                        </div>

                    </div>
                </div>
                <div className="border-t dark:border-gray-700 p-4 flex justify-end">
                    <button onClick={onClose} className="text-sm text-gray-500 hover:text-tema-primary">
                        Cancelar e Voltar
                    </button>
                </div>
            </div>
        </div>
    );
};

export default UpgradeModal;