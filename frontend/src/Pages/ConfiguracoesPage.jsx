// frontend/src/Pages/ConfiguracoesPage.jsx
import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';
import { useAuth } from '../contexts/AuthContext';

// Componente ToggleSwitch (sem alterações)
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
    // loadingProfile é importante para saber quando o refresh terminou
    const { userProfile, refreshProfile, loadingProfile } = useAuth();
    const [settings, setSettings] = useState({
        impressaoMesaAtivada: true,
        impressaoDeliveryAtivada: true,
        whatsappNumber: '',
        taxaEntrega: 0,
        isCalculoHaversineAtivo: false,
    });
    // Renomeado loading interno para evitar conflito
    const [isComponentLoading, setIsComponentLoading] = useState(true);

    useEffect(() => {
        // Roda APENAS quando o loadingProfile do AuthContext for FALSE
        // e userProfile existir. Isso garante que temos os dados mais recentes.
        if (!loadingProfile && userProfile) {
            console.log("ConfiguracoesPage - useEffect running AFTER loadingProfile is false.");
            console.log("ConfiguracoesPage - isCalculoHaversineAtivo from refreshed profile:", userProfile.isCalculoHaversineAtivo);
            setSettings({
                impressaoMesaAtivada: userProfile.impressaoMesaAtivada,
                impressaoDeliveryAtivada: userProfile.impressaoDeliveryAtivada,
                whatsappNumber: userProfile.whatsappNumber || '',
                taxaEntrega: userProfile.taxaEntrega || 0,
                // Aqui garantimos que o estado local reflita o perfil recém-carregado
                isCalculoHaversineAtivo: userProfile.isCalculoHaversineAtivo,
            });
            setIsComponentLoading(false); // Para o loading interno do componente
        } else if (!loadingProfile && !userProfile) {
            // Caso de erro ou deslogado
            setIsComponentLoading(false);
        }
        // Depende de userProfile E loadingProfile
    }, [userProfile, loadingProfile]);

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target; // Adicionado type e checked para toggles futuros
        const newValue = type === 'checkbox' ? checked : name === 'taxaEntrega' ? parseFloat(value) || 0 : value;
        setSettings(prev => ({ ...prev, [name]: newValue }));
    };

    // Função handleToggleChange específica para o ToggleSwitch
    const handleToggleChange = (name, value) => {
        setSettings(prev => ({ ...prev, [name]: value }));
    };


    const handleSaveSettings = async () => {
        // Desabilitar botão ou mostrar loading aqui seria bom
        console.log("ConfiguracoesPage - Saving settings:", settings);
        try {
            await apiClient.put('/api/restaurante/configuracoes', settings);
            toast.success('Configurações salvas!');
            console.log("ConfiguracoesPage - PUT successful. Calling refreshProfile...");
            // Chama o refreshProfile que agora gerencia seu próprio estado de loading
            await refreshProfile();
            console.log("ConfiguracoesPage - refreshProfile should have completed.");
            // Não precisa mais setar o loading local aqui, o useEffect cuidará disso
        } catch (error) {
            console.error("Erro ao salvar configurações:", error);
            toast.error('Erro ao salvar as configurações.');
            // Reabilitar botão ou esconder loading aqui
        }
    };


    // Usa o loading INTERNO do componente OU o loading GERAL do AuthContext
    if (isComponentLoading || loadingProfile) {
        return <div className="p-8 text-center">Carregando configurações...</div>;
    }

    return (
        <div className="w-full p-4 md:p-8 max-w-2xl mx-auto">
            {/* ... (restante do JSX sem alterações visuais) ... */}
            <div className="text-center mb-12">
                <h1 className="text-4xl font-extrabold text-tema-text dark:text-tema-text-dark mb-2">Ajustes Operacionais</h1>
                <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Ajuste as preferências do seu sistema.</p>
            </div>

            <div className="space-y-6">

                {/* --- SEÇÃO CÁLCULO DE FRETE (SWITCH) --- */}
                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Cálculo de Frete</h2>

                    <ToggleSwitch
                        label="Ativar Cálculo de Frete por CEP (Haversine/Faixas de KM)"
                        enabled={settings.isCalculoHaversineAtivo}
                        // Usa a função específica para o toggle
                        onChange={(value) => handleToggleChange('isCalculoHaversineAtivo', value)}
                    />

                    {settings.isCalculoHaversineAtivo ? (
                        <div className="mt-4 p-4 bg-yellow-100 border border-yellow-400 text-yellow-800 rounded relative dark:bg-yellow-900/30 dark:border-yellow-700 dark:text-yellow-200" role="alert">
                             <strong className="font-bold">MODO ATIVO:</strong>
                             <span className="block sm:inline ml-2">O frete será calculado pela distância em linha reta (Haversine). **Configure suas faixas de KM e CEP do restaurante!**</span>
                        </div>
                    ) : (
                        <div className="mt-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded relative dark:bg-green-900/30 dark:border-green-700 dark:text-green-200" role="alert">
                             <strong className="font-bold">MODO ATIVO:</strong>
                             <span className="block sm:inline ml-2">O frete usará o **Valor Fixo** definido abaixo.</span>
                        </div>
                    )}
                </div>
                {/* --- FIM SEÇÃO CÁLCULO DE FRETE --- */}


                {/* --- SEÇÃO TAXA DE ENTREGA FIXA --- */}
                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Taxa de Entrega (Valor Fixo)</h2>
                    <div className="space-y-4 bg-white dark:bg-tema-surface-dark p-4 rounded-lg border dark:border-gray-700">
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Valor Fixo da Taxa de Entrega (R$)</label>
                            <input
                                type="number"
                                name="taxaEntrega"
                                value={settings.taxaEntrega}
                                onChange={handleInputChange}
                                className="mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark border-gray-300"
                                placeholder="Ex: 5.00"
                                step="0.01" // Permite decimais
                            />
                        </div>
                    </div>
                </div>

                {/* --- SEÇÃO IMPRESSÃO E WHATSAPP --- */}
                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Impressão de Pedidos</h2>
                    <div className="space-y-4">
                        <ToggleSwitch
                            label="Ativar fila de impressão para Mesas"
                            enabled={settings.impressaoMesaAtivada}
                            onChange={(value) => handleToggleChange('impressaoMesaAtivada', value)}
                        />
                        <ToggleSwitch
                            label="Ativar fila de impressão para Delivery"
                            enabled={settings.impressaoDeliveryAtivada}
                            onChange={(value) => handleToggleChange('impressaoDeliveryAtivada', value)}
                        />
                    </div>
                </div>

                <div>
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark mb-3">Automação do WhatsApp</h2>
                    <div className="space-y-4 bg-white dark:bg-tema-surface-dark p-4 rounded-lg border dark:border-gray-700">
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Número do WhatsApp para enviar notificações</label>
                            <input
                                type="text"
                                name="whatsappNumber"
                                value={settings.whatsappNumber}
                                onChange={handleInputChange}
                                className="mt-1 w-full p-2 border rounded-md dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark border-gray-300"
                                placeholder="Ex: 5581999998888 (com código do país e DDD)"
                            />
                             <p className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark mt-2">
                                Este número será usado para enviar as atualizações de status dos pedidos para os seus clientes.
                            </p>
                        </div>
                    </div>
                </div>
                {/* --- FIM SEÇÃO IMPRESSÃO E WHATSAPP --- */}

                <div className="flex justify-end pt-6">
                    <button
                        onClick={handleSaveSettings}
                        // Desabilita enquanto o perfil está carregando (após salvar)
                        disabled={loadingProfile}
                        className={`bg-tema-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-opacity-80 transition-colors ${loadingProfile ? 'opacity-50 cursor-wait' : ''}`}
                    >
                        {loadingProfile ? 'Salvando...' : 'Salvar Configurações'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfiguracoesPage;