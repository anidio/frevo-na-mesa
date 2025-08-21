// src/pages/GerenciarCardapioPage.jsx

import React, { useState, useEffect, useMemo } from 'react';
import { toast } from 'react-toastify';

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// --- COMPONENTES INTERNOS ---

// Componente customizado para a notificação de confirmação
const ConfirmacaoToast = ({ closeToast, onConfirm, mensagem }) => (
    <div>
        <p className="font-semibold">{mensagem}</p>
        <div className="flex justify-end gap-2 mt-2">
            <button
                onClick={() => { onConfirm(); closeToast(); }}
                className="bg-red-600 text-white px-3 py-1 rounded-md text-sm font-bold"
            >
                Sim, Deletar
            </button>
            <button
                onClick={closeToast}
                className="bg-gray-200 text-gray-700 px-3 py-1 rounded-md text-sm font-bold"
            >
                Não
            </button>
        </div>
    </div>
);

const categorias = ['Todos', 'Entradas', 'Pratos', 'Sobremesas', 'Bebidas'];

// --- COMPONENTE PRINCIPAL DA PÁGINA ---
const GerenciarCardapioPage = () => {
    const [cardapio, setCardapio] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filtroCategoria, setFiltroCategoria] = useState('Todos');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [produtoEmEdicao, setProdutoEmEdicao] = useState(null);
    const [formState, setFormState] = useState({
        nome: '', descricao: '', preco: '', categoria: 'Entradas'
    });

    const fetchData = async () => {
        setLoading(true);
        try {
            const response = await fetch('${API_URL}/api/produtos');
            if (response.ok) {
                const data = await response.json();
                setCardapio(data);
            }
        } catch (error) {
            console.error("Erro ao buscar cardápio:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleDeletar = (produtoId, produtoNome) => {
        const performDelete = async () => {
             try {
                const response = await fetch(`http://localhost:8080/api/produtos/${produtoId}`, {
                    method: 'DELETE',
                });
                if (response.ok) {
                    toast.success(`"${produtoNome}" foi deletado com sucesso!`);
                    fetchData();
                } else {
                    toast.error('Erro ao deletar o produto.');
                }
            } catch (error) {
                toast.error('Erro de comunicação com o servidor.');
            }
        };

        toast.warn(
            <ConfirmacaoToast 
                mensagem={`Deletar "${produtoNome}"?`}
                onConfirm={performDelete}
            />, 
            { autoClose: false, closeOnClick: false, draggable: false }
        );
    };

    const handleOpenModal = (produto = null) => {
        if (produto) {
            setProdutoEmEdicao(produto);
            setFormState(produto);
        } else {
            setProdutoEmEdicao(null);
            setFormState({ nome: '', descricao: '', preco: '', categoria: 'Entradas' });
        }
        setIsModalOpen(true);
    };
    
    const handleCloseModal = () => setIsModalOpen(false);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormState(prevState => ({ ...prevState, [name]: value }));
    };

    const handleSalvar = async () => {
        if (!formState.nome || !formState.preco || !formState.categoria) {
            toast.warn('Nome, Preço e Categoria são obrigatórios.');
            return;
        }
        const isEditing = produtoEmEdicao !== null;
        const url = isEditing ? `${API_URL}/api/produtos/${produtoEmEdicao.id}` : '${API_URL}/api/produtos';
        const method = isEditing ? 'PUT' : 'POST';
        try {
            const response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formState),
            });
            if (response.ok || response.status === 201) {
                toast.success(`Produto ${isEditing ? 'atualizado' : 'adicionado'} com sucesso!`);
                handleCloseModal();
                fetchData();
            } else {
                toast.error(`Erro ao ${isEditing ? 'atualizar' : 'adicionar'} produto.`);
            }
        } catch (error) {
            console.error('Erro de comunicação:', error);
            toast.error('Erro de comunicação com o servidor.');
        }
    };

    const cardapioFiltrado = useMemo(() => {
        if (filtroCategoria === 'Todos') return cardapio;
        return cardapio.filter(produto => produto.categoria === filtroCategoria);
    }, [cardapio, filtroCategoria]);

    if (loading) return <div className="p-8 text-center font-semibold">Carregando...</div>;

    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
                <div className="flex justify-between items-center mb-6">
                    <h1 className="text-3xl font-bold text-gray-800">Gerenciar Cardápio</h1>
                    <button onClick={() => handleOpenModal()} className="bg-orange-500 text-white font-bold py-2 px-4 rounded-lg hover:bg-orange-600 transition-colors flex items-center gap-2">
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" /></svg>
                        Adicionar Item
                    </button>
                </div>
                
                <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
                    {categorias.map(cat => (<button key={cat} onClick={() => setFiltroCategoria(cat)} className={`px-4 py-2 rounded-full font-semibold text-sm whitespace-nowrap transition-colors ${filtroCategoria === cat ? 'bg-orange-500 text-white shadow' : 'bg-white text-gray-700 hover:bg-gray-100 border'}`}>{cat}</button>))}
                </div>
                
                <div className="bg-white shadow-md rounded-lg overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nome</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Categoria</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Preço</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {cardapioFiltrado.map(produto => (
                                <tr key={produto.id}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{produto.id}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{produto.nome}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{produto.categoria}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ {produto.preco.toFixed(2).replace('.', ',')}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-4">
                                        <button onClick={() => handleOpenModal(produto)} className="text-blue-600 hover:text-blue-900 font-semibold">Editar</button>
                                        <button onClick={() => handleDeletar(produto.id, produto.nome)} className="text-red-600 hover:text-red-900 font-semibold">Deletar</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {isModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
                    <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md space-y-4">
                        <h2 className="text-2xl font-bold text-gray-800">{produtoEmEdicao ? 'Editar Item do Cardápio' : 'Novo Item do Cardápio'}</h2>
                        <div><label className="block text-sm font-medium text-gray-700">Nome</label><input type="text" name="nome" value={formState.nome} onChange={handleInputChange} className="mt-1 w-full p-2 border border-gray-300 rounded-md" /></div>
                        <div><label className="block text-sm font-medium text-gray-700">Descrição</label><input type="text" name="descricao" value={formState.descricao} onChange={handleInputChange} className="mt-1 w-full p-2 border border-gray-300 rounded-md" /></div>
                        <div><label className="block text-sm font-medium text-gray-700">Preço (ex: 18.90)</label><input type="number" name="preco" value={formState.preco} onChange={handleInputChange} className="mt-1 w-full p-2 border border-gray-300 rounded-md" /></div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Categoria</label>
                            <select name="categoria" value={formState.categoria} onChange={handleInputChange} className="mt-1 w-full p-2 border border-gray-300 rounded-md bg-white">
                                <option>Entradas</option><option>Pratos</option><option>Sobremesas</option><option>Bebidas</option>
                            </select>
                        </div>
                        <div className="flex justify-end gap-4 pt-4">
                            <button onClick={handleCloseModal} className="px-4 py-2 rounded-lg text-gray-700 bg-gray-200 hover:bg-gray-300 font-semibold">Cancelar</button>
                            <button onClick={handleSalvar} className="px-4 py-2 rounded-lg text-white bg-orange-500 hover:bg-orange-600 font-semibold">Salvar</button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default GerenciarCardapioPage;