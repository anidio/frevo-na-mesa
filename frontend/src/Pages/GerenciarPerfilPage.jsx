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
        latitude: '',
        longitude: ''
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (userProfile) {
            setFormState({
                nome: userProfile.nome || '',
                endereco: userProfile.endereco || '',
                logoUrl: userProfile.logoUrl || '',
                latitude: userProfile.latitude || '',
                longitude: userProfile.longitude || ''
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
                latitude: parseFloat(formState.latitude) || null,
                longitude: parseFloat(formState.longitude) || null,
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
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Informações Básicas</h2>
                    <div>
                        <label className="block text-sm font-medium">Nome do Restaurante</label>
                        <input type="text" name="nome" value={formState.nome} onChange={handleInputChange} className={inputClass} />
                    </div>
                    <div className="mt-4">
                        <label className="block text-sm font-medium">Endereço Completo</label>
                        <input type="text" name="endereco" value={formState.endereco} onChange={handleInputChange} className={inputClass} />
                    </div>
                    <div className="mt-4">
                        <label className="block text-sm font-medium">URL da Logo (ou Imagem)</label>
                        <input type="text" name="logoUrl" value={formState.logoUrl} onChange={handleInputChange} className={inputClass} placeholder="Ex: https://seusite.com/logo.png" />
                        {formState.logoUrl && (
                            <img src={formState.logoUrl} alt="Pré-visualização da Logo" className="mt-2 h-20 border rounded object-contain" />
                        )}
                    </div>
                </div>

                <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-lg shadow-md border dark:border-gray-700">
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Localização (Frete por Distância)</h2>
                    <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark mb-4">
                        Estas coordenadas são usadas para calcular a distância do Delivery. Use uma ferramenta externa (ex: Google Maps) para obter a Lat/Long do seu restaurante.
                    </p>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium">Latitude</label>
                            <input type="number" name="latitude" value={formState.latitude} onChange={handleInputChange} className={inputClass} step="any" placeholder="-8.0578" />
                        </div>
                        <div>
                            <label className="block text-sm font-medium">Longitude</label>
                            <input type="number" name="longitude" value={formState.longitude} onChange={handleInputChange} className={inputClass} step="any" placeholder="-34.8828" />
                        </div>
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