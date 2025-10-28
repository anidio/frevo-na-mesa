// frontend/src/services/financeiroService.js
import apiClient from './apiClient';

// Pay-per-Use (compra única)
export const iniciarPagamentoPedidos = async () => {
    // Chama /api/financeiro/iniciar-pagamento
    const response = await apiClient.post('/api/financeiro/iniciar-pagamento', {});
    return response.paymentUrl; // Retorna a URL do Stripe Checkout
};

// Upgrade Delivery PRO (assinatura recorrente via Checkout)
export const iniciarUpgradeDeliveryMensal = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/delivery-mensal', {});
    return response.upgradeUrl; // Retorna a URL do Stripe Checkout
};

// Upgrade Salão PDV (assinatura recorrente via Checkout)
export const iniciarUpgradeSalaoMensal = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/salao-mensal', {});
    return response.upgradeUrl; // Retorna a URL do Stripe Checkout
};

// Upgrade Premium Mensal (assinatura recorrente via Checkout)
export const iniciarUpgradePremiumMensal = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/premium-mensal', {});
    return response.upgradeUrl; // Retorna a URL do Stripe Checkout
};

// Upgrade Premium Anual (assinatura recorrente via Checkout)
export const iniciarUpgradePremiumAnual = async () => {
    const response = await apiClient.post('/api/financeiro/upgrade/premium-anual', {});
    return response.upgradeUrl; // Retorna a URL do Stripe Checkout
};

// Função para obter a URL do Portal do Cliente Stripe
export const abrirPortalCliente = async () => {
    // Chama o novo endpoint no backend
    const response = await apiClient.post('/api/financeiro/portal-session', {});
    return response.portalUrl; // Retorna a URL do portal vinda do backend
};

/**
 * Busca no backend a URL segura para iniciar o processo de
 * onboarding/conexão da conta Stripe Express do restaurante.
 */
export const getConnectOnboardingLink = async () => {
    // Chama o novo endpoint no backend
    const response = await apiClient.post('/api/financeiro/connect/onboard', {});
    // O backend retorna a URL gerada pelo Stripe
    return response.onboardingUrl;
};