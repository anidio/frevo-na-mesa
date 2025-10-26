// anidio/frevo-na-mesa/frevo-na-mesa-8b115646b81a772561fa6372e54dd8820f175b8a/frontend/src/Pages/CardapioClientePage.jsx

import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { toast, ToastContainer } from 'react-toastify';

// --- Componente do Modal do Produto (Completo) ---
const ProdutoModal = ({ produto, onClose, onAddToCart }) => {
    const [quantidade, setQuantidade] = useState(1);
    const [observacao, setObservacao] = useState('');

    const handleAddItem = () => {
        if (quantidade <= 0) {
            toast.warn('A quantidade deve ser maior que zero.');
            return;
        }
        // Chamada para adicionar ao carrinho, que está no componente pai
        onAddToCart(produto, quantidade, observacao);
        onClose();
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50" onClick={onClose}>
            <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl w-full max-w-md" onClick={e => e.stopPropagation()}>
                {/* Exibir Imagem do Produto */}
                <div className="w-full h-48 bg-gray-200 dark:bg-gray-700 rounded-t-lg flex items-center justify-center overflow-hidden">
                    {produto.imageUrl ? (
                         <img src={produto.imageUrl} alt={produto.nome} className="w-full h-full object-cover" />
                    ) : (
                        <span className="text-gray-400">Imagem do Produto</span>
                    )}
                </div>
                <div className="p-6">
                    <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">{produto.nome}</h2>
                    <p className="text-tema-text-muted dark:text-tema-text-muted-dark mt-2">{produto.descricao}</p>
                    <p className="text-2xl font-bold text-tema-text dark:text-tema-text-dark my-4">R$ {produto.preco.toFixed(2).replace('.', ',')}</p>
                    
                    <textarea 
                        value={observacao} 
                        onChange={(e) => setObservacao(e.target.value)} 
                        placeholder="Alguma observação? Ex: sem cebola, ponto da carne, etc." 
                        className="w-full p-2 border rounded-lg dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark text-sm" 
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
    const [cardapio, setCardapio] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [carrinho, setCarrinho] = useState([]);
    const [isCarrinhoOpen, setIsCarrinhoOpen] = useState(false);
    
    // [MODIFICADO] cepCliente existe para registro
    const [dadosCliente, setDadosCliente] = useState({ nomeCliente: '', telefoneCliente: '', enderecoCliente: '', pontoReferencia: '', cepCliente: '' });
    const [produtoSelecionado, setProdutoSelecionado] = useState(null);
    const [abaAtiva, setAbaAtiva] = useState('cardapio'); 
    const [pedidosDoDia, setPedidosDoDia] = useState([]); 
    const [tipoPagamentoSelecionado, setTipoPagamentoSelecionado] = useState('ONLINE'); 
    
    // Estados para o cálculo dinâmico de frete
    const [taxaEntrega, setTaxaEntrega] = useState(0); 
    const [statusFrete, setStatusFrete] = useState('Aguardando CEP'); 
    const navigate = useNavigate();
    
    // CORREÇÃO #1: Leitura robusta da flag do backend (isCalculoHaversineAtivo OU calculoHaversineAtivo)
    const isHaversineAtivo = cardapio?.calculoHaversineAtivo || cardapio?.isCalculoHaversineAtivo || false; 

    // Função para buscar o frete dinamicamente
    const buscarFrete = async () => {
        // Esta função só é chamada se isHaversineAtivo for true.
        const cepLimpo = dadosCliente.cepCliente.replace(/\D/g, '');
        
        if (!cepLimpo || cepLimpo.length !== 8) {
            setStatusFrete('CEP inválido');
            setTaxaEntrega(-1.00); 
            toast.warn('Por favor, insira um CEP de 8 dígitos.');
            return;
        }
        
        setDadosCliente(prev => ({ ...prev, cepCliente: cepLimpo }));

        try {
            setStatusFrete('Calculando...');
            // O endpoint /frete usa a lógica condicional do backend (Haversine ou Taxa Fixa)
            const response = await apiClient.get(`/api/publico/frete/${restauranteId}/${cepLimpo}`);
            
            const taxa = response.taxaEntrega; 
            
            setTaxaEntrega(taxa);
            setStatusFrete(`Entrega: R$ ${taxa.toFixed(2).replace('.', ',')}`);
            toast.success("Taxa de entrega calculada!");

        } catch (err) {
            const errorMsg = err.message || "Erro desconhecido ao calcular frete.";
            
            if (errorMsg.includes("CEP inválido") || errorMsg.includes("Verifique o número")) {
                 setStatusFrete("CEP inválido ou não encontrado.");
            } else if (errorMsg.includes("fora de área")) {
                 setStatusFrete("Entrega Indisponível (Fora de Área)");
            } else if (errorMsg.includes("não está configurado")) {
                 setStatusFrete("Erro: CEP do Restaurante não configurado.");
            } else {
                 setStatusFrete('Erro ao calcular');
            }
            
            setTaxaEntrega(-1.00); // Bloqueia o pedido
            toast.error(errorMsg);
        }
    };

    // Busca o cardápio público
    useEffect(() => {
        const fetchCardapio = async () => {
            try {
                const data = await apiClient.get(`/api/publico/cardapio/${restauranteId}`);
                setCardapio(data);
                
                // CORREÇÃO #2: Leitura robusta da flag do backend no useEffect
                const isHaversine = data.calculoHaversineAtivo || data.isCalculoHaversineAtivo || false;
                
                // Inicializa a taxa com 0 se for Haversine, ou com a Taxa Fixa se for o modo fixo
                setTaxaEntrega(isHaversine ? 0 : data.taxaEntrega || 0); 
                setStatusFrete(isHaversine ? 'Aguardando Cálculo por CEP' : 'Taxa Fixa Ativa');
                
            } catch (err) {
                setError("Cardápio não encontrado ou indisponível.");
            } finally {
                setLoading(false);
            }
        };
        fetchCardapio();
    }, [restauranteId]);
    
    // Carrega pedidos do dia do localStorage na inicialização
    useEffect(() => {
        const storedPedidos = localStorage.getItem(`pedidosDia_${restauranteId}`);
        if (storedPedidos) {
            setPedidosDoDia(JSON.parse(storedPedidos));
        }
    }, [restauranteId]);

    const handleAddToCart = (produto, quantidade, observacao) => {
        // Cria um ID composto para diferenciar itens com a mesma observação
        const itemId = `${produto.id}-${observacao}`;
        setCarrinho(prev => {
            const itemExistente = prev.find(item => item.itemId === itemId);
            if (itemExistente) {
                return prev.map(item => item.itemId === itemId ? { ...item, quantidade: item.quantidade + quantidade } : item);
            }
            // Usa o ID real do produto para enviar à API e o itemId para o carrinho
            return [...prev, { ...produto, produtoId: produto.id, itemId, quantidade, observacao }];
        });
        toast.success(`${quantidade}x ${produto.nome} adicionado ao pedido!`);
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        
        // Reseta a taxa e o status SOMENTE se o Haversine estiver ativo
        if (name === 'cepCliente' && isHaversineAtivo) {
            setTaxaEntrega(0);
            setStatusFrete('Aguardando Recálculo');
        }
        
        setDadosCliente(prev => ({ ...prev, [name]: value }));
    };
    
    // CALCULA O SUBTOTAL (Itens * Preço)
    const subtotalCarrinho = useMemo(() => {
        return carrinho.reduce((acc, item) => acc + (item.preco * item.quantidade), 0);
    }, [carrinho]);

    // CALCULA O TOTAL GERAL (Subtotal + Taxa de Entrega)
    const totalCarrinho = useMemo(() => {
        // Se Haversine está ativo E taxa for negativa (erro/fora de área), o frete é 0, mas o pedido será bloqueado
        if (isHaversineAtivo && taxaEntrega < 0) return subtotalCarrinho; 
        return subtotalCarrinho + taxaEntrega;
    }, [subtotalCarrinho, taxaEntrega, isHaversineAtivo]);


    const totalItensCarrinho = useMemo(() => {
        return carrinho.reduce((acc, item) => acc + item.quantidade, 0);
    }, [carrinho]);

    const handleEnviarPedido = async () => {
        // Validações básicas (Endereço é obrigatório, CEP é condicional)
        if (!dadosCliente.nomeCliente || !dadosCliente.telefoneCliente || !dadosCliente.enderecoCliente) {
            toast.warn("Por favor, preencha seu nome, telefone e endereço.");
            return;
        }
        if (carrinho.length === 0) {
            toast.warn("Adicione itens ao carrinho.");
            return;
        }
        if (totalCarrinho <= 0) {
            toast.warn("O valor total do pedido deve ser maior que zero.");
            return;
        }
        
        // Validação para Haversine Ativo (Se Haversine está ativo, precisa de CEP, cálculo e área)
        if (isHaversineAtivo) {
            if (!dadosCliente.cepCliente) {
                toast.warn("O CEP é obrigatório para o cálculo de frete.");
                return;
            }
            if (taxaEntrega < 0) {
                toast.error("Entrega indisponível para o CEP informado. Corrija o CEP.");
                return;
            }
            // Impede o envio se o cliente digitou o CEP mas não clicou em calcular.
            if (statusFrete.includes('Aguardando')) {
                 toast.warn("Por favor, clique em 'Calcular Frete' para confirmar o valor da entrega.");
                 return;
            }
        }


        const pedidoParaApi = {
            restauranteId: Number(restauranteId), 
            ...dadosCliente,
            itens: carrinho.map(item => ({
                produtoId: item.produtoId, 
                quantidade: item.quantidade,
                observacao: item.observacao,
                adicionaisIds: [], 
            }))
        };
        
        try {
            if (tipoPagamentoSelecionado === 'ONLINE') {
                // FLUXO 1: PAGAR AGORA (Redireciona para o Mercado Pago/Stripe)
                const response = await apiClient.post('/api/publico/pagar/delivery', pedidoParaApi);
                const { paymentUrl } = response;
                
                toast.info("Redirecionando para o pagamento seguro...");
                
                // O pedido é salvo no backend em status AGUARDANDO_PGTO_LIMITE
                const placeholderPedido = { 
                    uuid: 'pending', 
                    data: new Date().toLocaleTimeString('pt-BR'), 
                    total: totalCarrinho,
                    itens: carrinho.map(item => `${item.quantidade}x ${item.nome}`)
                };
                
                // 1. Salva o novo pedido na lista do dia no localStorage
                const updatedPedidos = [placeholderPedido, ...pedidosDoDia];
                setPedidosDoDia(updatedPedidos);
                localStorage.setItem(`pedidosDia_${restauranteId}`, JSON.stringify(updatedPedidos));

                setCarrinho([]);
                setIsCarrinhoOpen(false);

                // REDIRECIONAMENTO CRÍTICO PARA O CHECKOUT
                window.location.href = paymentUrl;

            } else {
                // FLUXO 2: PAGAR NA ENTREGA 
                
                const endpoint = `/api/publico/pedido/delivery?pagamento=${tipoPagamentoSelecionado}`;
                
                const novoPedido = await apiClient.post(endpoint, pedidoParaApi);
                
                toast.success(`Pedido #${novoPedido.id} enviado! Pagamento na entrega: ${tipoPagamentoSelecionado}`);
                
                if (novoPedido && novoPedido.uuid) {
                    const novoPedidoComDetalhes = { 
                        uuid: novoPedido.uuid, 
                        data: new Date().toLocaleTimeString('pt-BR'), 
                        total: totalCarrinho,
                        itens: carrinho.map(item => `${item.quantidade}x ${item.nome}`)
                    };
                    const updatedPedidos = [novoPedidoComDetalhes, ...pedidosDoDia];
                    setPedidosDoDia(updatedPedidos);
                    localStorage.setItem(`pedidosDia_${restauranteId}`, JSON.stringify(updatedPedidos));
                }

                setCarrinho([]);
                setIsCarrinhoOpen(false);
                setAbaAtiva('acompanhar');
            }


        } catch (err) {
            const errorMsg = err.message || "Houve um erro ao enviar seu pedido. Tente novamente.";
            toast.error(errorMsg);
        }
    };


    const renderContent = () => {
        if (abaAtiva === 'cardapio') {
            return (
                <main className="max-w-6xl mx-auto p-4 md:p-6 space-y-8 pb-32">
                    <div className="flex gap-3 overflow-x-auto pb-2">
                        {cardapio.categorias.map(categoria => (
                            <a href={`#${categoria.nome}`} key={categoria.nome} className="px-5 py-2 rounded-lg font-semibold text-sm whitespace-nowrap transition-colors bg-white dark:bg-tema-surface-dark text-gray-700 dark:text-gray-300 shadow-sm border dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700">
                                {categoria.nome}
                            </a>
                        ))}
                    </div>

                    {cardapio.categorias.map(categoria => (
                        categoria.produtos.length > 0 && (
                            <section id={categoria.nome} key={categoria.nome}>
                                <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">{categoria.nome}</h2>
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                    {categoria.produtos.map(produto => (
                                        <div 
                                            key={produto.id} 
                                            onClick={() => setProdutoSelecionado(produto)} 
                                            className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-sm border dark:border-gray-700 flex gap-4 cursor-pointer hover:border-tema-primary"
                                        >
                                            <div className="flex-grow">
                                                <h3 className="font-semibold text-tema-text dark:text-tema-text-dark">{produto.nome}</h3>
                                                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mt-1">{produto.descricao}</p>
                                                <p className="font-semibold text-tema-text dark:text-tema-text-dark mt-2">R$ {produto.preco.toFixed(2).replace('.', ',')}</p>
                                            </div>
                                            {/* Exibir Imagem do Produto no Cardápio */}
                                            <div className="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-md flex-shrink-0 flex items-center justify-center overflow-hidden">
                                                {produto.imageUrl ? (
                                                     <img src={produto.imageUrl} alt={produto.nome} className="w-full h-full object-cover" />
                                                ) : (
                                                    <span className="text-xs text-gray-400">Imagem</span>
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
                                <div key={p.uuid} className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 flex justify-between items-center">
                                    <div>
                                        <p className="font-bold text-lg text-tema-text dark:text-tema-text-dark">Pedido #{pedidosDoDia.length - index} <span className="text-sm font-normal text-tema-text-muted dark:text-tema-text-muted-dark">({p.data})</span></p>
                                        <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">Total: R$ {p.total.toFixed(2).replace('.', ',')}</p>
                                        <ul className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark list-disc list-inside pl-2">
                                            {p.itens.map((item, i) => <li key={i} className="truncate max-w-[200px]">{item}</li>)}
                                        </ul>
                                    </div>
                                    <button 
                                        onClick={() => navigate(`/rastrear/${p.uuid}`)}
                                        className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors text-sm"
                                    >
                                        Rastrear
                                    </button>
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


    if (loading) return <div className="text-center p-10 text-lg font-semibold text-gray-500">Carregando cardápio...</div>;
    if (error) return <div className="text-center p-10 text-lg font-semibold text-red-500">{error}</div>;

    return (
        <div className="w-full min-h-screen bg-gray-100 dark:bg-tema-fundo-dark">
            <header className="bg-white dark:bg-tema-surface-dark shadow-md p-4 sticky top-0 z-20">
                <div className="max-w-6xl mx-auto">
                    {/* Exibir Logo do Restaurante se existir */}
                    {cardapio.logoUrl ? (
                         <img src={cardapio.logoUrl} alt={cardapio.nomeRestaurante} className="h-10 mb-2 object-contain mx-auto md:mx-0" />
                    ) : (
                        <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">{cardapio.nomeRestaurante}</h1>
                    )}
                    <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">{cardapio.enderecoRestaurante}</p>
                    
                    <div className="flex gap-4 mt-3 border-b dark:border-gray-700">
                        <button 
                            onClick={() => setAbaAtiva('cardapio')} 
                            className={`pb-2 font-semibold text-lg transition-colors ${abaAtiva === 'cardapio' ? 'text-tema-primary border-b-2 border-tema-primary dark:text-tema-link-dark dark:border-tema-link-dark' : 'text-tema-text-muted dark:text-tema-text-muted-dark hover:text-tema-primary dark:hover:text-tema-link-dark'}`}
                        >
                            Cardápio
                        </button>
                        <button 
                            onClick={() => setAbaAtiva('acompanhar')} 
                            className={`pb-2 font-semibold text-lg transition-colors ${abaAtiva === 'acompanhar' ? 'text-tema-primary border-b-2 border-tema-primary dark:text-tema-link-dark dark:border-tema-link-dark' : 'text-tema-text-muted dark:text-tema-text-muted-dark hover:text-tema-primary dark:hover:text-tema-link-dark'}`}
                        >
                            Acompanhar Pedido ({pedidosDoDia.length})
                        </button>
                    </div>

                </div>
            </header>

            {renderContent()}

            {produtoSelecionado && <ProdutoModal produto={produtoSelecionado} onClose={() => setProdutoSelecionado(null)} onAddToCart={handleAddToCart} />}
            
            {/* Footer flutuante (apenas se a aba Cardápio estiver ativa e tiver itens) */}
            {carrinho.length > 0 && abaAtiva === 'cardapio' && (
                <div className="fixed bottom-0 left-0 right-0 bg-white dark:bg-tema-surface-dark shadow-lg border-t dark:border-gray-700 p-4 z-30">
                    <div className="max-w-6xl mx-auto">
                        <button onClick={() => setIsCarrinhoOpen(true)} className="w-full bg-tema-primary text-white font-bold py-3 rounded-lg flex justify-between items-center px-4">
                            <span>Ver Sacola</span>
                            <span>{totalItensCarrinho} itens - R$ {subtotalCarrinho.toFixed(2).replace('.', ',')}</span>
                        </button>
                    </div>
                </div>
            )}

            {isCarrinhoOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-60 z-40" onClick={() => setIsCarrinhoOpen(false)}>
                    <div className="absolute bottom-0 left-0 right-0 bg-white dark:bg-tema-surface-dark p-6 rounded-t-2xl max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
                        <h2 className="text-2xl font-bold mb-4 dark:text-tema-text-dark">Finalizar Pedido</h2>
                        <div className="space-y-2 max-h-40 overflow-y-auto mb-4 border-b pb-4 dark:border-gray-700">
                            {carrinho.map(item => <p key={item.itemId} className="dark:text-tema-text-dark">{item.quantidade}x {item.nome} {item.observacao && `(${item.observacao})`}</p>)}
                        </div>
                        
                        {/* SEÇÃO DE FRETE CONDICIONAL */}
                        <div className="mt-6">
                            <label className="block text-sm font-bold text-tema-text dark:text-tema-text-dark mb-2">
                                {/* CORREÇÃO NO RÓTULO */}
                                {isHaversineAtivo ? 'CEP para Cálculo (Haversine)' : 'CEP para Entrega (Apenas Registro)'}
                            </label>
                            <div className="flex gap-2">
                                <input 
                                    type="text" 
                                    name="cepCliente"
                                    value={dadosCliente.cepCliente || ''} 
                                    onChange={handleInputChange} 
                                    placeholder={isHaversineAtivo ? "Aguardando 8 dígitos para calcular" : "Opcional"}
                                    maxLength={8}
                                    className="w-full p-3 border rounded-lg" 
                                />
                                {/* CORREÇÃO: Renderiza o botão 'Calcular' APENAS se isHaversineAtivo for TRUE */}
                                {isHaversineAtivo && (
                                     <button
                                         type="button"
                                         onClick={buscarFrete}
                                         disabled={!dadosCliente.cepCliente || dadosCliente.cepCliente.replace(/\D/g, '').length !== 8 || statusFrete.includes('Calculando')}
                                         className="bg-tema-primary text-white font-bold px-4 py-2 rounded-lg hover:bg-opacity-80 transition-colors disabled:bg-gray-400"
                                     >
                                         Calcular
                                     </button>
                                )}
                            </div>
                            <p className={`text-sm mt-2 font-semibold ${isHaversineAtivo && statusFrete.includes('Entrega') && taxaEntrega >= 0 ? 'text-tema-success dark:text-green-400' : 'text-tema-text-muted dark:text-tema-text-muted-dark'}`}>
                                {/* CORREÇÃO: Mostra o status/taxa conforme o modo ativo */}
                                {isHaversineAtivo ? `Status: ${statusFrete}` : `Taxa Fixa: R$ ${taxaEntrega.toFixed(2).replace('.', ',')}`}
                            </p>
                        </div>
                        
                        {/* DETALHES DE PREÇO */}
                        <div className="text-sm dark:text-tema-text-dark mt-4 pt-2 border-t dark:border-gray-700">
                            <div className="flex justify-between">
                                <span>Subtotal:</span>
                                <span>R$ {subtotalCarrinho.toFixed(2).replace('.',',')}</span>
                            </div>
                            
                            {/* LINHA DE TAXA DE ENTREGA */}
                            {taxaEntrega >= 0 && (
                                <div className={`flex justify-between text-lg pt-1 font-bold ${taxaEntrega > 0 ? 'text-gray-500' : 'text-tema-success'}`}>
                                    <span>Taxa de Entrega:</span>
                                    <span>R$ {taxaEntrega.toFixed(2).replace('.',',')}</span>
                                </div>
                            )}
                            {/* Mensagem de Erro de Cálculo/Bloqueio (Exclusiva Haversine) */}
                            {isHaversineAtivo && taxaEntrega < 0 && (
                                <div className={`flex justify-between text-lg pt-1 font-bold text-red-500 dark:text-red-400`}>
                                    <span>Taxa de Entrega:</span>
                                    <span>R$ 0,00 (Indisponível)</span>
                                </div>
                            )}
                        </div>
                        
                        {/* TOTAL GERAL SEMPRE NO FINAL */}
                        <p className="text-xl font-bold mt-4 text-right dark:text-tema-text-dark border-t dark:border-gray-700 pt-2">Total Geral: R$ {totalCarrinho.toFixed(2).replace('.',',')}</p>
                        
                        <div className="mt-6 space-y-4">
                            <input type="text" name="nomeCliente" value={dadosCliente.nomeCliente} onChange={handleInputChange} placeholder="* Seu Nome" className="w-full p-3 border rounded-lg" />
                            <input type="text" name="telefoneCliente" value={dadosCliente.telefoneCliente} onChange={handleInputChange} placeholder="* Telefone / WhatsApp" className="w-full p-3 border rounded-lg" />
                            <input type="text" name="enderecoCliente" value={dadosCliente.enderecoCliente} onChange={handleInputChange} placeholder="* Endereço para Entrega" className="w-full p-3 border rounded-lg" />
                            <input type="text" name="pontoReferencia" value={dadosCliente.pontoReferencia} onChange={handleInputChange} placeholder="Ponto de referência (opcional)" className="w-full p-3 border rounded-lg" />
                            
                            {/* SELETOR DE PAGAMENTO */}
                            <div className="mt-6">
                                <label className="block text-sm font-bold text-tema-text dark:text-tema-text-dark mb-2">Forma de Pagamento:</label>
                                <select
                                    value={tipoPagamentoSelecionado}
                                    onChange={(e) => setTipoPagamentoSelecionado(e.target.value)}
                                    className="w-full p-3 border rounded-lg bg-white dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark"
                                >
                                    <option value="ONLINE">💳 PIX ou Cartão AGORA</option>
                                    <option value="DINHEIRO">💰 Dinheiro na Entrega</option>
                                    <option value="CARTAO_DEBITO">📳 Cartão/Maquineta (Débito)</option>
                                    <option value="CARTAO_CREDITO">📳 Cartão/Maquineta (Crédito)</option>
                                    <option value="PIX">Pix na Entrega (Informar Chave)</option>
                                </select>
                            </div>
                        </div>
                        <button 
                            onClick={handleEnviarPedido} 
                            disabled={totalCarrinho <= 0 || (isHaversineAtivo && taxaEntrega < 0) || (isHaversineAtivo && statusFrete.includes('Aguardando'))} 
                            className="w-full mt-6 py-3 rounded-lg font-bold text-white bg-green-600 hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                        >
                            {/* TEXTO CONDICIONAL */}
                            {tipoPagamentoSelecionado === 'ONLINE' ? 'Pagar e Finalizar Agora' : 'Enviar Pedido (Pagar na Entrega)'}
                        </button>
                    </div>
                </div>
            )}
            <ToastContainer position="bottom-center" autoClose={3000} />
        </div>
    );
};

export default CardapioClientePage;