import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';

// Componente de Linha da Tabela
const AreaRow = ({ area, onEdit, onDelete }) => (
    <tr className="hover:bg-gray-50 dark:hover:bg-gray-800">
        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">{area.nome}</td>
        {/* ALTERADO: Exibe a Distância Máxima em KM */}
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-300">Até {area.maxDistanceKm.toFixed(1)} km</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-300">R$ {area.valorEntrega.toFixed(2).replace('.', ',')}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-300">R$ {area.valorMinimoPedido.toFixed(2).replace('.', ',')}</td>
        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-4">
            <button onClick={() => onEdit(area)} className="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 font-semibold">Editar</button>
            <button onClick={() => onDelete(area.id, area.nome)} className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 font-semibold">Deletar</button>
        </td>
    </tr>
);

// Componente Modal (Adicionar/Editar)
const AreaModal = ({ area, onClose, onSave }) => {
    const [formState, setFormState] = useState({
        nome: area?.nome || '',
        // ALTERADO: Inicializa com maxDistanceKm ou 0
        maxDistanceKm: area?.maxDistanceKm || 0.00,
        valorEntrega: area?.valorEntrega || 0.00,
        valorMinimoPedido: area?.valorMinimoPedido || 0.00,
    });

    const handleSave = () => {
        if (!formState.nome || formState.maxDistanceKm <= 0) {
            toast.warn('O nome e a Distância Máxima (KM) são obrigatórios e devem ser maiores que zero.');
            return;
        }
        onSave({ ...area, ...formState });
    };

    const inputClass = "mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark";

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
            <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-lg space-y-4">
                <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">
                    {area ? 'Editar Faixa de Distância' : 'Nova Faixa de Distância'}
                </h2>
                <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Nome da Faixa</label>
                    <input type="text" name="nome" value={formState.nome} onChange={e => setFormState({...formState, nome: e.target.value})} className={inputClass} autoFocus />
                </div>
                {/* ALTERADO: Campo de Distância Máxima */}
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Distância Máxima (KM)</label>
                        <input type="number" name="maxDistanceKm" value={formState.maxDistanceKm} onChange={e => setFormState({...formState, maxDistanceKm: parseFloat(e.target.value) || 0})} className={inputClass} step="0.1" />
                    </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Valor da Entrega (R$)</label>
                        <input type="number" name="valorEntrega" value={formState.valorEntrega} onChange={e => setFormState({...formState, valorEntrega: parseFloat(e.target.value) || 0})} className={inputClass} step="0.01" />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Valor Mínimo do Pedido (R$)</label>
                        <input type="number" name="valorMinimoPedido" value={formState.valorMinimoPedido} onChange={e => setFormState({...formState, valorMinimoPedido: parseFloat(e.target.value) || 0})} className={inputClass} step="0.01" />
                    </div>
                </div>
                <div className="flex justify-end gap-4 pt-4">
                    <button onClick={onClose} className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">Cancelar</button>
                    <button onClick={handleSave} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">Salvar</button>
                </div>
            </div>
        </div>
    );
};

// Componente Principal
const GerenciarAreasEntregaPage = () => {
    const [areas, setAreas] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [areaEmEdicao, setAreaEmEdicao] = useState(null);

    const fetchData = async () => {
        setLoading(true);
        try {
            // A rota antiga continua a mesma, mas agora retorna objetos com Faixa de Distância
            const data = await apiClient.get('/api/areas-entrega');
            setAreas(data);
        } catch (error) {
            toast.error("Não foi possível carregar as faixas de distância.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleOpenModal = (area = null) => {
        setAreaEmEdicao(area);
        setIsModalOpen(true);
    };

    const handleSaveArea = async (area) => {
        const endpoint = area.id ? `/api/areas-entrega/${area.id}` : '/api/areas-entrega';
        // Se a área tem ID, é PUT (atualizar), caso contrário é POST (criar)
        const method = area.id ? 'put' : 'post'; 

        try {
            await apiClient[method](endpoint, area);
            toast.success(`Faixa ${area.id ? 'atualizada' : 'adicionada'} com sucesso!`);
            setIsModalOpen(false);
            fetchData();
        } catch (error) {
            const errorMsg = error.message || 'Erro ao salvar faixa de distância.';
            toast.error(errorMsg);
        }
    };
    
    const handleDeletarArea = (areaId, nome) => {
        if (!window.confirm(`Tem certeza que deseja deletar a faixa "${nome}"?`)) return;
        apiClient.delete(`/api/areas-entrega/${areaId}`)
            .then(() => {
                toast.success(`Faixa "${nome}" deletada.`);
                fetchData();
            })
            .catch(error => toast.error(`Erro ao deletar: ${error.message}`));
    };


    if (loading) return <div className="p-8 text-center font-semibold text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</div>;

    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-8">
                <div className="flex justify-between items-center mb-6">
                    {/* ALTERADO: Título */}
                    <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Gerenciar Frete por Distância (KM)</h1>
                    <button onClick={() => handleOpenModal()} className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors flex items-center gap-2">
                        + Nova Faixa
                    </button>
                </div>
                
                {/* NOVO ALERTA: Instruções de uso */}
                <div className="bg-blue-100 border border-blue-400 text-blue-700 px-4 py-3 rounded relative" role="alert">
                    <strong className="font-bold">IMPORTANTE:</strong>
                    <span className="block sm:inline ml-2">Esta funcionalidade simula o cálculo de distância real. O valor final do frete será a **primeira faixa** em que o KM calculado se encaixar (da menor para a maior distância).</span>
                </div>

                <div className="bg-white dark:bg-tema-surface-dark shadow-md rounded-lg overflow-x-auto border dark:border-gray-700">
                    <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                        <thead className="bg-gray-50 dark:bg-gray-800">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Nome da Faixa</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Distância Máx. (KM)</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Entrega (R$)</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Pedido Mín. (R$)</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                            {areas.map(area => (
                                <AreaRow 
                                    key={area.id} 
                                    area={area} 
                                    onEdit={handleOpenModal} 
                                    onDelete={handleDeletarArea} 
                                />
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {isModalOpen && (
                <AreaModal 
                    area={areaEmEdicao} 
                    onClose={() => setIsModalOpen(false)}
                    onSave={handleSaveArea}
                />
            )}
        </>
    );
};

export default GerenciarAreasEntregaPage;