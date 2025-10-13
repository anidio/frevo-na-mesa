// frontend/src/services/financeiroService.js
import apiClient from './apiClient';

// MÉTODO CRÍTICO: Inicia o pagamento no Backend e retorna o URL do Mercado Pago
export const iniciarPagamentoPedidos = async () => {
    // Chama o endpoint que gera a Preferência de Pagamento via SDK do Mercado Pago
    const response = await apiClient.post('/api/financeiro/iniciar-pagamento', {});
    // O backend retorna { paymentUrl: "..." }
    return response.paymentUrl;
};

export const iniciarUpgradeDeliveryMensal = async () => {
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