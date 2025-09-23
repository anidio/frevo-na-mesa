import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { toast, ToastContainer } from 'react-toastify';

// --- Componente do Modal do Produto ---
const ProdutoModal = ({ produto, onClose, onAddToCart }) => {
    const [quantidade, setQuantidade] = useState(1);
    const [observacao, setObservacao] = useState('');

    const handleAddItem = () => {
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
                    <textarea value={observacao} onChange={(e) => setObservacao(e.target.value)} placeholder="Alguma observação? Ex: sem cebola, ponto da carne, etc." className="w-full p-2 border rounded-lg dark:bg-gray-800 dark:border-gray-600 text-sm" rows="2"></textarea>
                    <div className="flex justify-between items-center mt-6">
                        <div className="flex items-center gap-3">
                            <button onClick={() => setQuantidade(q => Math.max(1, q - 1))} className="bg-gray-200 dark:bg-gray-700 rounded-full w-10 h-10 font-bold text-xl">-</button>
                            <span className="w-10 text-center font-bold text-xl">{quantidade}</span>
                            <button onClick={() => setQuantidade(q => q + 1)} className="bg-gray-200 dark:bg-gray-700 rounded-full w-10 h-10 font-bold text-xl">+</button>
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
    const navigate = useNavigate();

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

    const handleAddToCart = (produto, quantidade, observacao) => {
        const itemId = `${produto.id}-${observacao}`;
        setCarrinho(prev => {
            const itemExistente = prev.find(item => item.id === itemId);
            if (itemExistente) {
                return prev.map(item => item.id === itemId ? { ...item, quantidade: item.quantidade + quantidade } : item);
            }
            return [...prev, { ...produto, id: itemId, quantidade, observacao }];
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
        const pedidoParaApi = {
            restauranteId,
            ...dadosCliente,
            itens: carrinho.map(item => ({
                produtoId: item.id.split('-')[0], // CORREÇÃO: Extrai o ID real do produto
                quantidade: item.quantidade,
                observacao: item.observacao,
                adicionaisIds: [],
            }))
        };
        try {
            const novoPedido = await apiClient.post('/api/publico/pedido/delivery', pedidoParaApi);
            toast.success("Pedido enviado com sucesso! Acompanhe as novidades pelo WhatsApp.");
            setCarrinho([]);
            setIsCarrinhoOpen(false);
            if (novoPedido && novoPedido.uuid) {
                navigate(`/rastrear/${novoPedido.uuid}`);
            }
        } catch (err) {
            toast.error("Houve um erro ao enviar seu pedido. Tente novamente.");
        }
    };

    if (loading) return <div className="text-center p-10 text-lg font-semibold text-gray-500">Carregando cardápio...</div>;
    if (error) return <div className="text-center p-10 text-lg font-semibold text-red-500">{error}</div>;

    return (
        <div className="w-full min-h-screen bg-gray-100 dark:bg-tema-fundo-dark">
            <header className="bg-white dark:bg-tema-surface-dark shadow-md p-4 sticky top-0 z-20">
                <div className="max-w-6xl mx-auto">
                    <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">{cardapio.nomeRestaurante}</h1>
                    <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark">{cardapio.enderecoRestaurante}</p>
                </div>
            </header>

            <main className="max-w-6xl mx-auto p-4 md:p-6 space-y-8 pb-32">
                <div className="flex gap-3 overflow-x-auto pb-2">
                    {cardapio.categorias.map(categoria => (
                        <a href={`#${categoria.nome}`} key={categoria.nome} className="px-5 py-2 rounded-lg font-semibold text-sm whitespace-nowrap transition-colors bg-white dark:bg-tema-surface-dark text-gray-700 dark:text-gray-300 shadow-sm border dark:border-gray-700">
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
                                    <div key={produto.id} onClick={() => setProdutoSelecionado(produto)} className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-sm border dark:border-gray-700 flex gap-4 cursor-pointer hover:border-tema-primary">
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

            {produtoSelecionado && <ProdutoModal produto={produtoSelecionado} onClose={() => setProdutoSelecionado(null)} onAddToCart={handleAddToCart} />}
            
            {carrinho.length > 0 && (
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
                        <h2 className="text-2xl font-bold mb-4">Finalizar Pedido</h2>
                        <div className="space-y-2 max-h-40 overflow-y-auto mb-4 border-b pb-4 dark:border-gray-700">
                            {carrinho.map(item => <p key={item.id}>{item.quantidade}x {item.nome} {item.observacao && `(${item.observacao})`}</p>)}
                        </div>
                        <p className="text-xl font-bold mt-4 text-right">Total: R$ {totalCarrinho.toFixed(2).replace('.',',')}</p>
                        <div className="mt-6 space-y-4">
                            <input type="text" name="nomeCliente" value={dadosCliente.nomeCliente} onChange={handleInputChange} placeholder="* Seu Nome" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600" />
                            <input type="text" name="telefoneCliente" value={dadosCliente.telefoneCliente} onChange={handleInputChange} placeholder="* Telefone / WhatsApp" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600" />
                            <input type="text" name="enderecoCliente" value={dadosCliente.enderecoCliente} onChange={handleInputChange} placeholder="* Endereço para Entrega" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600" />
                            {/* NOVO CAMPO */}
                            <input type="text" name="pontoReferencia" value={dadosCliente.pontoReferencia} onChange={handleInputChange} placeholder="Ponto de referência (opcional)" className="w-full p-3 border rounded-lg dark:bg-gray-800 dark:border-gray-600" />
                        </div>
                        <button onClick={handleEnviarPedido} className="w-full mt-6 py-3 rounded-lg font-bold text-white bg-green-600 hover:bg-green-700">
                            Enviar Pedido
                        </button>
                    </div>
                </div>
            )}
            <ToastContainer position="bottom-center" autoClose={3000} />
        </div>
    );
};

export default CardapioClientePage;