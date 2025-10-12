import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import * as financeiroService from '../services/financeiroService'; 
import { useAuth } from '../contexts/AuthContext';

// Adicione a prop onPedidoAceito
const UpgradeModal = ({ onClose, limiteAtual, refreshProfile, onPedidoAceito }) => {
    const navigate = useNavigate();
    // Dados para o Pay-per-Use
    const pedidosParaComprar = 10;
    const custoPorPedido = 1.49;
    const custoPorPacote = (pedidosParaComprar * custoPorPedido).toFixed(2);

    // LÓGICA ATUALIZADA: Ação de Upgrade (AGORA REDIRECIONA PARA O MP)
    const handleUpgradePro = async () => {
        try {
            // 1. Inicia o pagamento do Plano PRO e obtém o URL do MP
            const upgradeUrl = await financeiroService.iniciarUpgradePro(); 
            
            toast.info("Redirecionando para o Checkout de Assinatura...");
            onClose(); 
            
            // 2. Redireciona o usuário para o Checkout Pro (Sandbox)
            window.open(upgradeUrl, '_blank');
            
            // Note: A compensação de limite será tratada de forma ASSÍNCRONA pelo WEBHOOK do MP.

        } catch (error) {
            toast.error(error.message || "Erro ao iniciar o Upgrade. Verifique as credenciais.");
        }
    };

    // LÓGICA ATUALIZADA: Ação de Pay-per-Use (AGORA REDIRECIONA PARA O MP)
    const handlePayPerUse = async () => {
        try {
            // 1. Inicia o pagamento no Backend e obtém o URL do MP
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos(); 
            
            toast.info("Redirecionando para o Checkout Seguro do Mercado Pago...");
            onClose(); 
            
            // 2. Redireciona o usuário para o Checkout Pro (Sandbox)
            window.open(paymentUrl, '_blank');
            
            // NOTE: A compensação de limite será tratada de forma ASSÍNCRONA pelo WEBHOOK do MP.
            
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento. Verifique as credenciais do Mercado Pago.");
        }
    };


    return (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-40 p-4" onClick={onClose}>
            <div className="bg-white dark:bg-tema-surface-dark rounded-xl shadow-2xl w-full max-w-lg" onClick={e => e.stopPropagation()}>
                <div className="p-6 md:p-8 text-center">
                    <h2 className="text-3xl font-extrabold text-tema-accent dark:text-red-400 mb-2">
                        Limite de Pedidos Atingido!
                    </h2>
                    <p className="text-tema-text dark:text-tema-text-dark mb-4">
                        Seu plano **Frevo GO!** atingiu o limite de {limiteAtual} pedidos neste mês. Escolha como prosseguir:
                    </p>

                    {/* OPÇÕES DE UPGRADE */}
                    <div className="mt-6 space-y-4">

                        {/* Opção 1: Assinatura PRO (Incentivo Principal) */}
                        <div className="border-2 border-tema-primary bg-tema-primary/10 p-4 rounded-lg shadow-md">
                            <h3 className="text-xl font-bold text-tema-primary mb-1">
                                🚀 Opção 1: Plano Delivery PRO (Melhor Opção!)
                            </h3>
                            <p className="text-sm text-gray-700 dark:text-gray-300 mb-4">
                                Vendas **Ilimitadas** todos os meses e **Remoção da Marca Frevo na Mesa**.
                            </p>
                            <button 
                                onClick={handleUpgradePro}
                                className="w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-lg"
                            >
                                Assinar Agora (R$ 69,90/mês)
                            </button>
                        </div>

                        {/* Opção 2: Pay-per-Use (Taxa Transacional) */}
                        <div className="border border-gray-300 dark:border-gray-700 p-4 rounded-lg">
                            <h3 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-1">
                                💰 Opção 2: Pagar Pedidos Extras
                            </h3>
                            <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mb-4">
                                Adicione mais {pedidosParaComprar} pedidos por R$ {custoPorPacote} (R$ {custoPorPedido}/pedido).
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