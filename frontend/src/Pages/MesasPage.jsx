import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import MesaCard from '../components/MesaCard';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';

const GarcomIcon = () => (
    // ... (código do ícone sem alterações)
     <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
    </svg>
);

const MesasPage = () => {
    const { userProfile, loadingProfile } = useAuth();
    const [mesas, setMesas] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [novoNumero, setNovoNumero] = useState('');
    const [novoNome, setNovoNome] = useState('');
    const navigate = useNavigate();

    // --- LÓGICA DE ACESSO AO MÓDULO (REMOVIDA DAQUI) ---
    // A página sempre será acessível se logado. Os limites são tratados abaixo.

    const fetchData = async () => {
        // Busca os dados independentemente do plano PRO, pois o acesso básico é permitido.
        try {
            setLoading(true);
            const data = await apiClient.get('/api/mesas');
            data.sort((a, b) => a.numero - b.numero); // Ordena as mesas
            setMesas(data);
        } catch (error) {
            console.error("Erro ao buscar mesas:", error);
            toast.error("Erro ao carregar as mesas.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // Roda fetchData apenas se o perfil estiver carregado (logado)
        if (!loadingProfile && userProfile) {
            fetchData();
        } else if (!loadingProfile && !userProfile) {
             setLoading(false); // Para o loading se não estiver logado
             // O ProtectedRoute já deve redirecionar para login se não estiver logado
        }
    }, [userProfile, loadingProfile]); // Depende apenas do estado de login/perfil

    // --- LÓGICA DE LIMITES ATUALIZADA ---
    const isSalaoPro = userProfile?.isSalaoPro; // Verifica se tem o plano específico
    const limiteMesasGratuito = userProfile?.limiteMesas || 10; // Pega o limite do perfil (padrão 10)
    const totalMesas = mesas.length;
    const isLegacyFree = userProfile?.isLegacyFree;
    const isBetaTester = userProfile?.isBetaTester;

    // **CORREÇÃO AQUI:** Limite SÓ se aplica se NÃO for Salao PRO (e não for legacy/beta)
    const aplicaLimite = !isSalaoPro && !isLegacyFree && !isBetaTester;
    const isLimiteAtingido = aplicaLimite && (totalMesas >= limiteMesasGratuito);


    const handleAdicionarMesa = async () => {
        // Trava de limite (verifica a flag 'aplicaLimite' corrigida)
        if (isLimiteAtingido) {
            toast.error(`Limite de ${limiteMesasGratuito} mesas atingido para o plano Gratuito! Atualize para o Salão PDV.`);
            // Opcional: Redirecionar para a página de planos
            // navigate('/admin/financeiro');
            return;
        }

        if (!novoNumero || isNaN(parseInt(novoNumero)) || parseInt(novoNumero) <= 0) {
            toast.warn('Por favor, insira um número de mesa válido e positivo.');
            return;
        }
        try {
            await apiClient.post('/api/mesas', {
                numero: parseInt(novoNumero),
                nomeCliente: novoNome || null
            });

            toast.success('Mesa adicionada com sucesso!');
            setIsModalOpen(false);
            setNovoNumero('');
            setNovoNome('');
            fetchData();
        } catch (error) {
            console.error("Erro de comunicação ao adicionar mesa:", error);
            // Mostra a mensagem de erro específica do backend (ex: número já existe)
            toast.error(`Erro ao adicionar mesa: ${error.message || 'Verifique se o número já existe.'}`);
        }
    };

    // Exibe loading enquanto perfil ou mesas carregam
    if (loadingProfile || loading) {
        return <div className="p-8 text-center text-tema-text-muted dark:text-tema-text-muted-dark">Carregando...</div>;
    }

    // --- BLOCO DE VERIFICAÇÃO DE ACESSO REMOVIDO ---
    // A página será renderizada se o usuário estiver logado.

    // Calcula totais (sempre calcula, mesmo no plano gratuito)
    const mesasLivres = mesas.filter(m => m.status === 'LIVRE').length;
    const mesasOcupadas = mesas.filter(m => m.status === 'OCUPADA').length;
    const totalEmAberto = mesas
        .filter(m => m.status === 'OCUPADA')
        .reduce((acc, mesa) => acc + (mesa.valorTotal || 0), 0);

    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-7xl mx-auto">
                {/* Cabeçalho */}
                <div className="text-center mb-8 text-tema-primary">
                    <div className="flex justify-center items-center gap-2">
                        <GarcomIcon />
                        <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Frevo Garçom</h1>
                    </div>
                    <p className="mt-1 text-gray-500 dark:text-gray-400">Selecione uma mesa para começar o atendimento.</p>
                </div>

                {/* Alerta de Limite (Mostra apenas se o limite se aplica E está atingido) */}
                {aplicaLimite && isLimiteAtingido && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
                        <strong className="font-bold">⚠️ LIMITE ATINGIDO!</strong>
                        <span className="block sm:inline ml-2">Você atingiu o limite de {limiteMesasGratuito} mesas. <Link to="/admin/financeiro" className="font-bold underline hover:text-red-800">Atualize</Link> para o plano Salão PDV para mesas ilimitadas!</span>
                    </div>
                )}


                {/* Botão Adicionar Mesa */}
                <div className="flex justify-end mb-4">
                    <button
                        onClick={() => {
                            // Verifica o limite antes de abrir o modal
                            if (isLimiteAtingido) {
                                toast.error(`Limite de ${limiteMesasGratuito} mesas atingido para o plano Gratuito!`);
                            } else {
                                setIsModalOpen(true);
                            }
                        }}
                        // Botão desabilitado se o limite se aplica E está atingido
                        disabled={isLimiteAtingido}
                        className={`font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors flex items-center gap-2 ${isLimiteAtingido ? 'bg-gray-400 text-gray-700 cursor-not-allowed' : 'bg-tema-primary text-white'}`}
                    >
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" /></svg>
                        Adicionar Mesa
                    </button>
                </div>

                {/* Grid de Mesas */}
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-6">
                    {mesas.map(mesa => (
                        <MesaCard key={mesa.id} mesa={mesa} onClick={() => navigate(`/mesas/${mesa.id}`)} />
                    ))}
                    {mesas.length === 0 && !loading && (
                         <p className="col-span-full text-center text-tema-text-muted dark:text-tema-text-muted-dark py-10">Nenhuma mesa cadastrada ainda. Clique em "Adicionar Mesa".</p>
                    )}
                </div>

                {/* Resumo */}
                <div className="mt-12 p-4 bg-white dark:bg-tema-surface-dark rounded-lg shadow-md flex flex-wrap justify-around items-center gap-4 border border-gray-200 dark:border-gray-700">
                    <div className="text-center">
                        <p className="text-sm text-gray-500 dark:text-gray-400">Mesas Livres</p>
                        <p className="text-2xl font-bold text-green-600 dark:text-green-400">{mesasLivres}</p>
                    </div>
                    <div className="text-center">
                        <p className="text-sm text-gray-500 dark:text-gray-400">Mesas Ocupadas</p>
                        <p className="text-2xl font-bold text-yellow-600 dark:text-yellow-400">{mesasOcupadas}</p>
                    </div>
                    <div className="text-center">
                        <p className="text-sm text-gray-500 dark:text-gray-400">Total em Aberto</p>
                        <p className="text-2xl font-bold text-tema-accent dark:text-red-400">
                            R$ {totalEmAberto.toFixed(2).replace('.', ',')}
                        </p>
                    </div>
                </div>
            </div>

            {/* Modal de Adicionar Mesa */}
            {isModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50" onClick={() => setIsModalOpen(false)}>
                    <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-sm" onClick={e => e.stopPropagation()}>
                        <h2 className="text-2xl font-bold text-gray-800 dark:text-tema-text-dark mb-4">Adicionar Nova Mesa</h2>
                        <div className="mb-4">
                            <label htmlFor="table-number" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Número da Mesa*</label>
                            <input type="number" id="table-number" value={novoNumero} onChange={(e) => setNovoNumero(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md shadow-sm focus:outline-none focus:ring-orange-500 focus:border-orange-500" placeholder="Ex: 11" autoFocus required />
                        </div>
                        <div className="mb-4">
                            <label htmlFor="table-name" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Nome do Cliente (Opcional)</label>
                            <input type="text" id="table-name" value={novoNome} onChange={(e) => setNovoNome(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md shadow-sm focus:outline-none focus:ring-orange-500 focus:border-orange-500" placeholder="Ex: João Silva" />
                        </div>
                        <div className="mt-6 flex justify-end gap-4">
                            <button onClick={() => setIsModalOpen(false)} className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">Cancelar</button>
                            <button onClick={handleAdicionarMesa} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">Salvar Mesa</button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default MesasPage;