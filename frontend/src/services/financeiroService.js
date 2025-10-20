// frontend/src/services/financeiroService.js
import apiClient from './apiClient';

// Pay-per-Use (compra única)
export const iniciarPagamentoPedidos = async () => {
    // Chama /api/financeiro/iniciar-pagamento, que agora gera o Stripe Checkout Session Mode.PAYMENT
    const response = await apiClient.post('/api/financeiro/iniciar-pagamento', {});
    return response.paymentUrl; // Retorna a URL do Stripe
};

// Upgrade Delivery PRO (assinatura recorrente)
export const iniciarUpgradeDeliveryMensal = async () => {
    // Chama /api/financeiro/upgrade/delivery-mensal, que gera o Stripe Checkout Session Mode.SUBSCRIPTION
    const response = await apiClient.post('/api/financeiro/upgrade/delivery-mensal', {}); 
    return response.upgradeUrl; 
};

export const iniciarUpgradeSalaoMensal = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/salao-mensal', {}); 
    return response.upgradeUrl; 
};

export const iniciarUpgradePremiumMensal = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/premium-mensal', {}); 
    return response.upgradeUrl; 
};

export const iniciarUpgradePremiumAnual = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/premium-anual', {});
    return response.upgradeUrl; 
};