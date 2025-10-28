// frontend/src/Pages/CardapioClientePage.jsx

import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom'; // Adicionar useLocation
import apiClient from '../services/apiClient';
import { toast, ToastContainer } from 'react-toastify';

// --- Componente do Modal do Produto (sem alterações) ---
const ProdutoModal = ({ produto, onClose, onAddToCart }) => {
    const [quantidade, setQuantidade] = useState(1);
    const [observacao, setObservacao] = useState('');

    const handleAddItem = () => {
        if (quantidade <= 0) {
            toast.warn('A quantidade deve ser maior que zero.');
            return;
        }
        onAddToCart(produto, quantidade, observacao);
        onClose();
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50 p-4" onClick={onClose}>
            <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl w-full max-w-md max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
                {/* Imagem */}
                <div className="w-full h-48 bg-gray-200 dark:bg-gray-700 rounded-t-lg flex items-center justify-center overflow-hidden">
                    {produto.imageUrl ? (
                           <img src={produto.imageUrl} alt={produto.nome} className="w-full h-full object-cover" />
                    ) : (
                           <span className="text-gray-400">Imagem Indisponível</span>
                    )}
                </div>
                {/* Conteúdo */}
                <div className="p-6">
                    <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">{produto.nome}</h2>
                    <p className="text-tema-text-muted dark:text-tema-text-muted-dark mt-2">{produto.descricao}</p>
                    <p className="text-2xl font-bold text-tema-text dark:text-tema-text-dark my-4">R$ {produto.preco.toFixed(2).replace('.', ',')}</p>

                    <textarea
                        value={observacao}
                        onChange={(e) => setObservacao(e.target.value)}
                        placeholder="Alguma observação? Ex: sem cebola, ponto da carne, etc."
                        className="w-full p-2 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark text-sm mb-4"
                        rows="2"
                    ></textarea>

                    <div className="flex justify-between items-center mt-6">
                        <div className="flex items-center gap-3">
                            <button onClick={() => setQuantidade(q => Math.max(1, q - 1))} className="bg-gray-200 dark:bg-gray-700 dark:text-tema-text-dark rounded-full w-10 h-10 font-bold text-xl">-</button>
                            <span className="w-10 text-center font-bold text-xl dark:text-tema-text-dark">{quantidade}</span>
                            <button onClick={() => setQuantidade(q => q + 1)} className="bg-gray-200 dark:bg-gray-700 dark:text-tema-text-dark rounded-full w-10 h-10 font-bold text-xl">+</button>
                        </div>
                        <button onClick={handleAddItem} className="bg-tema-primary text-white font-bold py-3 px-6 rounded-lg hover:bg-opacity-80 transition-colors">
                            Adicionar R$ {(produto.preco * quantidade).toFixed(2).replace('.', ',')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};


// --- Componente Principal da Página ---
const CardapioClientePage = () => {
    const { restauranteId } = useParams();
    const navigate = useNavigate();
    const location = useLocation(); // Para ler parâmetros da URL de retorno do Stripe

    const [cardapioInfo, setCardapioInfo] = useState(null); // Renomeado para evitar conflito
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [carrinho, setCarrinho] = useState([]);
    const [isCarrinhoOpen, setIsCarrinhoOpen] = useState(false);
    const [dadosCliente, setDadosCliente] = useState({ nomeCliente: '', telefoneCliente: '', enderecoCliente: '', pontoReferencia: '', cepCliente: '' });
    const [produtoSelecionado, setProdutoSelecionado] = useState(null);
    const [abaAtiva, setAbaAtiva] = useState('cardapio');
    const [pedidosDoDia, setPedidosDoDia] = useState([]);
    const [tipoPagamentoSelecionado, setTipoPagamentoSelecionado] = useState('ONLINE'); // Default para online se disponível

    // Estados para frete
    const [taxaEntrega, setTaxaEntrega] = useState(0);
    const [statusFrete, setStatusFrete] = useState('Aguardando CEP');
    const [loadingFrete, setLoadingFrete] = useState(false); // Loading para o cálculo
    const [loadingPedido, setLoadingPedido] = useState(false); // Loading para envio do pedido

    // Flags derivadas dos dados do cardápio
    const isHaversineAtivo = cardapioInfo?.calculoHaversineAtivo || false;
    const isPagamentoOnlineAtivo = cardapioInfo?.pagamentoOnlineAtivo || false;

    // Busca o cardápio público e trata retorno do Stripe
    useEffect(() => {
        const fetchCardapio = async () => {
            try {
                setLoading(true);
                const data = await apiClient.get(`/api/publico/cardapio/${restauranteId}`);
                setCardapioInfo(data); // Armazena todas as infos

                // Inicializa taxa de entrega e status
                const haversine = data.calculoHaversineAtivo || false;
                setTaxaEntrega(haversine ? -3.00 : data.taxaEntrega || 0); // -3 indica que precisa calcular
                setStatusFrete(haversine ? 'Informe o CEP para calcular' : 'Taxa Fixa');

                // Define pagamento ONLINE como padrão apenas se estiver ativo
                if (data.pagamentoOnlineAtivo) {
                    setTipoPagamentoSelecionado('ONLINE');
                } else {
                    setTipoPagamentoSelecionado('DINHEIRO'); // Fallback se online não estiver ativo
                }

            } catch (err) {
                setError("Cardápio não encontrado ou indisponível.");
            } finally {
                setLoading(false);
            }
        };
        fetchCardapio();

         // Trata retorno do Stripe (sucesso ou cancelamento)
         const queryParams = new URLSearchParams(location.search);
         const paymentStatus = queryParams.get('payment');
         const pedidoUuid = queryParams.get('pedido_uuid');

         if (paymentStatus === 'success' && pedidoUuid) {
             toast.success(`Pagamento do pedido #${pedidoUuid.substring(0,8)}... confirmado! Acompanhe na aba 'Pedidos'.`);
             setAbaAtiva('acompanhar'); // Muda para a aba de acompanhamento
             // Limpa os parâmetros da URL
             navigate(`/cardapio/${restauranteId}`, { replace: true });
         } else if (paymentStatus === 'cancel') {
              toast.warn("Pagamento cancelado. Seu pedido não foi finalizado.");
              // Limpa os parâmetros da URL
              navigate(`/cardapio/${restauranteId}`, { replace: true });
         }

    }, [restauranteId, location.search, navigate]); // Adiciona location e navigate

    // Carrega pedidos do dia do localStorage
    useEffect(() => {
        const storedPedidos = localStorage.getItem(`pedidosDia_${restauranteId}`);
        if (storedPedidos) {
            try {
                setPedidosDoDia(JSON.parse(storedPedidos));
            } catch (e) {
                console.error("Erro ao carregar pedidos do dia do localStorage:", e);
                localStorage.removeItem(`pedidosDia_${restauranteId}`); // Limpa dados inválidos
            }
        }
    }, [restauranteId]);

    // Função para adicionar ao carrinho (sem alterações)
     const handleAddToCart = (produto, quantidade, observacao) => {
        const itemId = `${produto.id}-${observacao || 'semObs'}`; // ID único no carrinho
        setCarrinho(prev => {
            const itemExistente = prev.find(item => item.itemId === itemId);
            if (itemExistente) {
                return prev.map(item => item.itemId === itemId ? { ...item, quantidade: item.quantidade + quantidade } : item);
            }
            // produtoId é o ID real para a API
            return [...prev, { ...produto, produtoId: produto.id, itemId, quantidade, observacao }];
        });
        toast.success(`${quantidade}x ${produto.nome} adicionado!`);
    };

    // Função para remover do carrinho (nova)
    const handleRemoveFromCart = (itemId) => {
         setCarrinho(prev => prev.filter(item => item.itemId !== itemId));
         toast.info("Item removido.");
    };

    // Handler para inputs do cliente
    const handleInputChange = (e) => {
        const { name, value } = e.target;
        // Limpa CEP
        const cleanedValue = name === 'cepCliente' ? value.replace(/\D/g, '') : value;

        setDadosCliente(prev => ({ ...prev, [name]: cleanedValue }));

        // Reseta taxa se Haversine ativo e CEP mudou
        if (name === 'cepCliente' && isHaversineAtivo) {
            setTaxaEntrega(-3.00); // -3 indica que precisa calcular
            setStatusFrete('CEP alterado, recalcule o frete');
        }
    };

    // --- >> NOVA FUNÇÃO: Buscar Frete << ---
    const buscarFrete = async () => {
        if (!isHaversineAtivo) return; // Só executa se Haversine estiver ativo

        const cepLimpo = dadosCliente.cepCliente; // Já está limpo pelo handleInputChange
        if (!cepLimpo || cepLimpo.length !== 8) {
            setStatusFrete('CEP inválido (8 dígitos)');
            setTaxaEntrega(-1.00); // -1 indica erro de CEP
            toast.warn('Por favor, insira um CEP válido com 8 dígitos.');
            return;
        }

        setLoadingFrete(true);
        setStatusFrete('Calculando...');
        try {
            // Chama o endpoint de cálculo de frete
            const response = await apiClient.get(`/api/publico/frete/${restauranteId}/${cepLimpo}`);
            const taxa = response.taxaEntrega; // O backend retorna a taxa correta

            setTaxaEntrega(taxa);
            setStatusFrete(`Entrega: R$ ${taxa.toFixed(2).replace('.', ',')}`);
            toast.success("Taxa de entrega calculada!");

        } catch (err) {
            const errorMsg = err.message || "Erro ao calcular frete.";
            console.error("Erro buscarFrete:", err); // Log detalhado
            if (errorMsg.includes("fora de área")) {
                setStatusFrete("Entrega Indisponível (Fora de Área)");
                setTaxaEntrega(-2.00); // -2 indica fora de área
            } else if (errorMsg.includes("inválido") || errorMsg.includes("não encontrado")) {
                 setStatusFrete("CEP inválido ou não encontrado");
                 setTaxaEntrega(-1.00); // -1 indica erro de CEP
            } else {
                 setStatusFrete('Erro no cálculo');
                 setTaxaEntrega(-1.00); // Erro genérico
            }
            toast.error(errorMsg);
        } finally {
            setLoadingFrete(false);
        }
    };

    // Cálculos de totais (useMemo)
    const subtotalCarrinho = useMemo(() => {
        return carrinho.reduce((acc, item) => acc + (item.preco * item.quantidade), 0);
    }, [carrinho]);

    const totalCarrinho = useMemo(() => {
        // Só soma a taxa se ela for válida (>= 0)
        const taxaValida = taxaEntrega >= 0 ? taxaEntrega : 0;
        return subtotalCarrinho + taxaValida;
    }, [subtotalCarrinho, taxaEntrega]);

    const totalItensCarrinho = useMemo(() => {
        return carrinho.reduce((acc, item) => acc + item.quantidade, 0);
    }, [carrinho]);


    // --- >> FUNÇÃO ATUALIZADA: Enviar Pedido << ---
    const handleEnviarPedido = async () => {
        // Validações básicas
        if (carrinho.length === 0) {
            toast.warn("Sua sacola está vazia.");
            return;
        }
        if (!dadosCliente.nomeCliente || !dadosCliente.telefoneCliente || !dadosCliente.enderecoCliente) {
            toast.warn("Por favor, preencha seu nome, telefone e endereço de entrega.");
            return;
        }
         // Validação do valor mínimo do pedido (se houver taxa calculada e valor mínimo definido)
         if (taxaEntrega >= 0 && cardapioInfo?.valorMinimoPedido > 0 && subtotalCarrinho < cardapioInfo.valorMinimoPedido) {
             toast.warn(`O valor mínimo para entrega é R$ ${cardapioInfo.valorMinimoPedido.toFixed(2).replace('.', ',')}.`);
             return;
         }


        // Validação de Frete (especialmente se Haversine ativo)
        if (isHaversineAtivo) {
            if (!dadosCliente.cepCliente || dadosCliente.cepCliente.length !== 8) {
                toast.warn("Por favor, informe um CEP válido (8 dígitos) e calcule o frete.");
                return;
            }
            if (taxaEntrega === -3.00) { // -3 significa que não foi calculado ainda
                 toast.warn("Por favor, clique em 'Calcular Frete' antes de finalizar.");
                 return;
            }
            if (taxaEntrega < 0) { // -1 (CEP inválido) ou -2 (Fora de área)
                toast.error(`Entrega indisponível para o CEP informado (${statusFrete}).`);
                return;
            }
        }

        setLoadingPedido(true); // Ativa loading do botão principal

        const pedidoParaApi = {
            restauranteId: Number(restauranteId),
            nomeCliente: dadosCliente.nomeCliente,
            telefoneCliente: dadosCliente.telefoneCliente,
            enderecoCliente: dadosCliente.enderecoCliente,
            pontoReferencia: dadosCliente.pontoReferencia,
            cepCliente: dadosCliente.cepCliente, // Sempre envia o CEP
            itens: carrinho.map(item => ({
                produtoId: item.produtoId, // Usa o ID real do produto
                quantidade: item.quantidade,
                observacao: item.observacao,
                adicionaisIds: [], // Adicionar lógica de adicionais se implementado
            }))
        };

        try {
            if (tipoPagamentoSelecionado === 'ONLINE') {
                // --- FLUXO PAGAMENTO ONLINE ---
                logger.debug("Iniciando fluxo de pagamento online...");
                // Chama o endpoint que GERA a URL de pagamento
                const response = await apiClient.post('/api/publico/pagar/delivery', pedidoParaApi);
                const { paymentUrl } = response;

                if (!paymentUrl) {
                     throw new Error("Não foi possível obter o link de pagamento.");
                }

                toast.info("Redirecionando para o pagamento seguro...");

                // Salva um placeholder no localStorage ANTES de redirecionar
                 const placeholderPedido = {
                     uuid: 'aguardando_pagamento_' + Date.now(), // ID temporário
                     data: new Date().toLocaleTimeString('pt-BR'),
                     total: totalCarrinho,
                     itens: carrinho.map(item => `${item.quantidade}x ${item.nome}`),
                     status: 'Aguardando Pagamento' // Status visual
                 };
                 const updatedPedidos = [placeholderPedido, ...pedidosDoDia];
                 setPedidosDoDia(updatedPedidos);
                 localStorage.setItem(`pedidosDia_${restauranteId}`, JSON.stringify(updatedPedidos));

                // Limpa o carrinho localmente
                setCarrinho([]);
                setIsCarrinhoOpen(false);

                // REDIRECIONA para a URL do Stripe
                window.location.href = paymentUrl;
                // O setLoadingPedido(false) não será chamado aqui devido ao redirecionamento

            } else {
                // --- FLUXO PAGAMENTO NA ENTREGA ---
                logger.debug("Iniciando fluxo de pagamento na entrega...");
                // Chama o endpoint que SALVA o pedido offline
                const endpoint = `/api/publico/pedido/delivery?pagamento=${tipoPagamentoSelecionado}`;
                const novoPedido = await apiClient.post(endpoint, pedidoParaApi);

                toast.success(`Pedido #${novoPedido.id} enviado! Pagamento na entrega: ${tipoPagamentoSelecionado.replace('_', ' ')}`);

                // Salva pedido no localStorage para acompanhamento
                if (novoPedido && novoPedido.uuid) {
                    const novoPedidoComDetalhes = {
                        uuid: novoPedido.uuid,
                        id: novoPedido.id, // Guarda o ID real se precisar
                        data: new Date(novoPedido.dataHora).toLocaleTimeString('pt-BR'), // Usa data do backend
                        total: novoPedido.total, // Usa total do backend
                        itens: novoPedido.itens.map(item => `${item.quantidade}x ${item.produto.nome}`), // Mapeia itens do backend
                        status: 'Enviado' // Status inicial visual
                    };
                    const updatedPedidos = [novoPedidoComDetalhes, ...pedidosDoDia];
                    setPedidosDoDia(updatedPedidos);
                    localStorage.setItem(`pedidosDia_${restauranteId}`, JSON.stringify(updatedPedidos));
                }

                setCarrinho([]);
                setIsCarrinhoOpen(false);
                setAbaAtiva('acompanhar'); // Muda para a aba de acompanhamento
            }

        } catch (err) {
            const errorMsg = err.message || "Houve um erro ao enviar seu pedido. Tente novamente.";
            console.error("Erro handleEnviarPedido:", err); // Log detalhado
            toast.error(errorMsg);
        } finally {
             // Garante que o loading para, exceto se foi redirecionado
             if (tipoPagamentoSelecionado !== 'ONLINE') {
                 setLoadingPedido(false);
             }
        }
    };


    // --- Renderização das Abas ---
    const renderContent = () => {
        if (abaAtiva === 'cardapio') {
            return (
                <main className="max-w-6xl mx-auto p-4 md:p-6 space-y-8 pb-32"> {/* Aumenta padding bottom */}
                    {/* Filtros de Categoria */}
                    <div className="flex gap-3 overflow-x-auto pb-2 sticky top-16 z-10 bg-gray-100 dark:bg-tema-fundo-dark py-2 -mx-4 px-4"> {/* Sticky Nav */}
                        {cardapioInfo.categorias.map(categoria => (
                            <a href={`#categoria-${categoria.id}`} key={categoria.id} className="px-5 py-2 rounded-lg font-semibold text-sm whitespace-nowrap transition-colors bg-white dark:bg-tema-surface-dark text-gray-700 dark:text-gray-300 shadow-sm border dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700">
                                {categoria.nome}
                            </a>
                        ))}
                    </div>

                    {/* Seções de Produtos por Categoria */}
                    {cardapioInfo.categorias.map(categoria => (
                        categoria.produtos.length > 0 && (
                            <section id={`categoria-${categoria.id}`} key={categoria.id} className="pt-4"> {/* pt-4 para espaço do sticky nav */}
                                <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">{categoria.nome}</h2>
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                    {categoria.produtos.map(produto => (
                                        <div
                                            key={produto.id}
                                            onClick={() => setProdutoSelecionado(produto)}
                                            className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-sm border dark:border-gray-700 flex gap-4 cursor-pointer hover:border-tema-primary transition-all duration-200"
                                        >
                                            <div className="flex-grow">
                                                <h3 className="font-semibold text-tema-text dark:text-tema-text-dark">{produto.nome}</h3>
                                                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mt-1 line-clamp-2">{produto.descricao}</p>
                                                <p className="font-semibold text-tema-text dark:text-tema-text-dark mt-2">R$ {produto.preco.toFixed(2).replace('.', ',')}</p>
                                            </div>
                                            {/* Imagem */}
                                            <div className="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-md flex-shrink-0 flex items-center justify-center overflow-hidden">
                                                {produto.imageUrl ? (
                                                    <img src={produto.imageUrl} alt={produto.nome} className="w-full h-full object-cover" />
                                                ) : (
                                                    <span className="text-xs text-gray-400">Sem Foto</span>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        )
                    ))}
                </main>
            );
        }

        if (abaAtiva === 'acompanhar') {
            return (
                <main className="max-w-6xl mx-auto p-4 md:p-6 space-y-8 pb-32">
                    <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-6">Seus Pedidos no Dia</h2>

                    {pedidosDoDia.length > 0 ? (
                        <div className="space-y-4">
                            {pedidosDoDia.map((p, index) => (
                                <div key={p.uuid || index} className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 flex justify-between items-center flex-wrap gap-2">
                                    <div>
                                        <p className="font-bold text-lg text-tema-text dark:text-tema-text-dark">Pedido #{pedidosDoDia.length - index} <span className="text-sm font-normal text-tema-text-muted dark:text-tema-text-muted-dark">({p.data})</span></p>
                                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Total: R$ {p.total.toFixed(2).replace('.', ',')}</p>
                                        <ul className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark list-disc list-inside pl-2 mt-1">
                                            {p.itens.slice(0, 3).map((item, i) => <li key={i} className="truncate max-w-[200px]">{item}</li>)}
                                            {p.itens.length > 3 && <li className="text-xs italic">... e mais itens</li>}
                                        </ul>
                                        {/* Mostra status visual */}
                                        {p.status && <p className="text-xs font-semibold mt-1 text-blue-600 dark:text-blue-400">{p.status}</p>}
                                    </div>
                                    {/* Botão de rastrear só aparece se tiver UUID real */}
                                    {!p.uuid.startsWith('aguardando_') && (
                                         <button
                                             onClick={() => navigate(`/rastrear/${p.uuid}`)}
                                             className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors text-sm"
                                         >
                                             Rastrear
                                         </button>
                                     )}
                                     {/* Mensagem para pagamentos pendentes */}
                                     {p.uuid.startsWith('aguardando_') && (
                                         <span className="text-sm font-semibold text-orange-500">Aguardando Pagamento...</span>
                                     )}
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="p-10 text-center bg-white dark:bg-tema-surface-dark rounded-lg border dark:border-gray-700">
                             <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Você ainda não fez pedidos hoje. Comece pela aba "Cardápio".</p>
                        </div>
                    )}
                </main>
            );
        }
        return null;
    }


    if (loading) return <div className="text-center p-10 text-lg font-semibold text-gray-500 dark:text-gray-400">Carregando cardápio...</div>;
    if (error) return <div className="text-center p-10 text-lg font-semibold text-red-500">{error}</div>;
    if (!cardapioInfo) return <div className="text-center p-10 text-lg font-semibold text-gray-500 dark:text-gray-400">Cardápio indisponível.</div>; // Fallback


    return (
        <div className="w-full min-h-screen bg-gray-100 dark:bg-tema-fundo-dark">
            {/* Header */}
            <header className="bg-white dark:bg-tema-surface-dark shadow-md p-4 sticky top-0 z-20">
                <div className="max-w-6xl mx-auto">
                    {cardapioInfo.logoUrl ? (
                           <img src={cardapioInfo.logoUrl} alt={cardapioInfo.nomeRestaurante} className="h-10 mb-2 object-contain mx-auto md:mx-0" />
                    ) : (
                           <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark text-center md:text-left">{cardapioInfo.nomeRestaurante}</h1>
                    )}
                    <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark text-center md:text-left">{cardapioInfo.enderecoRestaurante}</p>

                    {/* Abas */}
                    <div className="flex gap-4 mt-3 border-b dark:border-gray-700">
                        <button
                            onClick={() => setAbaAtiva('cardapio')}
                            className={`pb-2 font-semibold text-lg transition-colors ${abaAtiva === 'cardapio' ? 'text-tema-primary border-b-2 border-tema-primary dark:text-tema-link-dark dark:border-tema-link-dark' : 'text-tema-text-muted dark:text-tema-text-muted-dark hover:text-tema-primary dark:hover:text-tema-link-dark'}`}
                        >
                            Cardápio
                        </button>
                        <button
                            onClick={() => setAbaAtiva('acompanhar')}
                            className={`pb-2 font-semibold text-lg transition-colors relative ${abaAtiva === 'acompanhar' ? 'text-tema-primary border-b-2 border-tema-primary dark:text-tema-link-dark dark:border-tema-link-dark' : 'text-tema-text-muted dark:text-tema-text-muted-dark hover:text-tema-primary dark:hover:text-tema-link-dark'}`}
                        >
                            Meus Pedidos
                            {/* Badge com contador */}
                            {pedidosDoDia.length > 0 && (
                                 <span className="absolute top-0 right-0 transform translate-x-1/2 -translate-y-1/2 bg-red-600 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
                                     {pedidosDoDia.length}
                                 </span>
                             )}
                        </button>
                    </div>
                </div>
            </header>

            {/* Conteúdo da Aba */}
            {renderContent()}

            {/* Modal do Produto */}
            {produtoSelecionado && <ProdutoModal produto={produtoSelecionado} onClose={() => setProdutoSelecionado(null)} onAddToCart={handleAddToCart} />}

            {/* Footer Flutuante (Sacola) - Só na aba Cardápio */}
            {carrinho.length > 0 && abaAtiva === 'cardapio' && (
                <div className="fixed bottom-0 left-0 right-0 bg-white dark:bg-tema-surface-dark shadow-lg border-t dark:border-gray-700 p-4 z-30">
                    <div className="max-w-6xl mx-auto">
                        <button onClick={() => setIsCarrinhoOpen(true)} className="w-full bg-tema-primary text-white font-bold py-3 rounded-lg flex justify-between items-center px-4 hover:bg-opacity-90 transition-colors">
                            <span>Ver Sacola ({totalItensCarrinho} {totalItensCarrinho === 1 ? 'item' : 'itens'})</span>
                            <span>R$ {subtotalCarrinho.toFixed(2).replace('.', ',')}</span>
                        </button>
                    </div>
                </div>
            )}

            {/* Modal da Sacola/Checkout */}
            {isCarrinhoOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-60 z-40 flex items-end md:items-center justify-center" onClick={() => setIsCarrinhoOpen(false)}>
                    {/* Conteúdo do Modal */}
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-t-2xl md:rounded-lg w-full max-w-lg max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
                        <h2 className="text-2xl font-bold mb-4 dark:text-tema-text-dark text-center">Finalizar Pedido</h2>

                        {/* Itens do Carrinho */}
                        <div className="space-y-3 max-h-40 overflow-y-auto mb-4 border-b pb-4 dark:border-gray-700 pr-2">
                             {carrinho.length > 0 ? carrinho.map(item => (
                                 <div key={item.itemId} className="flex justify-between items-start text-sm">
                                     <div className="flex-grow mr-2">
                                         <p className="dark:text-tema-text-dark font-semibold">{item.quantidade}x {item.nome}</p>
                                         {item.observacao && <p className="text-xs text-gray-500 dark:text-gray-400 italic">Obs: {item.observacao}</p>}
                                     </div>
                                     <div className="flex items-center flex-shrink-0">
                                          <span className='font-semibold dark:text-tema-text-dark mr-2'>R$ {(item.preco * item.quantidade).toFixed(2).replace('.', ',')}</span>
                                          <button type="button" onClick={() => handleRemoveFromCart(item.itemId)} className="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-500 text-lg font-bold leading-none">&times;</button>
                                     </div>
                                 </div>
                            )) : <p className="text-center text-gray-500 dark:text-gray-400">Sacola vazia.</p>}
                        </div>

                        {/* Dados do Cliente */}
                         <div className="space-y-4 mb-6">
                            <input type="text" name="nomeCliente" value={dadosCliente.nomeCliente} onChange={handleInputChange} placeholder="* Seu Nome Completo" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-white" required />
                            <input type="tel" name="telefoneCliente" value={dadosCliente.telefoneCliente} onChange={handleInputChange} placeholder="* Telefone / WhatsApp (com DDD)" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-white" required />
                             {/* Mostra CEP sempre, mas a label e o botão mudam */}
                             <div>
                                 <label className="block text-sm font-bold text-tema-text dark:text-tema-text-dark mb-1">
                                      {isHaversineAtivo ? '* CEP (para cálculo do frete)' : 'CEP (opcional)'}
                                 </label>
                                 <div className="flex gap-2 items-center">
                                      <input
                                          type="text" // Use text para facilitar a máscara no futuro
                                          name="cepCliente"
                                          value={dadosCliente.cepCliente}
                                          onChange={handleInputChange}
                                          placeholder="Apenas números"
                                          maxLength={8}
                                          className={`flex-grow p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-white ${isHaversineAtivo && taxaEntrega < 0 && taxaEntrega !== -3.00 ? 'border-red-500' : ''}`}
                                          required={isHaversineAtivo}
                                      />
                                      {isHaversineAtivo && (
                                          <button
                                              type="button"
                                              onClick={buscarFrete}
                                              disabled={loadingFrete || !dadosCliente.cepCliente || dadosCliente.cepCliente.length !== 8}
                                              className="bg-tema-primary text-white font-bold px-4 py-3 rounded-lg hover:bg-opacity-80 transition-colors disabled:bg-gray-400 whitespace-nowrap"
                                          >
                                              {loadingFrete ? '...' : 'Calcular'}
                                          </button>
                                      )}
                                 </div>
                                 {/* Mensagem de Status/Erro do Frete */}
                                 <p className={`text-sm mt-1 font-semibold ${
                                     taxaEntrega >= 0 ? 'text-green-600 dark:text-green-400' :
                                     taxaEntrega === -3.00 ? 'text-gray-500 dark:text-gray-400' :
                                     'text-red-600 dark:text-red-400'
                                 }`}>
                                     {statusFrete}
                                </p>
                             </div>

                             <input type="text" name="enderecoCliente" value={dadosCliente.enderecoCliente} onChange={handleInputChange} placeholder="* Endereço Completo (Rua, Nº, Bairro)" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-white" required />
                             <input type="text" name="pontoReferencia" value={dadosCliente.pontoReferencia} onChange={handleInputChange} placeholder="Ponto de referência (opcional)" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-white" />
                         </div>

                        {/* Resumo Financeiro */}
                        <div className="text-sm dark:text-tema-text-dark mt-4 pt-4 border-t dark:border-gray-700 space-y-1">
                            <div className="flex justify-between">
                                <span>Subtotal:</span>
                                <span>R$ {subtotalCarrinho.toFixed(2).replace('.',',')}</span>
                            </div>
                            {/* Mostra taxa apenas se calculada com sucesso ou fixa */}
                            {taxaEntrega >= 0 && (
                                <div className="flex justify-between">
                                    <span>Taxa de Entrega:</span>
                                    <span>R$ {taxaEntrega.toFixed(2).replace('.',',')}</span>
                                </div>
                            )}
                             {/* Mensagem se frete inválido/fora de área */}
                             {taxaEntrega < 0 && taxaEntrega !== -3.00 && (
                                 <div className="flex justify-between text-red-600 dark:text-red-400 font-semibold">
                                     <span>Taxa de Entrega:</span>
                                     <span>Indisponível</span>
                                 </div>
                             )}
                            <div className="flex justify-between font-bold text-lg pt-2 mt-2 border-t dark:border-gray-600">
                                <span>Total:</span>
                                <span>R$ {totalCarrinho.toFixed(2).replace('.',',')}</span>
                            </div>
                        </div>

                        {/* Seletor de Pagamento */}
                         <div className="mt-6">
                             <label className="block text-sm font-bold text-tema-text dark:text-tema-text-dark mb-2">Forma de Pagamento:</label>
                             <select
                                 value={tipoPagamentoSelecionado}
                                 onChange={(e) => setTipoPagamentoSelecionado(e.target.value)}
                                 className="w-full p-3 border rounded-lg bg-white dark:bg-gray-800 dark:border-gray-600 dark:text-white"
                             >
                                 {/* Opção Online SÓ aparece se ativa */}
                                 {isPagamentoOnlineAtivo && <option value="ONLINE">💳 Pagar Online Agora (Pix ou Cartão)</option>}
                                 <option value="DINHEIRO">💰 Dinheiro na Entrega</option>
                                 <option value="CARTAO_DEBITO">📲 Maquineta na Entrega (Débito)</option>
                                 <option value="CARTAO_CREDITO">📲 Maquineta na Entrega (Crédito)</option>
                                 <option value="PIX">🤳 Pix na Entrega (Solicitar Chave)</option>
                             </select>
                         </div>

                        {/* Botão Finalizar */}
                        <button
                            onClick={handleEnviarPedido}
                            disabled={loadingPedido || subtotalCarrinho <= 0 || (isHaversineAtivo && taxaEntrega < 0)} // Desabilita se carregando, carrinho vazio ou frete inválido
                            className={`w-full mt-6 py-3 rounded-lg font-bold text-white transition-colors ${
                                loadingPedido || subtotalCarrinho <= 0 || (isHaversineAtivo && taxaEntrega < 0)
                                ? 'bg-gray-400 cursor-not-allowed'
                                : 'bg-green-600 hover:bg-green-700'
                            }`}
                        >
                            {loadingPedido ? 'Enviando...' : (tipoPagamentoSelecionado === 'ONLINE' ? 'Ir para Pagamento Online' : 'Confirmar Pedido (Pagar na Entrega)')}
                        </button>
                    </div>
                </div>
            )}

            <ToastContainer position="bottom-center" autoClose={3500} theme={document.documentElement.classList.contains('dark') ? 'dark' : 'light'} />
        </div>
    );
};

export default CardapioClientePage;

// Adiciona um logger simples para debug no console do navegador
const logger = {
    debug: (...args) => console.debug('[DEBUG]', ...args),
    info: (...args) => console.info('[INFO]', ...args),
    warn: (...args) => console.warn('[WARN]', ...args),
    error: (...args) => console.error('[ERROR]', ...args),
};