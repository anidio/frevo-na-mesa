import React, { useState, useEffect, useMemo } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';

// Componente para o Modal de Categoria (Adicionar/Editar)
const CategoriaModal = ({ categoria, onClose, onSave }) => {
    const [nome, setNome] = useState(categoria ? categoria.nome : '');

    const handleSave = () => {
        if (!nome.trim()) {
            toast.warn('O nome da categoria não pode ser vazio.');
            return;
        }
        onSave({ ...categoria, nome });
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
            {/* CORREÇÃO: Fundo e borda do modal principal */}
            <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-sm border dark:border-gray-700">
                <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">
                    {categoria ? 'Editar Categoria' : 'Nova Categoria'}
                </h2>
                <div>
                    {/* CORREÇÃO: Cor do Label */}
                    <label className="block text-sm font-medium text-gray-700 dark:text-tema-text-muted-dark">Nome da Categoria</label>
                    <input
                        type="text"
                        value={nome}
                        onChange={(e) => setNome(e.target.value)}
                        // CORREÇÃO CRÍTICA: Cor do Input
                        className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark"
                        autoFocus
                    />
                </div>
                <div className="flex justify-end gap-4 pt-6">
                    <button onClick={onClose} className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">Cancelar</button>
                    <button onClick={handleSave} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">Salvar</button>
                </div>
            </div>
        </div>
    );
};

// Componente principal
const GerenciarCardapioPage = () => {
    const [cardapio, setCardapio] = useState([]);
    const [categorias, setCategorias] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filtroCategoria, setFiltroCategoria] = useState('Todos');
    
    // Estados para os modais
    const [isProdutoModalOpen, setIsProdutoModalOpen] = useState(false);
    const [produtoEmEdicao, setProdutoEmEdicao] = useState(null);
    const [isCategoriaModalOpen, setIsCategoriaModalOpen] = useState(false);
    const [categoriaEmEdicao, setCategoriaEmEdicao] = useState(null);

    // ALTERADO: Adicionado imageUrl ao estado inicial
    const [formState, setFormState] = useState({
        nome: '', descricao: '', preco: '', categoriaId: '', imageUrl: ''
    });

    const fetchData = async () => {
        setLoading(true);
        try {
            const [produtosData, categoriasData] = await Promise.all([
                apiClient.get('/api/produtos'),
                apiClient.get('/api/categorias')
            ]);
            setCardapio(produtosData);
            setCategorias(categoriasData);
        } catch (error) {
            toast.error("Não foi possível carregar os dados.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    // --- Lógica para Produtos ---
    const handleDeletarProduto = (produtoId, produtoNome) => {
        if (!window.confirm(`Tem certeza que deseja deletar o produto "${produtoNome}"?`)) return;
        apiClient.delete(`/api/produtos/${produtoId}`)
            .then(() => {
                toast.success(`"${produtoNome}" foi deletado com sucesso!`);
                fetchData();
            })
            .catch(error => toast.error(`Erro ao deletar: ${error.message}`));
    };

    const handleOpenProdutoModal = (produto = null) => {
        if (produto) {
            setProdutoEmEdicao(produto);
            // ALTERADO: Pega imageUrl do produto
            setFormState({ ...produto, categoriaId: produto.categoria.id, imageUrl: produto.imageUrl || '' });
        } else {
            setProdutoEmEdicao(null);
            // ALTERADO: Adicionado imageUrl ao estado padrão
            setFormState({ nome: '', descricao: '', preco: '', categoriaId: categorias[0]?.id || '', imageUrl: '' });
        }
        setIsProdutoModalOpen(true);
    };

    const handleSalvarProduto = async () => {
        if (!formState.nome || !formState.preco || !formState.categoriaId) {
            toast.warn('Nome, Preço e Categoria são obrigatórios.');
            return;
        }

        const endpoint = produtoEmEdicao ? `/api/produtos/${produtoEmEdicao.id}` : '/api/produtos';
        const method = produtoEmEdicao ? 'put' : 'post';

        // NOVO: Garantir que a URL da imagem não é nula
        const dataToSend = { ...formState, imageUrl: formState.imageUrl || null };
        
        try {
            await apiClient[method](endpoint, dataToSend);
            toast.success(`Produto ${produtoEmEdicao ? 'atualizado' : 'adicionado'} com sucesso!`);
            setIsProdutoModalOpen(false);
            fetchData();
        } catch (error) {
            toast.error(`Erro ao salvar produto: ${error.message}`);
        }
    };

    // --- Lógica para Categorias ---
    const handleOpenCategoriaModal = (categoria = null) => {
        setCategoriaEmEdicao(categoria);
        setIsCategoriaModalOpen(true);
    };

    const handleSalvarCategoria = async (categoria) => {
        const endpoint = categoria.id ? `/api/categorias/${categoria.id}` : '/api/categorias';
        const method = categoria.id ? 'put' : 'post';

        try {
            await apiClient[method](endpoint, { nome: categoria.nome });
            toast.success(`Categoria ${categoria.id ? 'atualizada' : 'adicionada'} com sucesso!`);
            setIsCategoriaModalOpen(false);
            fetchData();
        } catch (error) {
            toast.error(`Erro ao salvar categoria: ${error.message}`);
        }
    };

    const handleDeletarCategoria = (categoriaId, categoriaNome) => {
        if (!window.confirm(`Tem certeza que deseja deletar a categoria "${categoriaNome}"?`)) return;
        apiClient.delete(`/api/categorias/${categoriaId}`)
            .then(() => {
                toast.success(`Categoria "${categoriaNome}" deletada com sucesso!`);
                setFiltroCategoria('Todos'); // Reseta o filtro
                fetchData();
            })
            .catch(error => toast.error(`Erro ao deletar: ${error.message}`));
    };

    const cardapioFiltrado = useMemo(() => {
        if (filtroCategoria === 'Todos') return cardapio;
        // @ts-ignore
        return cardapio.filter(produto => produto.categoria.id === filtroCategoria);
    }, [cardapio, filtroCategoria]);
    
    // ...

    if (loading) return <div className="p-8 text-center font-semibold text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</div>;

    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-8">
                {/* Seção de Gerenciar Categorias */}
                <div>
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">Categorias</h2>
                        <button onClick={() => handleOpenCategoriaModal()} className="bg-gray-200 dark:bg-gray-700 text-sm text-tema-text dark:text-tema-text-dark font-bold py-2 px-3 rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors">
                            + Nova Categoria
                        </button>
                    </div>
                    <div className="flex flex-wrap gap-2 p-4 bg-white dark:bg-tema-surface-dark rounded-lg border dark:border-gray-700">
                        {categorias.map(cat => (
                            <div key={cat.id} className="group flex items-center gap-1 bg-gray-100 dark:bg-gray-800 rounded-full text-sm font-semibold">
                                <span className="pl-3 pr-2 text-gray-700 dark:text-gray-300">{cat.nome}</span>
                                <button onClick={() => handleOpenCategoriaModal(cat)} className="p-1 text-gray-400 hover:text-blue-500 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.5L15.232 5.232z" /></svg>
                                </button>
                                <button onClick={() => handleDeletarCategoria(cat.id, cat.nome)} className="p-1 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity pr-2">
                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
                                </button>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Seção de Gerenciar Produtos */}
                <div>
                    <div className="flex justify-between items-center mb-4">
                        <h1 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">Produtos do Cardápio</h1>
                        <button onClick={() => handleOpenProdutoModal()} className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors flex items-center gap-2">
                            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" /></svg>
                            Adicionar Item
                        </button>
                    </div>

                    <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
                        <button onClick={() => setFiltroCategoria('Todos')} className={`px-4 py-2 rounded-full font-semibold text-sm whitespace-nowrap transition-colors ${filtroCategoria === 'Todos' ? 'bg-tema-primary text-white shadow' : 'bg-white dark:bg-tema-surface-dark text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 border dark:border-gray-700'}`}>Todos</button>
                        {categorias.map(cat => (<button key={cat.id} onClick={() => setFiltroCategoria(cat.id)} className={`px-4 py-2 rounded-full font-semibold text-sm whitespace-nowrap transition-colors ${filtroCategoria === cat.id ? 'bg-tema-primary text-white shadow' : 'bg-white dark:bg-tema-surface-dark text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 border dark:border-gray-700'}`}>{cat.nome}</button>))}
                    </div>

                    <div className="bg-white dark:bg-tema-surface-dark shadow-md rounded-lg overflow-x-auto border dark:border-gray-700">
                        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                            <thead className="bg-gray-50 dark:bg-gray-800">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Nome</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Categoria</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Preço</th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Ações</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                                {cardapioFiltrado.map(produto => (
                                    <tr key={produto.id} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">{produto.nome}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-300">{produto.categoria.nome}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-300">R$ {produto.preco.toFixed(2).replace('.', ',')}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-4">
                                            <button onClick={() => handleOpenProdutoModal(produto)} className="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 font-semibold">Editar</button>
                                            <button onClick={() => handleDeletarProduto(produto.id, produto.nome)} className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 font-semibold">Deletar</button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {isProdutoModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
                    {/* Fundo do modal */}
                    <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-md space-y-4">
                        <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark">{produtoEmEdicao ? 'Editar Item' : 'Novo Item'}</h2>
                        
                        {/* INPUT NOME */}
                        <div><label className="block text-sm font-medium text-gray-700 dark:text-tema-text-muted-dark">Nome</label>
                            <input type="text" name="nome" value={formState.nome} onChange={e => setFormState({...formState, nome: e.target.value})} 
                                className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark" />
                        </div>
                        
                        {/* INPUT DESCRIÇÃO */}
                        <div><label className="block text-sm font-medium text-gray-700 dark:text-tema-text-muted-dark">Descrição</label>
                            <input type="text" name="descricao" value={formState.descricao} onChange={e => setFormState({...formState, descricao: e.target.value})} 
                                className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark" />
                        </div>

                        {/* NOVO INPUT URL IMAGEM */}
                        <div><label className="block text-sm font-medium text-gray-700 dark:text-tema-text-muted-dark">URL da Imagem</label>
                            <input type="text" name="imageUrl" value={formState.imageUrl} onChange={e => setFormState({...formState, imageUrl: e.target.value})} 
                                className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark" />
                            {formState.imageUrl && <img src={formState.imageUrl} alt="Pré-visualização" className="mt-2 h-16 w-16 object-cover rounded" />}
                        </div>
                        
                        {/* INPUT PREÇO */}
                        <div><label className="block text-sm font-medium text-gray-700 dark:text-tema-text-muted-dark">Preço</label>
                            <input type="number" name="preco" value={formState.preco} onChange={e => setFormState({...formState, preco: e.target.value})} 
                                className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark" />
                        </div>
                        
                        {/* SELECT CATEGORIA */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-tema-text-muted-dark">Categoria</label>
                            <select name="categoriaId" value={formState.categoriaId} onChange={e => setFormState({...formState, categoriaId: e.target.value})} 
                                className="mt-1 w-full p-2 border rounded-md bg-white dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark">
                                {categorias.map(cat => <option key={cat.id} value={cat.id}>{cat.nome}</option>)}
                            </select>
                        </div>
                        
                        <div className="flex justify-end gap-4 pt-4">
                            <button onClick={() => setIsProdutoModalOpen(false)} className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">Cancelar</button>
                            <button onClick={handleSalvarProduto} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">Salvar</button>
                        </div>
                    </div>
                </div>
            )}

            {isCategoriaModalOpen && (
                <CategoriaModal 
                    categoria={categoriaEmEdicao} 
                    onClose={() => setIsCategoriaModalOpen(false)}
                    onSave={handleSalvarCategoria}
                />
            )}
        </>
    );
};

export default GerenciarCardapioPage;