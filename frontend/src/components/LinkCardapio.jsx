import React from 'react';
import { toast } from 'react-toastify';
import { useAuth } from '../contexts/AuthContext';

const LinkCardapio = () => {
    const { userProfile, loadingProfile } = useAuth(); // Puxar loadingProfile

    if (loadingProfile) {
        // Retorna um placeholder enquanto carrega o perfil
        return (
            <div className="bg-white dark:bg-tema-surface-dark p-4 rounded-xl shadow-lg border dark:border-gray-700 text-center mb-4">
                <p className="text-tema-text-muted dark:text-tema-text-muted-dark">Carregando informações do perfil...</p>
            </div>
        );
    }
    
    // Verifica se o perfil existe E se o ID do restaurante existe (evita o erro do link ser "/cardapio/null")
    if (!userProfile || !userProfile.id) return null;

    const link = `${window.location.origin}/cardapio/${userProfile.id}`;

    const handleCopyLink = () => {
        navigator.clipboard.writeText(link);
        toast.info("Link do cardápio copiado!");
    };

    return (
        <div className="bg-white dark:bg-tema-surface-dark p-4 rounded-xl shadow-lg border dark:border-gray-700 text-center mb-4">
            <h3 className="text-lg font-bold text-tema-text dark:text-tema-text-dark">Seu Cardápio Digital</h3>
            <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark my-2">Compartilhe este link com seus clientes:</p>
            <input 
                type="text" 
                readOnly 
                value={link}
                // 🚨 CORREÇÃO CRÍTICA AQUI
                className="w-full text-center bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-tema-text-dark p-2 rounded-md border"
            />
            <button
                onClick={handleCopyLink}
                className="mt-4 bg-tema-primary text-white font-bold py-2 px-6 rounded-lg hover:bg-opacity-80 transition-colors"
            >
                Copiar Link
            </button>
        </div>
    );
};

export default LinkCardapio;