import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import * as financeiroService from '../services/financeiroService';
import { useAuth } from '../contexts/AuthContext';
import UpgradeModal from '../components/UpgradeModal';

// --- ÍCONES ---
const CardIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3v-6a3 3 0 00-3-3H6a3 3 0 00-3 3v6a3 3 0 003 3z" /></svg>
);
const CheckIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-tema-success" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" /></svg>
);
const AlertIcon = () => ( // Ícone para limites
     <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-orange-500" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M8.257 3.099c.636-1.1 2.142-1.1 2.778 0l5.86 10.15a1.5 1.5 0 01-1.289 2.251H3.686a1.5 1.5 0 01-1.289-2.251l5.86-10.15zM10 13a1 1 0 110-2 1 1 0 010 2zm0-5a1 1 0 011 1v2a1 1 0 11-2 0V9a1 1 0 011-1z" clipRule="evenodd" />
     </svg>
);
// --- FIM ÍCONES ---

// Constantes
const PEDIDOS_POR_PACOTE = 10;
const LIMITE_PEDIDOS_GRATUITO_CONFIG = 5; // Limite base do plano gratuito

const FinanceiroPage = () => {
    const { userProfile, refreshProfile, loadingProfile } = useAuth();
    // LOG: Veja o objeto userProfile que este componente está recebendo do contexto
    console.log(">>> FinanceiroPage - userProfile RECEBIDO do Contexto:", JSON.stringify(userProfile || {}, null, 2));

    const [loadingManualUpdate, setLoadingManualUpdate] = useState(false);
    const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);

    const location = useLocation();
    const navigate = useNavigate();

    // Efeito para refresh automático após retorno do Stripe
    useEffect(() => {
        const queryParams = new URLSearchParams(location.search);
        const returnedFromStripeSuccess = queryParams.get('subscription') === 'success' || queryParams.get('payment') === 'success';
        const returnedFromStripeCancel = queryParams.get('subscription') === 'cancel' || queryParams.get('payment') === 'cancel';

        const checkAndRefresh = async () => {
            if (!loadingProfile && (returnedFromStripeSuccess || returnedFromStripeCancel)) {
                console.log("FinanceiroPage: Detectado retorno do Stripe...");
                navigate('/admin/financeiro', { replace: true }); // Limpa URL

                if (returnedFromStripeSuccess) {
                    setLoadingManualUpdate(true);
                    try {
                        await refreshProfile();
                        console.log("FinanceiroPage: refreshProfile concluído após retorno do Stripe.");
                        toast.success("Plano atualizado com sucesso!");
                    } catch (refreshError) {
                        console.error("FinanceiroPage: Erro no refreshProfile pós-Stripe:", refreshError);
                        toast.error("Pagamento ok, mas erro ao atualizar dados. Tente manualmente.");
                    } finally {
                        setLoadingManualUpdate(false);
                    }
                } else if (returnedFromStripeCancel) {
                     toast.warn("Pagamento ou assinatura cancelada.");
                }
            }
        };
        checkAndRefresh();
    }, [loadingProfile, location.search, refreshProfile, navigate]);

    // handlePayPerUse
    const handlePayPerUse = async () => {
        setLoadingManualUpdate(true);
        try {
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos();
            toast.info("Redirecionando... Após o pagamento, retorne e atualize os dados.");
            window.open(paymentUrl, '_blank');
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento.");
        } finally {
            setLoadingManualUpdate(false);
        }
    };

    // handleAtualizarDados
     const handleAtualizarDados = async () => {
         console.log("FinanceiroPage: Botão 'Atualizar Dados' clicado.");
         setLoadingManualUpdate(true);
         try {
             await refreshProfile();
             console.log("FinanceiroPage: refreshProfile concluído via botão.");
             toast.info("Dados atualizados com sucesso!");
         } catch (error) {
              console.error("FinanceiroPage: Erro em handleAtualizarDados:", error);
              toast.error("Erro ao atualizar dados.");
         } finally {
              setLoadingManualUpdate(false);
         }
     }

    // --- Renderização ---
    if (loadingProfile) return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</div>;
    if (!userProfile) return <div className="p-8 text-center text-red-500">Erro: Perfil do usuário não carregado. Faça o login novamente.</div>;

    // **CORREÇÃO CRÍTICA AQUI:** Verifique pela propriedade correta 'deliveryPro'
    if (typeof userProfile.deliveryPro === 'undefined') {
        console.warn(">>> FinanceiroPage - userProfile AINDA não tem 'deliveryPro' definido. Aguardando re-renderização...");
        return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Atualizando dados do plano...</div>;
    }

    // --- Extração e Lógica de Exibição ---
    // Agora desestruturamos usando os nomes corretos da API/DTO
    const {
        plano: planoAtualNomeBase,
        limiteMesas: limiteMesasConfig,
        limiteUsuarios: limiteUsuariosConfig,
        pedidosMesAtual,
        isLegacyFree,
        deliveryPro, // << NOME CORRETO
        salaoPro,    // << NOME CORRETO
        isBetaTester
    } = userProfile;

    // DEBUG LOG CRÍTICO: Verificando o valor correto
    console.log(">>> FinanceiroPage - Valor deliveryPro ANTES do RENDER (Após checagem):", deliveryPro);

    // Lógica recalculada usando as variáveis corretas
    const aplicaLimitePedidos = !deliveryPro && !isLegacyFree && !isBetaTester && planoAtualNomeBase === 'GRATUITO';
    const limiteAtingido = aplicaLimitePedidos && (pedidosMesAtual >= LIMITE_PEDIDOS_GRATUITO_CONFIG);
    const pedidosCompensados = aplicaLimitePedidos ? Math.max(0, LIMITE_PEDIDOS_GRATUITO_CONFIG - pedidosMesAtual) : 0;

    let planoExibido = planoAtualNomeBase;
    if (planoExibido === 'PREMIUM') planoExibido = 'PREMIUM (Salão + Delivery)';
    else if (planoExibido === 'DELIVERY_PRO') planoExibido = 'DELIVERY PRO';
    else if (planoExibido === 'SALÃO_PDV') planoExibido = 'SALÃO PDV';
    else if (planoExibido === 'GRATUITO') planoExibido = 'Frevo GO! (Gratuito)';

    // Usando as variáveis corretas desestruturadas
    const textoStatusSalao = salaoPro ? "ATIVO" : "Limitado";
    const textoLimiteMesas = salaoPro ? "Ilimitado" : `${limiteMesasConfig}`;
    const textoLimiteUsuarios = salaoPro ? "Ilimitado" : `${limiteUsuariosConfig}`;

    const textoStatusDelivery = deliveryPro ? "ATIVO" : "Limitado";
    const textoLimitePedidos = deliveryPro ? "Ilimitado" : `${LIMITE_PEDIDOS_GRATUITO_CONFIG} / mês`;

    const corPlano = deliveryPro || salaoPro ? 'text-green-600 dark:text-green-400' : 'text-orange-500 dark:text-orange-400';

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
                        disabled={loadingManualUpdate || loadingProfile}
                        className={`bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors ${ (loadingManualUpdate || loadingProfile) ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                        { (loadingManualUpdate || loadingProfile) ? 'Atualizando...' : 'Atualizar Dados'}
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
                          {deliveryPro && salaoPro ? "Acesso completo a todos os módulos." : // Usa as variáveis corretas
                          deliveryPro ? "Módulo Delivery ilimitado ativo." : // Usa as variáveis corretas
                          salaoPro ? "Módulo Salão ilimitado ativo." : // Usa as variáveis corretas
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
                         <div className="flex justify-between items-center mb-2">
                             <h3 className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">Gestão de Salão (Mesas)</h3>
                              <span className={`font-bold text-sm px-2 py-0.5 rounded ${salaoPro ? 'bg-green-200 text-green-800' : 'bg-red-200 text-red-800'}`}>
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
                          <div className="flex justify-between items-center mb-2">
                              <h3 className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">Delivery Próprio</h3>
                              <span className={`font-bold text-sm px-2 py-0.5 rounded ${deliveryPro ? 'bg-green-200 text-green-800' : 'bg-red-200 text-red-800'}`}>
                                  {textoStatusDelivery}
                              </span>
                         </div>
                          <div className="text-sm space-y-1 text-tema-text-muted dark:text-tema-text-muted-dark">
                              <p>Limite de Pedidos: <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimitePedidos}</span></p>
                              {/* Mostra Uso Atual APENAS se o limite se aplica */}
                              {aplicaLimitePedidos && (
                                  <p>Uso Atual (Este Mês):
                                      <span className={`font-semibold ml-1 ${limiteAtingido ? 'text-red-600 dark:text-red-400' : 'text-tema-text dark:text-tema-text-dark'}`}>
                                          {pedidosMesAtual} / {LIMITE_PEDIDOS_GRATUITO_CONFIG}
                                      </span>
                                      {limiteAtingido && <span className="text-red-600 dark:text-red-400 ml-1">(Limite Atingido!)</span>}
                                  </p>
                              )}
                         </div>
                          {/* Aviso sobre pedidos compensados (Pay-per-Use) */}
                         {planoAtualNomeBase === 'GRATUITO' && pedidosCompensados > 0 && !deliveryPro && (
                              <div className="mt-3 p-2 bg-yellow-100 border border-yellow-300 text-yellow-800 rounded text-xs font-medium">
                                   <CheckIcon className="inline h-4 w-4 mr-1 text-yellow-600" />
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
                         <button
                             onClick={() => setIsUpgradeModalOpen(true)}
                             disabled={loadingManualUpdate || loadingProfile}
                             className={`w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-md ${(loadingManualUpdate || loadingProfile) ? 'opacity-50 cursor-not-allowed' : ''}`}
                         >
                             {deliveryPro || salaoPro ? 'Ver Opções / Alterar Plano' : 'Ver Planos PRO e Assinar'}
                         </button>
                         {/* Botão Pay-Per-Use (só aparece se Delivery NÃO for PRO) */}
                         {!deliveryPro && (
                              <button
                                  onClick={handlePayPerUse}
                                  disabled={loadingManualUpdate || loadingProfile}
                                  className={`w-full py-3 rounded-lg font-bold text-gray-800 bg-yellow-400 hover:bg-yellow-500 transition-colors ${(loadingManualUpdate || loadingProfile) ? 'opacity-50 cursor-not-allowed' : ''}`}
                              >
                                  Comprar +{PEDIDOS_POR_PACOTE} Pedidos Extras (Pay-per-Use)
                              </button>
                         )}
                    </div>
                )}

            </div> {/* Fim do container principal */}

            {/* Modal de Upgrade */}
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