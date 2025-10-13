import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext'; 
import UpgradeModal from '../components/UpgradeModal'; 

const ProdutoCard = ({ produto, onAdicionar }) => {
    return (
        // A estrutura principal agora é uma coluna flexível que distribui o espaço
        <div className="bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-md flex flex-col justify-between min-h-[140px] border border-gray-200 dark:border-gray-700">
            {/* Seção do Texto */}
            <div>
                <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark">{produto.nome}</h3>
                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mt-1">{produto.descricao}</p>
            </div>

            {/* Seção de Ação (Preço e Botão) */}
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
    const { userProfile, refreshProfile } = useAuth(); 

    // Estado para o modal de upgrade
    const [isModalOpen, setIsModalOpen] = useState(false); 

    // Estado para os dados do cliente
    const [cliente, setCliente] = useState({
        nome: '',
        telefone: '',
        endereco: '',
        pontoReferencia: ''
    });
    // Estado para o cardápio completo
    const [cardapio, setCardapio] = useState([]);
    // Estado para o "carrinho" do pedido atual
    const [itensPedido, setItensPedido] = useState([]);
    // Estado para o termo de busca de produtos
    const [termoBusca, setTermoBusca] = useState('');
    
    // --- LÓGICA DE MONETIZAÇÃO (Limites para exibição de alerta) ---
    const LIMITE_PEDIDOS_GRATUITO = 5; 
    const pedidosAtuais = userProfile?.pedidosMesAtual || 0;
    const isPlanoGratuito = userProfile?.plano === 'GRATUITO'; 
    const isLegacyFree = userProfile?.isLegacyFree;
    const isLimiteAtingido = !isLegacyFree && isPlanoGratuito && pedidosAtuais >= LIMITE_PEDIDOS_GRATUITO;
    const pedidosRestantes = LIMITE_PEDIDOS_GRATUITO - pedidosAtuais;


    // Busca o cardápio quando a página carrega
    useEffect(() => {
        const fetchCardapio = async () => {
            try {
                const data = await apiClient.get('/api/produtos');
                setCardapio(data);
            } catch (error) {
                toast.error('Não foi possível carregar o cardápio.');
            }
        };
        fetchCardapio();
    }, []);

    // Atualiza os dados do cliente conforme o usuário digita
    const handleClienteChange = (e) => {
        const { name, value } = e.target;
        setCliente(prev => ({ ...prev, [name]: value }));
    };

    // Adiciona um item ao pedido (carrinho)
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
    
    // Remove um item do pedido
    const handleRemoverItem = (produtoId) => {
        setItensPedido(prevItens => prevItens.filter(item => item.id !== produtoId));
    };

    // Filtra o cardápio com base na busca
    const cardapioFiltrado = useMemo(() => {
        if (!termoBusca) return cardapio;
        return cardapio.filter(p => p.nome.toLowerCase().includes(termoBusca.toLowerCase()));
    }, [cardapio, termoBusca]);
    
    // Calcula o total do pedido
    const totalPedido = useMemo(() => {
        return itensPedido.reduce((acc, item) => acc + (item.preco * item.quantidade), 0);
    }, [itensPedido]);

    // Envia o pedido para o backend
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
                observacao: '' 
            }))
        };

        try {
            await apiClient.post('/api/pedidos/delivery', dadosDoPedido);
            toast.success('Pedido de delivery criado com sucesso! (Contador incrementado)');
            await refreshProfile(); 
            navigate('/delivery'); 
        } catch (error) {
            // Log para diagnóstico futuro
            console.error("ERRO CAPTURADO:", error);
            
            // Verifica a mensagem de erro: se o apiClient lançar "PEDIDO_LIMIT_REACHED" ou o erro 400
            const errorMsg = String(error.message || error);
    
            // VERIFICAÇÃO FINAL: Detecta o erro customizado OU o erro HTTP que ele gera.
            if (errorMsg.includes("PEDIDO_LIMIT_REACHED") || errorMsg.includes("400 Bad Request") || errorMsg.includes("400")) {
                toast.warn("Limite de pedidos atingido! Realize o pagamento ou upgrade.");
                setIsModalOpen(true); // ATIVA O MODAL
            } else {
                toast.error(errorMsg || 'Erro ao criar pedido de delivery.');
            }
        }
    };
    
    // CORREÇÃO: Classe de input unificada
    const inputClass = "mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark";


    return (
        <div className="w-full p-4 md:p-8 max-w-6xl mx-auto">
            {/* NOVO: ALERTA DE LIMITE E STATUS */}
            <div className="flex justify-end">
            <div className={`mb-6 p-3 rounded-lg font-semibold border text-sm w-full md:w-1/2 ${isLimiteAtingido ? 'bg-red-100 border-red-400 text-red-700' : (pedidosRestantes <= 5 && pedidosRestantes > 0 && !isLegacyFree ? 'bg-yellow-100 border-yellow-400 text-yellow-700' : 'hidden')}`}>
                {isLimiteAtingido ? (
                    <p className="font-bold">LIMITE ATINGIDO! Seu plano ({userProfile?.plano || 'GRATUITO'}) atingiu {LIMITE_PEDIDOS_GRATUITO} pedidos. Clique em Salvar Pedido para pagar o excedente.</p>
                ) : (
                    <p>ALERTA! Você tem apenas {pedidosRestantes} pedidos restantes no seu plano gratuito neste mês.</p>
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
                    {/* CORREÇÃO TEMA: Usando tema-surface-dark e border-gray-700 */}
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md border dark:border-gray-700">
                        <h2 className="text-xl font-semibold mb-4 text-tema-text dark:text-tema-text-dark">1. Dados do Cliente</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Nome*</label>
                                <input type="text" name="nome" value={cliente.nome} onChange={handleClienteChange} required className={`${inputClass} border-gray-300`} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Telefone*</label>
                                <input type="text" name="telefone" value={cliente.telefone} onChange={handleClienteChange} required className={`${inputClass} border-gray-300`} />
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Endereço de Entrega</label>
                                <input type="text" name="endereco" value={cliente.endereco} onChange={handleClienteChange} className={`${inputClass} border-gray-300`} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Ponto de Referência (Opcional)</label>
                                <input type="text" name="pontoReferencia" value={cliente.pontoReferencia} onChange={handleClienteChange} className={`${inputClass} border-gray-300`} />
                            </div>
                        </div>
                    </div>
                    {/* CORREÇÃO TEMA: Usando tema-surface-dark e border-gray-700 */}
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md border dark:border-gray-700">
                        <h2 className="text-xl font-semibold mb-4 text-tema-text dark:text-tema-text-dark">2. Adicionar Itens</h2>
                        <input type="text" placeholder="🔎 Buscar no cardápio..." value={termoBusca} onChange={e => setTermoBusca(e.target.value)} className={`${inputClass} p-3 mb-4`} />
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-96 overflow-y-auto pr-2">
                            {cardapioFiltrado.map(produto => (
                                <ProdutoCard key={produto.id} produto={produto} onAdicionar={handleAdicionarItem} />
                            ))}
                        </div>
                    </div>
                </div>

                {/* Coluna da Direita: Resumo do Pedido */}
                <div className="lg:col-span-1">
                    {/* CORREÇÃO TEMA: Usando tema-surface-dark e border-gray-700 */}
                    <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md sticky top-8 border dark:border-gray-700">
                        <h2 className="text-xl font-semibold mb-4 text-tema-text dark:text-tema-text-dark">3. Resumo do Pedido</h2>
                        <div className="space-y-2 mb-4 text-tema-text dark:text-tema-text-dark">
                            {itensPedido.length > 0 ? (
                                itensPedido.map(item => (
                                    <div key={item.id} className="flex justify-between items-center text-sm">
                                        <span>{item.quantidade}x {item.nome}</span>
                                        <div className='flex items-center gap-2'>
                                            <span className='font-semibold'>R$ {(item.preco * item.quantidade).toFixed(2).replace('.', ',')}</span>
                                            <button type="button" onClick={() => handleRemoverItem(item.id)} className="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-500">✖</button>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <p className="text-tema-text-muted dark:text-tema-text-muted-dark">Nenhum item adicionado.</p>
                            )}
                        </div>
                        <div className="border-t dark:border-gray-700 pt-4">
                            <div className="flex justify-between font-bold text-lg text-tema-text dark:text-tema-text-dark">
                                <span>Total:</span>
                                <span>R$ {totalPedido.toFixed(2).replace('.', ',')}</span>
                            </div>
                        </div>
                        <button type="submit" className="w-full mt-6 bg-green-600 text-white font-bold py-3 rounded-lg hover:bg-green-700 transition-colors">
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
                />
            )}
        </div>
    );
};

export default NovoPedidoDeliveryPage;