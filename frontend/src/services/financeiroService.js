// frontend/src/services/financeiroService.js
import apiClient from './apiClient';

export const comprarPacotePedidos = async () => {
    // Chama o endpoint de simulação de compra de pacote Pay-per-Use
    return apiClient.post('/api/financeiro/comprar-pedidos-extras', {});
};

export const upgradeParaDeliveryPro = async () => {
    // Chama o endpoint de simulação de upgrade de plano
    return apiClient.post('/api/financeiro/upgrade-pro', {});
};