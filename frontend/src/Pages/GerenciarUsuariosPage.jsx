import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';

// Modal para Adicionar/Editar Usuário
const UsuarioModal = ({ onClose, onSave, usuarioEmEdicao, limite }) => {
    const [formState, setFormState] = useState({
        nome: usuarioEmEdicao?.nome || '',
        email: usuarioEmEdicao?.email || '',
        senha: '',
        role: usuarioEmEdicao?.role || 'GARCOM', // Padrão: Garçom
    });

    const isEditing = !!usuarioEmEdicao;

    const handleSave = async () => {
        if (!formState.nome || !formState.email || (!isEditing && !formState.senha)) {
            toast.warn('Nome, Email e Senha (para novo usuário) são obrigatórios.');
            return;
        }

        // Simplificação: no back-end, você só precisa do POST para criar
        const dataToSend = { ...formState, role: formState.role };
        if (isEditing && !formState.senha) delete dataToSend.senha; // Não enviar senha se estiver editando e vazia

        try {
            await apiClient.post('/api/usuarios', dataToSend);
            onSave();
        } catch (error) {
            toast.error(error.message || 'Erro ao salvar usuário.');
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
            <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-md space-y-4" onClick={e => e.stopPropagation()}>
                <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">
                    {isEditing ? 'Editar Usuário' : `Novo Usuário (Limite: ${limite})`}
                </h2>
                <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Nome</label><input type="text" name="nome" value={formState.nome} onChange={e => setFormState({...formState, nome: e.target.value})} className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600" /></div>
                <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Email (Login)</label><input type="email" name="email" value={formState.email} onChange={e => setFormState({...formState, email: e.target.value})} className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600" /></div>
                <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Função</label>
                    <select name="role" value={formState.role} onChange={e => setFormState({...formState, role: e.target.value})} className="mt-1 w-full p-2 border rounded-md bg-white dark:bg-gray-800 dark:border-gray-600">
                        <option value="GARCOM">Garçom</option>
                        <option value="CAIXA">Caixa</option>
                        <option value="ADMIN">Administrador</option>
                    </select>
                </div>
                <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Senha {isEditing ? '(Deixe vazio para manter a atual)' : '*'}</label><input type="password" name="senha" value={formState.senha} onChange={e => setFormState({...formState, senha: e.target.value})} className="mt-1 w-full p-2 border rounded-md bg-gray-50 dark:bg-gray-800 dark:border-gray-600" /></div>
                <div className="flex justify-end gap-4 pt-4">
                    <button onClick={onClose} className="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 font-semibold">Cancelar</button>
                    <button onClick={handleSave} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">Salvar Usuário</button>
                </div>
            </div>
        </div>
    );
};


const GerenciarUsuariosPage = () => {
    const { userProfile, refreshProfile } = useAuth();
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const limiteUsuarios = userProfile?.limiteUsuarios || 3;
    const usuariosAtuais = usuarios.length;
    const isLimiteAtingido = usuariosAtuais >= limiteUsuarios && !userProfile?.isLegacyFree;
    
    const fetchUsuarios = async () => {
        try {
            const data = await apiClient.get('/api/usuarios');
            setUsuarios(data);
        } catch (error) {
            toast.error("Não foi possível carregar os usuários.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (userProfile) {
            fetchUsuarios();
        }
    }, [userProfile]);

    const handleOpenModal = () => {
        if (isLimiteAtingido) {
            toast.error("Limite de usuários atingido! Atualize seu plano.");
            return;
        }
        setIsModalOpen(true);
    };

    const handleDeletar = async (id, nome) => {
        if (!window.confirm(`Tem certeza que deseja deletar o usuário "${nome}"?`)) return;
        try {
            await apiClient.delete(`/api/usuarios/${id}`);
            toast.success(`Usuário ${nome} deletado com sucesso.`);
            fetchUsuarios();
        } catch (error) {
            toast.error(error.message || "Erro ao deletar usuário.");
        }
    };
    
    const handleSaveSuccess = () => {
        setIsModalOpen(false);
        fetchUsuarios();
    };

    if (loading) return <div className="p-8 text-center">Carregando...</div>;

    return (
        <>
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-6">
                <div className="flex justify-between items-center">
                    <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Gerenciar Usuários ({usuariosAtuais}/{limiteUsuarios})</h1>
                    <button 
                        onClick={handleOpenModal} 
                        disabled={isLimiteAtingido}
                        className={`font-bold py-2 px-4 rounded-lg transition-colors flex items-center gap-2 ${isLimiteAtingido ? 'bg-red-500 text-white cursor-not-allowed' : 'bg-tema-primary text-white hover:bg-opacity-80'}`}
                    >
                        + Novo Usuário
                    </button>
                </div>

                {isLimiteAtingido && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
                        <strong className="font-bold">LIMITE ATINGIDO!</strong>
                        <span className="block sm:inline ml-2">Você atingiu o limite de {limiteUsuarios} usuários ativos. Assine o plano Salão PDV para usuários ilimitados.</span>
                    </div>
                )}

                <div className="bg-white dark:bg-tema-surface-dark shadow-md rounded-lg overflow-x-auto border dark:border-gray-700">
                    <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                        <thead className="bg-gray-50 dark:bg-gray-800">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Nome</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Email</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Função</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                            {usuarios.map(u => (
                                <tr key={u.id} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">{u.nome}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-300">{u.email}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-tema-primary dark:text-tema-link-dark">{u.role}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-4">
                                        <button onClick={() => handleDeletar(u.id, u.nome)} className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 font-semibold">Deletar</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
            
            {isModalOpen && (<UsuarioModal onClose={() => setIsModalOpen(false)} onSave={handleSaveSuccess} limite={limiteUsuarios} />)}
        </>
    );
};

export default GerenciarUsuariosPage;