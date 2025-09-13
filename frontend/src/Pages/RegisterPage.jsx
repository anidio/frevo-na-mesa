// src/Pages/RegisterPage.jsx

import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as authService from '../services/authService';
import { toast } from 'react-toastify';

const RegisterPage = () => {
    const [nome, setNome] = useState('');
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [confirmaSenha, setConfirmaSenha] = useState('');
    const [erroSenha, setErroSenha] = useState('');
    const [tipo, setTipo] = useState('RESTAURANTE_COM_MESAS');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();

        // 2. Validação da confirmação de senha
        if (senha !== confirmaSenha) {
            setErroSenha('As senhas não coincidem!'); // Define a mensagem de erro
            toast.error('As senhas não coincidem!');
            return; // Interrompe o envio do formulário
        }

        // Se as senhas estiverem corretas, limpa qualquer erro anterior
        setErroSenha('');

        try {
            await authService.registrar(nome, email, senha, tipo);
            toast.success('Restaurante registrado com sucesso! Faça o login.');
            navigate('/login');
        } catch (error) {
            toast.error(error.message || 'Não foi possível registrar.');
        }
    };

    // 3. Define classes de estilo para os inputs de senha com base no erro
    const senhaInputClass = erroSenha
        ? "w-full px-3 py-2 mt-1 border border-red-500 rounded-md shadow-sm"
        : "w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm";

    return (
        <div className="flex items-center justify-center min-h-screen bg-orange-50">
            <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-lg shadow-md">
                <h2 className="text-2xl font-bold text-center text-gray-800">Crie sua Conta</h2>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Nome do Restaurante</label>
                        <input type="text" value={nome} onChange={(e) => setNome(e.target.value)} required className="w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm" />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email</label>
                        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required className="w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm" />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Qual o seu tipo de negócio?</label>
                        <select 
                            value={tipo} 
                            onChange={(e) => setTipo(e.target.value)} 
                            className="w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm bg-white"
                        >
                            <option value="APENAS_MESAS">Apenas Gestão de Mesas (Restaurante/Bar)</option>
                            <option value="APENAS_DELIVERY">Apenas Delivery</option>
                            <option value="MESAS_E_DELIVERY">Plano Completo (Mesas e Delivery)</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Senha</label>
                        <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required className={senhaInputClass} />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Confirme a Senha</label>
                        <input type="password" value={confirmaSenha} onChange={(e) => setConfirmaSenha(e.target.value)} required className={senhaInputClass} />
                    </div>

                    {/* 4. Exibe a mensagem de erro diretamente no formulário */}
                    {erroSenha && (
                        <p className="text-sm text-red-600 text-center">{erroSenha}</p>
                    )}

                    <button type="submit" className="w-full py-2 px-4 font-semibold text-white bg-orange-600 rounded-md hover:bg-orange-700">
                        Registrar
                    </button>
                </form>
                <p className="text-sm text-center text-gray-600">
                    Já tem uma conta?{' '}
                    <Link to="/login" className="font-medium text-orange-600 hover:underline">
                        Faça o login
                    </Link>
                </p>
            </div>
        </div>
    );
};

export default RegisterPage;