import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';
import UpgradeModal from '../components/UpgradeModal';

// Componente ProdutoCard (sem alterações)
const ProdutoCard = ({ produto, onAdicionar }) => {
    return (
        <div className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-md flex flex-col justify-between min-h-[140px] border border-gray-200 dark:border-gray-700">
            <div>
                <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark">{produto.nome}</h3>
                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mt-1">{produto.descricao}</p>
            </div>
            <div className="flex items-center justify-between mt-4">
                <p className="text-lg font-semibold text-tema-text dark:text-tema-text-dark">R$ {produto.preco.toFixed(2).replace('.', ',')}</p>
                <button
                    type="button"
                    onClick={() => onAdicionar(produto, 1)}
                    className="bg-tema-primary text-white font-bold py-2 px-3 rounded-lg hover:bg-opacity-80 transition-colors whitespace-nowrap"
                >
                    Adicionar
                </button>
            </div>
        </div>
    );
};

const NovoPedidoDeliveryPage = () => {
    const navigate = useNavigate();
    const { userProfile, refreshProfile, loadingProfile } = useAuth(); // Adicionado loadingProfile

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [cliente, setCliente] = useState({ nome: '', telefone: '', endereco: '', pontoReferencia: '' });
    const [cardapio, setCardapio] = useState([]);
    const [itensPedido, setItensPedido] = useState([]);
    const [termoBusca, setTermoBusca] = useState('');
    const [loadingCardapio, setLoadingCardapio] = useState(true); // Loading específico para cardápio

    // --- LÓGICA DE MONETIZAÇÃO ATUALIZADA ---
    const LIMITE_PEDIDOS_GRATUITO = userProfile?.limitePedidosGratuito || 5; // Usa o limite do backend se disponível
    const pedidosAtuais = userProfile?.pedidosMesAtual || 0;
    const isPlanoGratuito = userProfile?.plano === 'GRATUITO';
    const isLegacyFree = userProfile?.isLegacyFree;
    const isBetaTester = userProfile?.isBetaTester; // Considerar beta tester
    const isDeliveryPro = userProfile?.isDeliveryPro; // Flag principal

    // **AJUSTE 1:** Limite SÓ é Atingido se: NÃO for Pro, NÃO for Legacy, NÃO for Beta, for Gratuito E contador >= limite
    const aplicaLimite = !isDeliveryPro && !isLegacyFree && !isBetaTester && isPlanoGratuito;
    const isLimiteAtingido = aplicaLimite && (pedidosAtuais >= LIMITE_PEDIDOS_GRATUITO);
    // **AJUSTE 2:** Pedidos restantes só fazem sentido se o limite se aplica
    const pedidosRestantes = aplicaLimite ? Math.max(0, LIMITE_PEDIDOS_GRATUITO - pedidosAtuais) : Infinity; // Infinity se não aplica limite
    // **AJUSTE 3:** Condição para mostrar o alerta (limite atingido OU poucos restantes E limite se aplica)
    const mostrarAlertaLimite = aplicaLimite && (isLimiteAtingido || (pedidosRestantes <= 5 && pedidosRestantes >= 0));


    useEffect(() => {
        const fetchCardapio = async () => {
            setLoadingCardapio(true); // Inicia loading do cardápio
            try {
                // Busca produtos apenas se o usuário estiver carregado (evita chamadas desnecessárias)
                if (userProfile) {
                    const data = await apiClient.get('/api/produtos');
                    setCardapio(data);
                }
            } catch (error) {
                toast.error('Não foi possível carregar o cardápio.');
            } finally {
                 setLoadingCardapio(false); // Finaliza loading do cardápio
            }
        };
        // Roda fetchCardapio apenas quando userProfile estiver disponível (não mais em loading)
        if (!loadingProfile) {
             fetchCardapio();
        }
    }, [userProfile, loadingProfile]); // Depende de userProfile e loadingProfile

    // Funções handleClienteChange, handleAdicionarItem, handleRemoverItem permanecem iguais
     const handleClienteChange = (e) => {
        const { name, value } = e.target;
        setCliente(prev => ({ ...prev, [name]: value }));
    };
    const handleAdicionarItem = (produto, quantidade) => {
        setItensPedido(prevItens => {
            const itemExistente = prevItens.find(item => item.id === produto.id);
            if (itemExistente) {
                return prevItens.map(item =>
                    item.id === produto.id ? { ...item, quantidade: item.quantidade + quantidade } : item
                );
            }
            return [...prevItens, { ...produto, quantidade }];
        });
    };
    const handleRemoverItem = (produtoId) => {
        setItensPedido(prevItens => prevItens.filter(item => item.id !== produtoId));
    };


    const cardapioFiltrado = useMemo(() => {
        if (!termoBusca) return cardapio;
        return cardapio.filter(p => p.nome.toLowerCase().includes(termoBusca.toLowerCase()));
    }, [cardapio, termoBusca]);

    const totalPedido = useMemo(() => {
        return itensPedido.reduce((acc, item) => acc + (item.preco * item.quantidade), 0);
    }, [itensPedido]);

    // Função handleSubmit permanece igual na lógica de erro
     const handleSubmit = async (e) => {
        e.preventDefault();

        if (itensPedido.length === 0) {
            toast.warn('Adicione pelo menos um item ao pedido.');
            return;
        }
        if (!cliente.nome || !cliente.telefone) {
            toast.warn('Nome e Telefone do cliente são obrigatórios.');
            return;
        }

        const dadosDoPedido = {
            nomeCliente: cliente.nome,
            telefoneCliente: cliente.telefone,
            enderecoCliente: cliente.endereco,
            pontoReferencia: cliente.pontoReferencia,
            itens: itensPedido.map(item => ({
                produtoId: item.id,
                quantidade: item.quantidade,
                observacao: '' // Ou adicione um campo de observação se necessário
            }))
        };

        try {
            await apiClient.post('/api/pedidos/delivery', dadosDoPedido);
            toast.success('Pedido de delivery criado com sucesso!');
            // Atualiza o perfil para obter o novo contador 'pedidosMesAtual'
            await refreshProfile();
            navigate('/delivery');
        } catch (error) {
            console.error("ERRO AO CRIAR PEDIDO:", error);
            const errorMsg = String(error.message || error);

             // A lógica de detecção de erro e abertura do modal permanece a mesma
            if (errorMsg.includes("PEDIDO_LIMIT_REACHED") || errorMsg.includes("400")) {
                toast.warn("Limite de pedidos atingido! Realize o pagamento ou upgrade.");
                setIsModalOpen(true);
            } else {
                toast.error(errorMsg || 'Erro ao criar pedido de delivery.');
            }
        }
    };

    const inputClass = "mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark";

    // Mostra loading geral se o perfil ainda não carregou
    if (loadingProfile) {
        return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</div>;
    }

    // Se não for Delivery Pro OU Gratuito, exibe mensagem de bloqueio
    if (!isDeliveryPro && !isGratuito) {
         return (
             <div className="w-full p-4 md:p-8 max-w-6xl mx-auto text-center">
                 <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">Novo Pedido Delivery</h1>
                 <p className="text-tema-text-muted dark:text-tema-text-muted-dark mt-4">
                     Esta funcionalidade requer o plano Delivery PRO ou Premium.
                     <Link to="/admin/financeiro" className="text-tema-primary ml-2 hover:underline">Ver planos</Link>
                 </p>
                 <Link to="/delivery" className="mt-4 inline-block px-4 py-2 rounded-lg font-semibold border bg-gray-200 dark:bg-gray-700 text-tema-text dark:text-tema-text-dark hover:bg-gray-300 dark:hover:bg-gray-600">
                     Voltar ao Delivery
                 </Link>
            </div>
         );
    }

    return (
        <div className="w-full p-4 md:p-8 max-w-6xl mx-auto">
            {/* **AJUSTE 3:** Alerta de Limite usa a nova flag 'mostrarAlertaLimite' */}
            <div className="flex justify-end">
                <div className={`mb-6 p-3 rounded-lg font-semibold border text-sm w-full md:w-auto ${
                    mostrarAlertaLimite
                        ? isLimiteAtingido
                            ? 'bg-red-100 border-red-400 text-red-700'
                            : 'bg-yellow-100 border-yellow-400 text-yellow-700'
                        : 'hidden' // Esconde se não for para mostrar o alerta
                }`}>
                    {isLimiteAtingido ? (
                        <p className="font-bold">⚠️ LIMITE ATINGIDO! Seu plano ({userProfile?.plano}) atingiu {LIMITE_PEDIDOS_GRATUITO} pedidos. Clique em Salvar Pedido para opções.</p>
                    ) : (
                        <p>🔔 ALERTA! Você tem {pedidosRestantes} pedidos restantes no seu plano gratuito este mês.</p>
                    )}
                </div>
            </div>

            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Novo Pedido de Delivery</h1>
                <Link to="/delivery" className="px-4 py-2 rounded-lg font-semibold border bg-gray-200 dark:bg-gray-700 text-tema-text dark:text-tema-text-dark hover:bg-gray-300 dark:hover:bg-gray-600">
                    Voltar ao Delivery
                </Link>
            </div>

            <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Coluna da Esquerda: Dados do Cliente e Itens do Cardápio */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md border dark:border-gray-700">
                        <h2 className="text-xl font-semibold mb-4 text-tema-text dark:text-tema-text-dark">1. Dados do Cliente</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Nome*</label>
                                <input type="text" name="nome" value={cliente.nome} onChange={handleClienteChange} required className={`${inputClass} border-gray-300 dark:border-gray-600`} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Telefone*</label>
                                <input type="text" name="telefone" value={cliente.telefone} onChange={handleClienteChange} required className={`${inputClass} border-gray-300 dark:border-gray-600`} />
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Endereço de Entrega</label>
                                <input type="text" name="endereco" value={cliente.endereco} onChange={handleClienteChange} className={`${inputClass} border-gray-300 dark:border-gray-600`} />
                            </div>
                            <div className="md:col-span-2"> {/* Alterado para ocupar a linha toda */}
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Ponto de Referência (Opcional)</label>
                                <input type="text" name="pontoReferencia" value={cliente.pontoReferencia} onChange={handleClienteChange} className={`${inputClass} border-gray-300 dark:border-gray-600`} />
                            </div>
                        </div>
                    </div>
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md border dark:border-gray-700">
                        <h2 className="text-xl font-semibold mb-4 text-tema-text dark:text-tema-text-dark">2. Adicionar Itens</h2>
                        <input type="text" placeholder="🔎 Buscar no cardápio..." value={termoBusca} onChange={e => setTermoBusca(e.target.value)} className={`${inputClass} p-3 mb-4`} />
                        { loadingCardapio ? (
                             <p className="text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando cardápio...</p>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-96 overflow-y-auto pr-2">
                                {cardapioFiltrado.length > 0 ? cardapioFiltrado.map(produto => (
                                    <ProdutoCard key={produto.id} produto={produto} onAdicionar={handleAdicionarItem} />
                                )) : <p className="text-center text-tema-text-muted dark:text-tema-text-muted-dark md:col-span-2">Nenhum produto encontrado.</p>}
                            </div>
                        )}
                    </div>
                </div>

                {/* Coluna da Direita: Resumo do Pedido */}
                <div className="lg:col-span-1">
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md sticky top-8 border dark:border-gray-700">
                        <h2 className="text-xl font-semibold mb-4 text-tema-text dark:text-tema-text-dark">3. Resumo do Pedido</h2>
                        <div className="space-y-2 mb-4 text-tema-text dark:text-tema-text-dark max-h-60 overflow-y-auto pr-2"> {/* Add scroll */}
                            {itensPedido.length > 0 ? (
                                itensPedido.map(item => (
                                    <div key={item.id} className="flex justify-between items-center text-sm">
                                        <span>{item.quantidade}x {item.nome}</span>
                                        <div className='flex items-center gap-2'>
                                            <span className='font-semibold'>R$ {(item.preco * item.quantidade).toFixed(2).replace('.', ',')}</span>
                                            <button type="button" onClick={() => handleRemoverItem(item.id)} className="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-500 text-lg leading-none">&times;</button> {/* Ícone X menor */}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <p className="text-tema-text-muted dark:text-tema-text-muted-dark text-center py-4">Nenhum item adicionado.</p>
                            )}
                        </div>
                        <div className="border-t dark:border-gray-700 pt-4">
                            <div className="flex justify-between font-bold text-lg text-tema-text dark:text-tema-text-dark">
                                <span>Total:</span>
                                <span>R$ {totalPedido.toFixed(2).replace('.', ',')}</span>
                            </div>
                        </div>
                        <button
                            type="submit"
                            // O botão fica sempre ativo, a lógica de limite é tratada no handleSubmit
                            className="w-full mt-6 bg-green-600 text-white font-bold py-3 rounded-lg hover:bg-green-700 transition-colors"
                        >
                            Salvar Pedido
                        </button>
                    </div>
                </div>
            </form>
            {/* Modal de Upgrade/Pagamento */}
            {isModalOpen && (
                <UpgradeModal
                    onClose={() => setIsModalOpen(false)}
                    limiteAtual={LIMITE_PEDIDOS_GRATUITO}
                    refreshProfile={refreshProfile}
                    // onPedidoAceito não é necessário aqui, pois o pedido ainda não foi criado
                />
            )}
        </div>
    );
};

export default NovoPedidoDeliveryPage;