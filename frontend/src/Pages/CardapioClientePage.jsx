import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { toast, ToastContainer } from 'react-toastify';

// --- Componente do Modal do Produto ---
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
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50" onClick={onClose}>
            <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl w-full max-w-md" onClick={e => e.stopPropagation()}>
                <div className="w-full h-48 bg-gray-200 dark:bg-gray-700 rounded-t-lg flex items-center justify-center">
                    <span className="text-gray-400">Imagem do Produto</span>
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
    const [dadosCliente, setDadosCliente] = useState({ nomeCliente: '', telefoneCliente: '', enderecoCliente: '', pontoReferencia: '' });
    const [produtoSelecionado, setProdutoSelecionado] = useState(null);
    const [abaAtiva, setAbaAtiva] = useState('cardapio'); 
    const [pedidosDoDia, setPedidosDoDia] = useState([]); 
    const navigate = useNavigate();

    // Busca o cardápio público
    useEffect(() => {
        const fetchCardapio = async () => {
            try {
                const data = await apiClient.get(`/api/publico/cardapio/${restauranteId}`);
                setCardapio(data);
            } catch (err) {
                setError("Cardápio não encontrado ou indisponível.");
            } finally {
                setLoading(false);
            }
        };
        fetchCardapio();
    }, [restauranteId]);
    
    // NOVO: Carrega pedidos do dia do localStorage na inicialização
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
        setDadosCliente(prev => ({ ...prev, [name]: value }));
    };

    const totalCarrinho = useMemo(() => {
        return carrinho.reduce((acc, item) => acc + (item.preco * item.quantidade), 0);
    }, [carrinho]);

    const totalItensCarrinho = useMemo(() => {
        return carrinho.reduce((acc, item) => acc + item.quantidade, 0);
    }, [carrinho]);

    const handleEnviarPedido = async () => {
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


        const pedidoParaApi = {
            restauranteId: Number(restauranteId), // Garante que o ID seja um número
            ...dadosCliente,
            itens: carrinho.map(item => ({
                produtoId: item.produtoId, 
                quantidade: item.quantidade,
                observacao: item.observacao,
                adicionaisIds: [], // Placeholder (Adicionais não estão implementados no DTO)
            }))
        };
        
        try {
            // CHAMADA AO NOVO ENDPOINT DE PAGAMENTO
            const response = await apiClient.post('/api/publico/pagar/delivery', pedidoParaApi);
            const { paymentUrl } = response;
            
            toast.info("Redirecionando para o pagamento seguro...");
            
            // O pedido é salvo no banco em status AGUARDANDO_PGTO_LIMITE e será confirmado pelo webhook.
            // Aqui, salvamos um placeholder para o rastreamento, já que o UUID é gerado no backend antes de ir para o MP.
            const placeholderPedido = { 
                uuid: 'pending', // Usamos um placeholder, o rastreio real virá por email
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

        } catch (err) {
            const errorMsg = err.message || "Houve um erro ao iniciar o pagamento.";
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
                                            <div className="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-md flex-shrink-0 flex items-center justify-center">
                                                <span className="text-xs text-gray-400">Imagem</span>
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
    }


    if (loading) return <div className="text-center p-10 text-lg font-semibold text-gray-500">Carregando cardápio...</div>;
    if (error) return <div className="text-center p-10 text-lg font-semibold text-red-500">{error}</div>;

    return (
        <div className="w-full min-h-screen bg-gray-100 dark:bg-tema-fundo-dark">
            <header className="bg-white dark:bg-tema-surface-dark shadow-md p-4 sticky top-0 z-20">
                <div className="max-w-6xl mx-auto">
                    <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">{cardapio.nomeRestaurante}</h1>
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
                            <span>{totalItensCarrinho} itens - R$ {totalCarrinho.toFixed(2).replace('.', ',')}</span>
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
                        <p className="text-xl font-bold mt-4 text-right dark:text-tema-text-dark">Total: R$ {totalCarrinho.toFixed(2).replace('.',',')}</p>
                        <div className="mt-6 space-y-4">
                            <input type="text" name="nomeCliente" value={dadosCliente.nomeCliente} onChange={handleInputChange} placeholder="* Seu Nome" className="w-full p-3 border rounded-lg" />
                            <input type="text" name="telefoneCliente" value={dadosCliente.telefoneCliente} onChange={handleInputChange} placeholder="* Telefone / WhatsApp" className="w-full p-3 border rounded-lg" />
                            <input type="text" name="enderecoCliente" value={dadosCliente.enderecoCliente} onChange={handleInputChange} placeholder="* Endereço para Entrega" className="w-full p-3 border rounded-lg" />
                            <input type="text" name="pontoReferencia" value={dadosCliente.pontoReferencia} onChange={handleInputChange} placeholder="Ponto de referência (opcional)" className="w-full p-3 border rounded-lg" />
                        </div>
                        <button onClick={handleEnviarPedido} className="w-full mt-6 py-3 rounded-lg font-bold text-white bg-green-600 hover:bg-green-700">
                            Pagar Pedido com PIX/Cartão
                        </button>
                    </div>
                </div>
            )}
            <ToastContainer position="bottom-center" autoClose={3000} />
        </div>
    );
};

export default CardapioClientePage;