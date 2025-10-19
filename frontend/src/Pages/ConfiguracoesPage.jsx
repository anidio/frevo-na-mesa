import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';

const ToggleSwitch = ({ label, enabled, onChange }) => (
    <div className="flex items-center justify-between bg-white dark:bg-tema-surface-dark p-4 rounded-lg border dark:border-gray-700">
        <span className="font-semibold text-tema-text dark:text-tema-text-dark">{label}</span>
        <button
            onClick={() => onChange(!enabled)}
            className={`relative inline-flex items-center h-6 rounded-full w-11 transition-colors duration-300 ${
                enabled ? 'bg-tema-primary' : 'bg-gray-300 dark:bg-gray-600'
            }`}
        >
            <span
                className={`inline-block w-4 h-4 transform bg-white rounded-full transition-transform duration-300 ${
                    enabled ? 'translate-x-6' : 'translate-x-1'
                }`}
            />
        </button>
    </div>
);

const ConfiguracoesPage = () => {
    const { userProfile, refreshProfile } = useAuth();
    const [settings, setSettings] = useState({
        impressaoMesaAtivada: true,
        impressaoDeliveryAtivada: true,
        whatsappNumber: '', 
        taxaEntrega: 0, // REINTRODUZIDO
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (userProfile) {
            setSettings({
                impressaoMesaAtivada: userProfile.impressaoMesaAtivada,
                impressaoDeliveryAtivada: userProfile.impressaoDeliveryAtivada,
                whatsappNumber: userProfile.whatsappNumber || '', 
                taxaEntrega: userProfile.taxaEntrega || 0, // CARREGA TAXA FIXA
            });
            setLoading(false);
        }
    }, [userProfile]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        // Lida com a entrada de número para taxaEntrega, garantindo que seja numérico
        const newValue = name === 'taxaEntrega' ? parseFloat(value) || 0 : value; 
        setSettings(prev => ({ ...prev, [name]: newValue }));
    };

    const handleSaveSettings = async () => {
        try {
            await apiClient.put('/api/restaurante/configuracoes', settings);
            toast.success('Configurações salvas!');
            await refreshProfile();
        } catch (error) {
            toast.error('Erro ao salvar as configurações.');
        }
    };

    if (loading) {
        return <div className="p-8 text-center">Carregando...</div>;
    }

    return (
        <div className="w-full p-4 md:p-8 max-w-2xl mx-auto">
            <div className="text-center mb-12">
                <h1 className="text-4xl font-extrabold text-tema-text dark:text-tema-text-dark mb-2">Configurações</h1>
                <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Ajuste as preferências do seu sistema.</p>
            </div>

            <div className="space-y-6">
                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Impressão de Pedidos</h2>
                    <div className="space-y-4">
                        <ToggleSwitch
                            label="Ativar fila de impressão para Mesas"
                            enabled={settings.impressaoMesaAtivada}
                            onChange={(value) => setSettings({...settings, impressaoMesaAtivada: value})}
                        />
                        <ToggleSwitch
                            label="Ativar fila de impressão para Delivery"
                            enabled={settings.impressaoDeliveryAtivada}
                            onChange={(value) => setSettings({...settings, impressaoDeliveryAtivada: value})}
                        />
                    </div>
                </div>

                {/* --- SEÇÃO TAXA DE ENTREGA FIXA (REINTRODUZIDA) --- */}
                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Taxa de Entrega (Fixa)</h2>
                    <div className="space-y-4 bg-white dark:bg-tema-surface-dark p-4 rounded-lg border dark:border-gray-700">
                        <div>
                            <label className="block text-sm font-medium">Valor Fixo da Taxa de Entrega (R$)</label>
                            <input 
                                type="number" 
                                name="taxaEntrega" 
                                value={settings.taxaEntrega} 
                                onChange={handleInputChange} 
                                className="mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600" 
                                placeholder="Ex: 5.00"
                                step="0.01" // Permite decimais
                            />
                        </div>
                    </div>
                </div>

                {/* --- SEÇÃO WHATSAPP --- */}
                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Automação do WhatsApp</h2>
                    <div className="space-y-4 bg-white dark:bg-tema-surface-dark p-4 rounded-lg border dark:border-gray-700">
                        <div>
                            <label className="block text-sm font-medium">Número do WhatsApp para enviar notificações</label>
                            <input 
                                type="text" 
                                name="whatsappNumber" 
                                value={settings.whatsappNumber} 
                                onChange={handleInputChange} 
                                className="mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600" 
                                placeholder="Ex: 5581999998888 (com código do país e DDD)" 
                            />
                             <p className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark mt-2">
                                Este número será usado para enviar as atualizações de status dos pedidos para os seus clientes.
                            </p>
                        </div>
                    </div>
                </div>

                <div className="flex justify-end pt-6">
                    <button
                        onClick={handleSaveSettings}
                        className="bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors"
                    >
                        Salvar Configurações
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfiguracoesPage;