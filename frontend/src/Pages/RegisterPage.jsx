import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import * as authService from '../services/authService';
import Header from '../components/Header';
import Footer from '../components/Footer';

const RegisterPage = () => {
    const [nome, setNome] = useState('');
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [endereco, setEndereco] = useState('');
    const [confirmaSenha, setConfirmaSenha] = useState('');
    const [erroSenha, setErroSenha] = useState('');
    const [tipo, setTipo] = useState('APENAS_MESAS');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (senha !== confirmaSenha) {
            setErroSenha('As senhas não coincidem!');
            toast.error('As senhas não coincidem!');
            return;
        }
        setErroSenha('');

        try {
            await authService.registrar(nome, email, senha, tipo, endereco);
            toast.success('Restaurante registrado com sucesso! Faça o login.');
            navigate('/login');
        } catch (error) {
            toast.error(error.message || 'Não foi possível registrar.');
        }
    };

    const inputClass = "w-full px-3 py-2 mt-1 border rounded-md shadow-sm dark:bg-gray-800 dark:text-white dark:border-gray-600 focus:ring-tema-primary focus:border-tema-primary";
    const senhaInputClass = erroSenha
        ? `${inputClass} border-red-500`
        : `${inputClass} border-gray-300`;

    return (
        <div className="flex flex-col min-h-screen bg-tema-fundo dark:bg-tema-fundo-dark">
            <Header />
            <div className="flex-grow flex items-center justify-center p-4">
                <div className="w-full max-w-md p-8 space-y-6 bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl border dark:border-gray-700">
                    <h2 className="text-2xl font-bold text-center text-tema-text dark:text-tema-text-dark">Crie sua Conta</h2>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Nome do Restaurante</label>
                            <input type="text" value={nome} onChange={(e) => setNome(e.target.value)} required className={inputClass} />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Email</label>
                            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required className={inputClass} />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Endereço Completo</label>
                            <input type="text" value={endereco} onChange={(e) => setEndereco(e.target.value)} required className={inputClass} />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Qual o seu tipo de negócio?</label>
                            <select
                                value={tipo}
                                onChange={(e) => setTipo(e.target.value)}
                                className={`${inputClass} bg-white dark:bg-gray-800`}
                            >
                                <option value="APENAS_MESAS">Apenas Gestão de Mesas (Restaurante/Bar)</option>
                                <option value="APENAS_DELIVERY">Apenas Delivery</option>
                                <option value="MESAS_E_DELIVERY">Plano Completo (Mesas e Delivery)</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Senha</label>
                            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required className={senhaInputClass} />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-tema-text-muted dark:text-tema-text-muted-dark">Confirme a Senha</label>
                            <input type="password" value={confirmaSenha} onChange={(e) => setConfirmaSenha(e.target.value)} required className={senhaInputClass} />
                        </div>

                        {erroSenha && (
                            <p className="text-sm text-red-600 text-center">{erroSenha}</p>
                        )}

                        <button type="submit" className="w-full py-2 px-4 font-semibold text-white bg-tema-primary rounded-md hover:bg-opacity-80 transition-colors">
                            Registrar
                        </button>
                    </form>
                    <p className="text-sm text-center text-tema-text-muted dark:text-tema-text-muted-dark">
                        Já tem uma conta?{' '}
                        <Link to="/login" className="font-medium text-tema-primary hover:underline">
                            Faça o login
                        </Link>
                    </p>
                </div>
            </div>
            <Footer />
        </div>
    );
};

export default RegisterPage;