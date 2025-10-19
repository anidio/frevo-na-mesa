import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';

const GerenciarPerfilPage = () => {
    const { userProfile, refreshProfile } = useAuth();
    const [formState, setFormState] = useState({
        nome: '',
        endereco: '',
        logoUrl: '',
        cepRestaurante: '', // NOVO: Campo CEP do Restaurante
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (userProfile) {
            setFormState({
                nome: userProfile.nome || '',
                endereco: userProfile.endereco || '',
                logoUrl: userProfile.logoUrl || '',
                cepRestaurante: userProfile.cepRestaurante || '', // CARREGA O NOVO CAMPO
            });
            setLoading(false);
        }
    }, [userProfile]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormState(prev => ({ ...prev, [name]: value }));
    };

    const handleSaveProfile = async () => {
        try {
            await apiClient.put('/api/restaurante/perfil', {
                ...formState,
                // Removido: latitude e longitude
            });
            toast.success('Dados do Perfil atualizados!');
            await refreshProfile();
        } catch (error) {
            toast.error('Erro ao salvar o perfil.');
        }
    };

    if (loading) {
        return <div className="p-8 text-center">Carregando perfil...</div>;
    }
    
    const inputClass = "mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark";

    return (
        <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
            <div className="text-center mb-12">
                <h1 className="text-4xl font-extrabold text-tema-text dark:text-tema-text-dark mb-2">Gerenciar Perfil</h1>
                <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Informações que aparecem no seu Cardápio Digital.</p>
            </div>

            <div className="space-y-6">
                <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md border dark:border-gray-700">
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Informações Básicas e Endereço</h2>
                    <div>
                        <label className="block text-sm font-medium">Nome do Restaurante</label>
                        <input type="text" name="nome" value={formState.nome} onChange={handleInputChange} className={inputClass} />
                    </div>
                    <div className="mt-4">
                        <label className="block text-sm font-medium">Endereço Completo</label>
                        <input type="text" name="endereco" value={formState.endereco} onChange={handleInputChange} className={inputClass} />
                    </div>
                    {/* NOVO CAMPO PARA CEP DO RESTAURANTE */}
                    <div className="mt-4">
                        <label className="block text-sm font-medium">CEP do Restaurante (Ponto de Partida para Frete)</label>
                        <input type="text" name="cepRestaurante" value={formState.cepRestaurante} onChange={handleInputChange} className={inputClass} placeholder="Ex: 50000000" />
                    </div>
                    <div className="mt-4">
                        <label className="block text-sm font-medium">URL da Logo (ou Imagem)</label>
                        <input type="text" name="logoUrl" value={formState.logoUrl} onChange={handleInputChange} className={inputClass} placeholder="Ex: https://seusite.com/logo.png" />
                        {formState.logoUrl && (
                            <img src={formState.logoUrl} alt="Pré-visualização da Logo" className="mt-2 h-20 border rounded object-contain" />
                        )}
                    </div>
                </div>

                <div className="flex justify-end pt-6">
                    <button
                        onClick={handleSaveProfile}
                        className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors"
                    >
                        Salvar Perfil
                    </button>
                </div>
            </div>
        </div>
    );
};

export default GerenciarPerfilPage;