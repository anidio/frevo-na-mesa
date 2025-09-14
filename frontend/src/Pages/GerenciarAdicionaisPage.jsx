import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';

const GerenciarAdicionaisPage = () => {
    const [adicionais, setAdicionais] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [adicionalEmEdicao, setAdicionalEmEdicao] = useState(null);
    const [formState, setFormState] = useState({ nome: '', preco: '' });

    const fetchData = async () => {
        try {
            setLoading(true);
            const data = await apiClient.get('/api/adicionais');
            setAdicionais(data);
        } catch (error) {
            toast.error("Não foi possível carregar os adicionais.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleOpenModal = (adicional = null) => {
        if (adicional) {
            setAdicionalEmEdicao(adicional);
            setFormState({ nome: adicional.nome, preco: adicional.preco });
        } else {
            setAdicionalEmEdicao(null);
            setFormState({ nome: '', preco: '' });
        }
        setIsModalOpen(true);
    };

    const handleCloseModal = () => setIsModalOpen(false);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormState(prev => ({ ...prev, [name]: value }));
    };

    const handleSalvar = async () => {
        if (!formState.nome || !formState.preco) {
            toast.warn("Nome e Preço são obrigatórios.");
            return;
        }

        const endpoint = adicionalEmEdicao ? `/api/adicionais/${adicionalEmEdicao.id}` : '/api/adicionais';
        const method = adicionalEmEdicao ? 'put' : 'post';

        try {
            await apiClient[method](endpoint, formState);
            toast.success(`Adicional ${adicionalEmEdicao ? 'atualizado' : 'criado'} com sucesso!`);
            handleCloseModal();
            fetchData();
        } catch (error) {
            toast.error("Erro ao salvar adicional.");
        }
    };

    const handleDeletar = async (id, nome) => {
        if (window.confirm(`Tem certeza que deseja deletar o adicional "${nome}"?`)) {
            try {
                await apiClient.delete(`/api/adicionais/${id}`);
                toast.success(`Adicional "${nome}" deletado.`);
                fetchData();
            } catch (error) {
                toast.error("Erro ao deletar adicional.");
            }
        }
    };

    if (loading) return <div className="p-8 text-center">Carregando...</div>;

    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
                <div className="flex justify-between items-center mb-6">
                    <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Gerenciar Adicionais</h1>
                    <button onClick={() => handleOpenModal()} className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 flex items-center gap-2">
                        + Adicionar Item
                    </button>
                </div>
                <div className="bg-white dark:bg-tema-surface-dark shadow-md rounded-lg overflow-x-auto border dark:border-gray-700">
                    <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                        <thead className="bg-gray-50 dark:bg-gray-800">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Nome</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Preço</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                            {adicionais.map(ad => (
                                <tr key={ad.id}>
                                    <td className="px-6 py-4 text-sm text-gray-700 dark:text-gray-300">{ad.nome}</td>
                                    <td className="px-6 py-4 text-sm text-gray-700 dark:text-gray-300">R$ {ad.preco.toFixed(2).replace('.', ',')}</td>
                                    <td className="px-6 py-4 text-right text-sm font-medium space-x-4">
                                        <button onClick={() => handleOpenModal(ad)} className="text-blue-600 hover:text-blue-900 font-semibold">Editar</button>
                                        <button onClick={() => handleDeletar(ad.id, ad.nome)} className="text-red-600 hover:text-red-900 font-semibold">Deletar</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {isModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
                    <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-md space-y-4">
                        <h2 className="text-2xl font-bold">{adicionalEmEdicao ? 'Editar Adicional' : 'Novo Adicional'}</h2>
                        <div>
                            <label className="block text-sm font-medium">Nome</label>
                            <input type="text" name="nome" value={formState.nome} onChange={handleInputChange} className="mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600" />
                        </div>
                        <div>
                            <label className="block text-sm font-medium">Preço</label>
                            <input type="number" name="preco" value={formState.preco} onChange={handleInputChange} className="mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600" />
                        </div>
                        <div className="flex justify-end gap-4 pt-4">
                            <button onClick={handleCloseModal} className="px-4 py-2 rounded-lg bg-gray-200 dark:bg-gray-700 font-semibold">Cancelar</button>
                            <button onClick={handleSalvar} className="px-4 py-2 rounded-lg text-white bg-tema-primary font-semibold">Salvar</button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default GerenciarAdicionaisPage;