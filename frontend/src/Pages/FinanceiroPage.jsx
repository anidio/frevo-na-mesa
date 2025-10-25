import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom'; // Adicionado Link
import { toast } from 'react-toastify';
import * as financeiroService from '../services/financeiroService';
import { useAuth } from '../contexts/AuthContext';
import UpgradeModal from '../components/UpgradeModal';

// --- ÍCONES --- (mantidos como antes)
const CardIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3v-6a3 3 0 00-3-3H6a3 3 0 00-3 3v6a3 3 0 003 3z" /></svg>
);
const CheckIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-tema-success" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" /></svg>
);
const AlertIcon = () => (
     <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-orange-500" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M8.257 3.099c.636-1.1 2.142-1.1 2.778 0l5.86 10.15a1.5 1.5 0 01-1.289 2.251H3.686a1.5 1.5 0 01-1.289-2.251l5.86-10.15zM10 13a1 1 0 110-2 1 1 0 010 2zm0-5a1 1 0 011 1v2a1 1 0 11-2 0V9a1 1 0 011-1z" clipRule="evenodd" />
     </svg>
);
// --- FIM ÍCONES ---

const PEDIDOS_POR_PACOTE = 10;
const LIMITE_PEDIDOS_GRATUITO_CONFIG = 5;

const FinanceiroPage = () => {
    const { userProfile, refreshProfile, loadingProfile } = useAuth();
    const [loadingManualUpdate, setLoadingManualUpdate] = useState(false);
    const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);
    const [loadingPortal, setLoadingPortal] = useState(false);

    const location = useLocation();
    const navigate = useNavigate();

    // useEffect para refresh pós-Stripe (ajustado para esperar userProfile)
    useEffect(() => {
        const queryParams = new URLSearchParams(location.search);
        const returnedFromStripeSuccess = queryParams.get('subscription') === 'success' || queryParams.get('payment') === 'success';
        const returnedFromStripeCancel = queryParams.get('subscription') === 'cancel' || queryParams.get('payment') === 'cancel';

        const checkAndRefresh = async () => {
            if (!loadingProfile && userProfile && (returnedFromStripeSuccess || returnedFromStripeCancel)) {
                console.log("FinanceiroPage: Detectado retorno do Stripe...");
                navigate('/admin/financeiro', { replace: true });

                if (returnedFromStripeSuccess) {
                    setLoadingManualUpdate(true);
                    toast.info("Processando pagamento/assinatura...");
                    await new Promise(resolve => setTimeout(resolve, 3000)); // Espera 3s pelo webhook
                    try {
                        await refreshProfile();
                        console.log("FinanceiroPage: refreshProfile concluído após retorno do Stripe.");
                        toast.success("Plano atualizado com sucesso!");
                    } catch (refreshError) {
                        console.error("FinanceiroPage: Erro no refreshProfile pós-Stripe:", refreshError);
                        toast.error("Pagamento/assinatura OK, mas erro ao atualizar dados locais. Tente atualizar manualmente.");
                    } finally {
                        setLoadingManualUpdate(false);
                    }
                } else if (returnedFromStripeCancel) {
                     toast.warn("Pagamento ou assinatura cancelada.");
                }
            }
        };
        checkAndRefresh();
    }, [loadingProfile, userProfile, location.search, refreshProfile, navigate]);

    // handlePayPerUse (sem alterações)
    const handlePayPerUse = async () => {
        setLoadingManualUpdate(true);
        try {
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos();
            toast.info("Redirecionando para pagamento... Após concluir, retorne e atualize os dados.");
            window.open(paymentUrl, '_blank', 'noopener,noreferrer');
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento.");
        } finally {
            setLoadingManualUpdate(false);
        }
    };

    // handleAtualizarDados (sem alterações)
     const handleAtualizarDados = async () => {
         console.log("FinanceiroPage: Botão 'Atualizar Dados' clicado.");
         setLoadingManualUpdate(true);
         try {
             await refreshProfile();
             console.log("FinanceiroPage: refreshProfile concluído via botão.");
             toast.info("Dados do plano atualizados!");
         } catch (error) {
              console.error("FinanceiroPage: Erro em handleAtualizarDados:", error);
              toast.error("Erro ao atualizar dados do plano.");
         } finally {
              setLoadingManualUpdate(false);
         }
     }

     // handleAbrirPortal (sem alterações)
    const handleAbrirPortal = async () => {
        setLoadingPortal(true);
        try {
            const portalUrl = await financeiroService.abrirPortalCliente();
            window.location.href = portalUrl;
        } catch (error) {
            toast.error(error.message || "Erro ao abrir o portal do cliente.");
            setLoadingPortal(false);
        }
    };

    // --- Renderização ---
    if (loadingProfile) return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando informações do usuário...</div>;
    if (!userProfile) return <div className="p-8 text-center text-red-500">Erro: Perfil do usuário não carregado. Faça o login novamente.</div>;

    // Desestruturação e Lógica de Exibição
    const {
        plano: planoAtualNomeBase = 'GRATUITO',
        limiteMesas: limiteMesasConfig = 10,
        limiteUsuarios: limiteUsuariosConfig = 4,
        pedidosMesAtual = 0,
        isLegacyFree = false,
        isBetaTester = false,
        deliveryPro = false,
        salaoPro = false,
        stripeSubscriptionId // <<< Essencial para a condição
    } = userProfile;

    const aplicaLimitePedidos = !deliveryPro && !isLegacyFree && !isBetaTester && planoAtualNomeBase === 'GRATUITO';
    const limiteAtingido = aplicaLimitePedidos && (pedidosMesAtual >= LIMITE_PEDIDOS_GRATUITO_CONFIG);
    const pedidosCompensados = aplicaLimitePedidos ? Math.max(0, LIMITE_PEDIDOS_GRATUITO_CONFIG - pedidosMesAtual) : 0;

    let planoExibido = planoAtualNomeBase;
    if (planoExibido === 'PREMIUM') planoExibido = 'PREMIUM (Salão + Delivery)';
    else if (planoExibido === 'DELIVERY_PRO') planoExibido = 'DELIVERY PRO';
    else if (planoExibido === 'SALÃO_PDV') planoExibido = 'SALÃO PDV';
    else if (planoExibido === 'GRATUITO') planoExibido = 'Frevo GO! (Gratuito)';

    const textoStatusSalao = salaoPro ? "ATIVO" : "Limitado";
    const textoLimiteMesas = salaoPro ? "Ilimitado" : `${limiteMesasConfig}`;
    const textoLimiteUsuarios = salaoPro ? "Ilimitado" : `${limiteUsuariosConfig}`;

    const textoStatusDelivery = deliveryPro ? "ATIVO" : "Limitado";
    const textoLimitePedidos = deliveryPro ? "Ilimitado" : `${LIMITE_PEDIDOS_GRATUITO_CONFIG} / mês`;

    const corPlano = deliveryPro || salaoPro ? 'text-green-600 dark:text-green-400' : 'text-orange-500 dark:text-orange-400';

    // *** CONDIÇÃO CENTRAL ***
    // Verifica se há uma assinatura ativa (não é gratuito E tem um ID de assinatura vindo do userProfile)
    const temAssinaturaAtiva = planoAtualNomeBase !== 'GRATUITO' && !!stripeSubscriptionId;

    // --- Determina a AÇÃO e o TEXTO do botão azul principal ---
    const acaoBotaoPrincipal = temAssinaturaAtiva ? handleAbrirPortal : () => setIsUpgradeModalOpen(true);
    const textoBotaoPrincipal = temAssinaturaAtiva ? 'Gerenciar Assinatura / Alterar Plano' : 'Ver Planos PRO e Assinar';
    const loadingBotaoPrincipal = temAssinaturaAtiva ? loadingPortal : false; // Loading só se for para o portal


    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-8">
                {/* Header da Página */}
                <div className="flex justify-between items-center flex-wrap gap-4">
                    <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark flex items-center gap-3">
                        <CardIcon /> Gerenciamento Financeiro
                    </h1>
                    <button
                        onClick={handleAtualizarDados}
                        disabled={loadingManualUpdate || loadingProfile || loadingPortal}
                        className={`bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors ${ (loadingManualUpdate || loadingProfile || loadingPortal) ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                        { loadingManualUpdate ? 'Atualizando...' : 'Atualizar Dados'}
                    </button>
                </div>

                {/* Card Principal: Status do Plano */}
                <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-4">
                    <h2 className={`text-2xl font-extrabold ${corPlano}`}>
                        Seu Plano Atual: {planoExibido}
                        {isLegacyFree && ' (Cliente Piloto)'}
                        {isBetaTester && ' (Beta Tester)'}
                    </h2>
                     <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">
                          {deliveryPro && salaoPro ? "Acesso completo a todos os módulos." :
                          deliveryPro ? "Módulo Delivery ilimitado ativo." :
                          salaoPro ? "Módulo Salão ilimitado ativo." :
                          "Plano gratuito com limites de uso."}
                     </p>
                </div>

                 {/* Card Detalhado: Detalhes e Limites do Plano */}
                <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-6">
                     <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark border-b dark:border-gray-700 pb-2">
                          Detalhes e Limites do Plano
                     </h2>
                     {/* Seção Salão */}
                     <div className={`p-4 rounded-lg border ${salaoPro ? 'border-green-200 bg-green-50 dark:border-green-700 dark:bg-green-900/20' : 'border-red-200 bg-red-50 dark:border-red-700 dark:bg-red-900/20'}`}>
                         {/* ... (conteúdo interno sem alterações) ... */}
                         <div className="flex justify-between items-center mb-2">
                             <h3 className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">Gestão de Salão (Mesas)</h3>
                              <span className={`font-bold text-sm px-2 py-0.5 rounded ${salaoPro ? 'bg-green-200 text-green-800 dark:bg-green-700 dark:text-green-100' : 'bg-red-200 text-red-800 dark:bg-red-700 dark:text-red-100'}`}>
                                   {textoStatusSalao}
                              </span>
                         </div>
                         <div className="text-sm space-y-1 text-tema-text-muted dark:text-tema-text-muted-dark">
                              <p>Limite de Mesas: <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimiteMesas}</span></p>
                              <p>Limite de Usuários (Garçom/Caixa): <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimiteUsuarios}</span></p>
                         </div>
                    </div>
                     {/* Seção Delivery */}
                     <div className={`p-4 rounded-lg border ${deliveryPro ? 'border-green-200 bg-green-50 dark:border-green-700 dark:bg-green-900/20' : 'border-red-200 bg-red-50 dark:border-red-700 dark:bg-red-900/20'}`}>
                          {/* ... (conteúdo interno sem alterações) ... */}
                          <div className="flex justify-between items-center mb-2">
                              <h3 className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">Delivery Próprio</h3>
                              <span className={`font-bold text-sm px-2 py-0.5 rounded ${deliveryPro ? 'bg-green-200 text-green-800 dark:bg-green-700 dark:text-green-100' : 'bg-red-200 text-red-800 dark:bg-red-700 dark:text-red-100'}`}>
                                  {textoStatusDelivery}
                              </span>
                         </div>
                          <div className="text-sm space-y-1 text-tema-text-muted dark:text-tema-text-muted-dark">
                              <p>Limite de Pedidos: <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimitePedidos}</span></p>
                              {aplicaLimitePedidos && (
                                  <p>Uso Atual (Este Mês):
                                      <span className={`font-semibold ml-1 ${limiteAtingido ? 'text-red-600 dark:text-red-400' : 'text-tema-text dark:text-tema-text-dark'}`}>
                                          {pedidosMesAtual} / {LIMITE_PEDIDOS_GRATUITO_CONFIG}
                                      </span>
                                      {limiteAtingido && <span className="text-red-600 dark:text-red-400 ml-1">(Limite Atingido!)</span>}
                                  </p>
                              )}
                         </div>
                         {planoAtualNomeBase === 'GRATUITO' && pedidosCompensados > 0 && !deliveryPro && (
                              <div className="mt-3 p-2 bg-yellow-100 border border-yellow-300 text-yellow-800 rounded text-xs font-medium dark:bg-yellow-900/30 dark:border-yellow-700 dark:text-yellow-200">
                                   <CheckIcon className="inline h-4 w-4 mr-1 text-yellow-600 dark:text-yellow-400" />
                                   {pedidosCompensados} pedidos extras disponíveis este mês via Pay-per-Use.
                              </div>
                          )}
                    </div>
                </div> {/* Fim do Card Detalhado */}


                {/* Card de Ações: Gerenciar Plano / Comprar Extras */}
                {!isLegacyFree && !isBetaTester && (
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-4">
                        <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark">
                             Gerenciar Plano / Comprar Extras
                        </h2>

                        {/* --- BOTÃO AZUL PRINCIPAL (CONDICIONAL) --- */}
                         <button
                             onClick={acaoBotaoPrincipal} // <<< Ação dinâmica
                             disabled={loadingManualUpdate || loadingProfile || loadingBotaoPrincipal}
                             className={`w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-md ${(loadingManualUpdate || loadingProfile || loadingBotaoPrincipal) ? 'opacity-50 cursor-not-allowed' : ''}`}
                         >
                            {loadingBotaoPrincipal ? 'Abrindo portal...' : textoBotaoPrincipal} {/* <<< Texto dinâmico */}
                         </button>

                         {/* --- BOTÃO "Gerenciar Assinatura e Pagamento" REMOVIDO --- */}


                         {/* Botão Pay-Per-Use (Só aparece se for gratuito e não tiver delivery pro) */}
                         {planoAtualNomeBase === 'GRATUITO' && !deliveryPro && (
                              <button
                                  onClick={handlePayPerUse}
                                  disabled={loadingManualUpdate || loadingProfile || loadingPortal}
                                  className={`w-full py-3 rounded-lg font-bold text-gray-800 bg-yellow-400 hover:bg-yellow-500 transition-colors ${(loadingManualUpdate || loadingProfile || loadingPortal) ? 'opacity-50 cursor-not-allowed' : ''}`}
                              >
                                  Comprar +{PEDIDOS_POR_PACOTE} Pedidos Extras (Pay-per-Use)
                              </button>
                         )}
                    </div>
                )}

            </div> {/* Fim do container principal */}

            {/* Modal de Upgrade (Abre apenas se não tiver assinatura ativa) */}
            {isUpgradeModalOpen && (
                 <UpgradeModal
                     onClose={() => setIsUpgradeModalOpen(false)}
                     limiteAtual={LIMITE_PEDIDOS_GRATUITO_CONFIG}
                     refreshProfile={refreshProfile}
                 />
             )}
        </>
    );
};

export default FinanceiroPage;