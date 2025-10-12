// frontend/src/services/financeiroService.js
import apiClient from './apiClient';

// MÉTODO CRÍTICO: Inicia o pagamento no Backend e retorna o URL do Mercado Pago
export const iniciarPagamentoPedidos = async () => {
    // Chama o endpoint que gera a Preferência de Pagamento via SDK do Mercado Pago
    const response = await apiClient.post('/api/financeiro/iniciar-pagamento', {});
    // O backend retorna { paymentUrl: "..." }
    return response.paymentUrl;
};

// NOVO: Inicia o pagamento do Plano PRO e retorna a URL
export const iniciarUpgradePro = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade-pro', {});
    // O backend retorna { upgradeUrl: "..." }
    return response.upgradeUrl; 
};