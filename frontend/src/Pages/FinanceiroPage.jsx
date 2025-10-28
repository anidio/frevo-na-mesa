// frontend/src/Pages/FinanceiroPage.jsx
import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import * as financeiroService from '../services/financeiroService'; // Garanta a importação
import { useAuth } from '../contexts/AuthContext';
import UpgradeModal from '../components/UpgradeModal';

// --- ÍCONES ---
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
// Adicionar ícone do Stripe (exemplo simples)
const StripeIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-[#635BFF]" viewBox="0 0 24 24" fill="currentColor">
      <path d="M16.71.241l-3.32 15.68h3.91L20.62.241h-3.91zM9.75.241L6.44 15.92h3.91L13.67.241H9.75zM3.48.241L0 18.08h3.91l2.1-10.45 2.13 10.45h3.91l3.47-17.84H8.79l-2.11 10.45L4.54.241H3.48z"/>
    </svg>
);
// --- FIM ÍCONES ---

const PEDIDOS_POR_PACOTE = 10;
const LIMITE_PEDIDOS_GRATUITO_CONFIG = 5; // Pode ser ajustado ou vir do backend

const FinanceiroPage = () => {
    const { userProfile, refreshProfile, loadingProfile } = useAuth();
    const [loadingManualUpdate, setLoadingManualUpdate] = useState(false);
    const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);
    const [loadingPortal, setLoadingPortal] = useState(false);
    const [loadingConnect, setLoadingConnect] = useState(false); // <-- NOVO ESTADO

    const location = useLocation();
    const navigate = useNavigate();

    // useEffect para refresh pós-Stripe (Connect ou Assinatura/Pagamento)
    useEffect(() => {
        const queryParams = new URLSearchParams(location.search);
        const returnedFromSubscriptionSuccess = queryParams.get('subscription') === 'success';
        const returnedFromPaymentSuccess = queryParams.get('payment') === 'success';
        const returnedFromConnectReturn = queryParams.get('connect_return') === 'true'; // Retorno do onboarding
        const returnedFromCancel = queryParams.get('subscription') === 'cancel' || queryParams.get('payment') === 'cancel';
        const returnedFromConnectRefresh = queryParams.get('connect_refresh') === 'true'; // Retorno se link expirou

        const checkAndRefresh = async () => {
            // Roda apenas se o perfil estiver carregado e houver algum parâmetro de retorno do Stripe
            if (!loadingProfile && userProfile && (returnedFromSubscriptionSuccess || returnedFromPaymentSuccess || returnedFromConnectReturn || returnedFromCancel || returnedFromConnectRefresh)) {
                console.log("FinanceiroPage: Detectado retorno do Stripe...");
                // Limpa os parâmetros da URL para evitar refreshs repetidos
                navigate('/admin/financeiro', { replace: true });

                if (returnedFromSubscriptionSuccess || returnedFromPaymentSuccess || returnedFromConnectReturn) {
                    setLoadingManualUpdate(true); // Usa o loading geral
                    const successMessage = returnedFromConnectReturn
                        ? "Conexão com Stripe finalizada! Verificando status..."
                        : "Processando pagamento/assinatura...";
                    toast.info(successMessage);
                    // Espera um pouco para o webhook do Stripe (se aplicável) ser processado pelo backend
                    await new Promise(resolve => setTimeout(resolve, 4000));
                    try {
                        await refreshProfile(); // Puxa os dados atualizados (plano, stripeConnectAccountId)
                        console.log("FinanceiroPage: refreshProfile concluído após retorno do Stripe.");
                        const finalMessage = returnedFromConnectReturn
                            ? "Status da conexão Stripe atualizado!"
                            : "Plano atualizado com sucesso!";
                        toast.success(finalMessage);
                    } catch (refreshError) {
                        console.error("FinanceiroPage: Erro no refreshProfile pós-Stripe:", refreshError);
                        toast.error("Retorno do Stripe OK, mas erro ao atualizar dados locais. Tente atualizar manualmente.");
                    } finally {
                        setLoadingManualUpdate(false);
                    }
                } else if (returnedFromCancel) {
                     toast.warn("Operação com Stripe cancelada.");
                } else if (returnedFromConnectRefresh) {
                     toast.warn("Link de conexão expirado. Tente conectar novamente.");
                }
            }
        };
        checkAndRefresh();
    // Adiciona `Maps` às dependências
    }, [loadingProfile, userProfile, location.search, refreshProfile, navigate]);

    // handlePayPerUse (sem alterações)
    const handlePayPerUse = async () => {
        setLoadingManualUpdate(true);
        try {
            const paymentUrl = await financeiroService.iniciarPagamentoPedidos();
            toast.info("Redirecionando para pagamento... Após concluir, retorne e atualize os dados.");
            window.location.href = paymentUrl; // Redireciona a aba atual
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar o pagamento.");
             setLoadingManualUpdate(false); // Garante que o loading para em caso de erro
        }
    };

    // handleAtualizarDados (sem alterações)
     const handleAtualizarDados = async () => {
         console.log("FinanceiroPage: Botão 'Atualizar Dados' clicado.");
         setLoadingManualUpdate(true);
         try {
             await refreshProfile();
             console.log("FinanceiroPage: refreshProfile concluído via botão.");
             toast.info("Dados do plano e conexão atualizados!");
         } catch (error) {
              console.error("FinanceiroPage: Erro em handleAtualizarDados:", error);
              toast.error("Erro ao atualizar dados.");
         } finally {
              setLoadingManualUpdate(false);
         }
     }

     // handleAbrirPortal (sem alterações)
    const handleAbrirPortal = async () => {
        setLoadingPortal(true);
        try {
            const portalUrl = await financeiroService.abrirPortalCliente();
            window.location.href = portalUrl; // Redireciona a aba atual
        } catch (error) {
            toast.error(error.message || "Erro ao abrir o portal do cliente.");
            setLoadingPortal(false); // Para o loading se der erro
        }
        // Não para o loading em caso de sucesso, pois a página será redirecionada
    };

    // --- >> INÍCIO: NOVO HANDLER PARA CONECTAR STRIPE << ---
    const handleConnectStripe = async () => {
        setLoadingConnect(true); // Ativa o loading específico do botão Connect
        try {
            // Chama a nova função no service que busca a URL no backend
            const onboardingUrl = await financeiroService.getConnectOnboardingLink();
            toast.info("Redirecionando para o Stripe para conectar sua conta...");
            window.location.href = onboardingUrl; // Redireciona o usuário para o onboarding
        } catch (error) {
             toast.error(error.message || "Erro ao iniciar conexão com Stripe.");
             setLoadingConnect(false); // Para o loading se der erro antes de redirecionar
        }
        // Não definimos setLoadingConnect(false) aqui em caso de sucesso, pois a página será redirecionada
    };
    // --- >> FIM: NOVO HANDLER PARA CONECTAR STRIPE << ---


    // --- Renderização ---
    if (loadingProfile) return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando informações...</div>;
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
        stripeSubscriptionId,
        stripeConnectAccountId // <-- Pegar o ID da conta conectada
    } = userProfile;

    const isStripeConnected = !!stripeConnectAccountId; // Flag para saber se está conectado

    const aplicaLimitePedidos = !deliveryPro && !isLegacyFree && !isBetaTester && planoAtualNomeBase === 'GRATUITO';
    // Usa o limitePedidosGratuito que veio do backend (ou o default)
    const limitePedidosConfig = userProfile.limitePedidosGratuito ?? LIMITE_PEDIDOS_GRATUITO_CONFIG;
    const limiteAtingido = aplicaLimitePedidos && (pedidosMesAtual >= limitePedidosConfig);
    const pedidosRestantes = aplicaLimitePedidos ? Math.max(0, limitePedidosConfig - pedidosAtuais) : Infinity;

    let planoExibido = planoAtualNomeBase.replace('_', ' '); // Formatação básica
    if (planoAtualNomeBase === 'PREMIUM') planoExibido = 'PREMIUM (Salão + Delivery)';
    else if (planoAtualNomeBase === 'GRATUITO') planoExibido = 'Frevo GO! (Gratuito)';


    const textoStatusSalao = salaoPro ? "ATIVO" : "Limitado";
    const textoLimiteMesas = salaoPro ? "Ilimitado" : `${limiteMesasConfig}`;
    const textoLimiteUsuarios = salaoPro ? "Ilimitado" : `${limiteUsuariosConfig}`;

    const textoStatusDelivery = deliveryPro ? "ATIVO" : "Limitado";
    const textoLimitePedidos = deliveryPro ? "Ilimitado" : `${limitePedidosConfig} / mês`;

    const corPlano = deliveryPro || salaoPro ? 'text-green-600 dark:text-green-400' : 'text-orange-500 dark:text-orange-400';

    const temAssinaturaAtiva = planoAtualNomeBase !== 'GRATUITO' && !!stripeSubscriptionId;

    const acaoBotaoPrincipal = temAssinaturaAtiva ? handleAbrirPortal : () => setIsUpgradeModalOpen(true);
    const textoBotaoPrincipal = temAssinaturaAtiva ? 'Gerenciar Assinatura / Alterar Plano' : 'Ver Planos PRO e Assinar';
    const loadingBotaoPrincipal = temAssinaturaAtiva ? loadingPortal : false;


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
                        disabled={loadingManualUpdate || loadingProfile || loadingPortal || loadingConnect} // Desabilita se qualquer loading estiver ativo
                        className={`bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors ${ (loadingManualUpdate || loadingProfile || loadingPortal || loadingConnect) ? 'opacity-50 cursor-not-allowed' : ''}`}
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

                 {/* --- >> INÍCIO: NOVO CARD STRIPE CONNECT << --- */}
                 <div className={`bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-4 ${isStripeConnected ? 'border-l-4 border-green-500' : 'border-l-4 border-orange-500'}`}>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark flex items-center gap-2">
                        <StripeIcon /> Pagamentos Online (Pedidos Delivery)
                    </h2>
                    {isStripeConnected ? (
                        <>
                            <p className="text-tema-success dark:text-green-400 font-semibold">
                                <CheckIcon className="inline h-5 w-5 mr-1" /> Sua conta Stripe está conectada!
                            </p>
                            <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">
                                Os pagamentos online (Cartão/Pix) dos seus clientes de delivery serão direcionados para sua conta Stripe ({stripeConnectAccountId.substring(0, 10)}...).
                            </p>
                             {/* Botão para re-onboarding ou atualizar dados */}
                             <button
                                onClick={handleConnectStripe}
                                disabled={loadingConnect || loadingManualUpdate || loadingProfile || loadingPortal}
                                className={`w-full mt-2 py-2 rounded-lg font-semibold text-tema-primary border border-tema-primary hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors ${ (loadingConnect || loadingManualUpdate || loadingProfile || loadingPortal) ? 'opacity-50 cursor-not-allowed' : ''}`}
                             >
                                {loadingConnect ? 'Aguarde...' : 'Atualizar Dados / Re-conectar Stripe'}
                             </button>
                        </>
                    ) : (
                        <>
                            <p className="text-orange-600 dark:text-orange-400 font-semibold">
                                <AlertIcon className="inline h-5 w-5 mr-1" /> Conecte sua conta Stripe para receber pagamentos online.
                            </p>
                            <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">
                                Habilite PIX e Cartão de Crédito diretamente no seu cardápio digital. Os valores vão direto para sua conta Stripe, descontada a taxa da plataforma.
                            </p>
                            {/* Botão para iniciar o onboarding */}
                            <button
                                onClick={handleConnectStripe}
                                disabled={loadingConnect || loadingManualUpdate || loadingProfile || loadingPortal}
                                className={`w-full mt-2 py-3 rounded-lg font-bold text-white bg-blue-600 hover:bg-blue-700 transition-colors shadow-md ${ (loadingConnect || loadingManualUpdate || loadingProfile || loadingPortal) ? 'opacity-50 cursor-not-allowed' : ''}`}
                            >
                                {loadingConnect ? 'Aguarde...' : 'Conectar Conta Stripe'}
                            </button>
                        </>
                    )}
                </div>
                {/* --- >> FIM: NOVO CARD STRIPE CONNECT << --- */}


                 {/* Card Detalhado: Detalhes e Limites do Plano */}
                <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-6">
                     <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark border-b dark:border-gray-700 pb-2">
                          Detalhes e Limites do Plano (Plataforma)
                     </h2>
                     {/* Seção Salão */}
                     <div className={`p-4 rounded-lg border ${salaoPro ? 'border-green-200 bg-green-50 dark:border-green-700 dark:bg-green-900/20' : 'border-red-200 bg-red-50 dark:border-red-700 dark:bg-red-900/20'}`}>
                         <div className="flex justify-between items-center mb-2">
                             <h3 className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">Gestão de Salão (Mesas)</h3>
                              <span className={`font-bold text-sm px-2 py-0.5 rounded ${salaoPro ? 'bg-green-200 text-green-800 dark:bg-green-700 dark:text-green-100' : 'bg-red-200 text-red-800 dark:bg-red-700 dark:text-red-100'}`}>
                                   {textoStatusSalao}
                              </span>
                         </div>
                         <div className="text-sm space-y-1 text-tema-text-muted dark:text-tema-text-muted-dark">
                              <p>Limite de Mesas: <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimiteMesas}</span></p>
                              <p>Limite de Usuários: <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimiteUsuarios}</span></p>
                         </div>
                    </div>
                     {/* Seção Delivery */}
                     <div className={`p-4 rounded-lg border ${deliveryPro ? 'border-green-200 bg-green-50 dark:border-green-700 dark:bg-green-900/20' : 'border-red-200 bg-red-50 dark:border-red-700 dark:bg-red-900/20'}`}>
                          <div className="flex justify-between items-center mb-2">
                              <h3 className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">Delivery Próprio</h3>
                              <span className={`font-bold text-sm px-2 py-0.5 rounded ${deliveryPro ? 'bg-green-200 text-green-800 dark:bg-green-700 dark:text-green-100' : 'bg-red-200 text-red-800 dark:bg-red-700 dark:text-red-100'}`}>
                                  {textoStatusDelivery}
                              </span>
                         </div>
                          <div className="text-sm space-y-1 text-tema-text-muted dark:text-tema-text-muted-dark">
                              <p>Limite de Pedidos: <span className="font-semibold text-tema-text dark:text-tema-text-dark">{textoLimitePedidos}</span></p>
                              {aplicaLimitePedidos && ( // Mostra contador apenas se o limite se aplica
                                  <p>Uso Atual (Este Mês):
                                      <span className={`font-semibold ml-1 ${limiteAtingido ? 'text-red-600 dark:text-red-400' : 'text-tema-text dark:text-tema-text-dark'}`}>
                                          {pedidosAtuais} / {limitePedidosConfig}
                                      </span>
                                      {limiteAtingido && <span className="text-red-600 dark:text-red-400 ml-1">(Limite Atingido!)</span>}
                                  </p>
                              )}
                         </div>
                         {/* Mostra pedidos compensados apenas se o limite se aplica */}
                         {aplicaLimitePedidos && userProfile?.pedidosRestantesCompensados > 0 && (
                              <div className="mt-3 p-2 bg-yellow-100 border border-yellow-300 text-yellow-800 rounded text-xs font-medium dark:bg-yellow-900/30 dark:border-yellow-700 dark:text-yellow-200">
                                   <CheckIcon className="inline h-4 w-4 mr-1 text-yellow-600 dark:text-yellow-400" />
                                   {userProfile.pedidosRestantesCompensados} pedidos extras disponíveis este mês via Pay-per-Use.
                              </div>
                          )}
                    </div>
                </div> {/* Fim do Card Detalhado */}


                {/* Card de Ações: Gerenciar Plano / Comprar Extras (Plataforma) */}
                {!isLegacyFree && !isBetaTester && (
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg border dark:border-gray-700 space-y-4">
                        <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark">
                             Gerenciar Assinatura (Plataforma) / Comprar Pedidos Extras
                        </h2>

                         <button
                             onClick={acaoBotaoPrincipal}
                             disabled={loadingManualUpdate || loadingProfile || loadingBotaoPrincipal || loadingConnect}
                             className={`w-full py-3 rounded-lg font-bold text-white bg-tema-primary hover:bg-opacity-90 transition-colors shadow-md ${(loadingManualUpdate || loadingProfile || loadingBotaoPrincipal || loadingConnect) ? 'opacity-50 cursor-not-allowed' : ''}`}
                         >
                            {loadingBotaoPrincipal ? 'Abrindo portal...' : textoBotaoPrincipal}
                         </button>

                         {/* Botão Pay-Per-Use (Só aparece se limite se aplica E não for Pro) */}
                         {aplicaLimitePedidos && !deliveryPro && (
                              <button
                                  onClick={handlePayPerUse}
                                  disabled={loadingManualUpdate || loadingProfile || loadingPortal || loadingConnect}
                                  className={`w-full py-3 rounded-lg font-bold text-gray-800 bg-yellow-400 hover:bg-yellow-500 transition-colors ${(loadingManualUpdate || loadingProfile || loadingPortal || loadingConnect) ? 'opacity-50 cursor-not-allowed' : ''}`}
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
                     limiteAtual={limitePedidosConfig} // Usa o limite correto
                     refreshProfile={refreshProfile}
                 />
             )}
        </>
    );
};

export default FinanceiroPage;