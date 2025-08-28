import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient'; // 1. IMPORTAR O API CLIENT

const RelatorioPage = () => {
    const [relatorio, setRelatorio] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchRelatorio = async () => {
            try {
                // 2. USAR O API CLIENT PARA BUSCAR O RELATÓRIO
                const data = await apiClient.get('/api/relatorios/hoje');
                setRelatorio(data);
            } catch (error) {
                console.error("Erro ao buscar relatório:", error);
                toast.error("Não foi possível carregar o relatório.");
            } finally {
                setLoading(false);
            }
        };
        fetchRelatorio();
    }, []);

    const handleFecharCaixa = async () => {
        const confirm = window.confirm(
            "ATENÇÃO!\n\nVocê tem certeza que deseja fechar o caixa?\n\nEsta ação é IRREVERSÍVEL e irá apagar todos os pedidos e mesas do SEU RESTAURANTE para iniciar um novo dia."
        );

        if (confirm) {
            try {
                // 3. USAR O API CLIENT PARA FECHAR O CAIXA
                await apiClient.post('/api/caixa/fechar');
                toast.success("Caixa fechado e sistema reiniciado com sucesso!");
                navigate('/'); // Redireciona para a Home
            } catch (error) {
                console.error("Erro de comunicação:", error);
                toast.error("Erro de comunicação com o servidor.");
            }
        }
    };

    if (loading) {
        return <div className="p-8 text-center font-semibold">Gerando relatório do dia...</div>;
    }

    if (!relatorio || relatorio.faturamentoTotal === 0) {
        return (
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
                 <div className="flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-800 mb-2">Relatório do Dia</h1>
                        <p className="text-gray-500">Nenhuma venda registrada hoje ainda para o seu restaurante.</p>
                    </div>
                    <button
                        onClick={handleFecharCaixa}
                        className="bg-red-600 text-white font-bold py-2 px-4 rounded-lg hover:bg-red-700 transition-colors"
                    >
                        Fechar Caixa e Reiniciar Dia
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full p-4 md:p-8 max-w-4xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Relatório do Dia</h1>
                    <p className="text-lg text-gray-500">Resumo de vendas para: {new Date(relatorio.data).toLocaleDateString('pt-BR', { timeZone: 'UTC' })}</p>
                </div>
                <button
                    onClick={handleFecharCaixa}
                    className="bg-red-600 text-white font-bold py-2 px-4 rounded-lg hover:bg-red-700 transition-colors"
                >
                    Fechar Caixa e Reiniciar Dia
                </button>
            </div>

            {/* --- RESUMO FINANCEIRO --- */}
            <div className="bg-white p-6 rounded-lg shadow-md space-y-4">
                <h2 className="text-xl font-bold text-gray-700 border-b pb-2">Resumo Financeiro</h2>
                <div className="flex justify-between items-center text-2xl font-bold text-green-600">
                    <span>Faturamento Total:</span>
                    <span>R$ {relatorio.faturamentoTotal.toFixed(2).replace('.', ',')}</span>
                </div>
                <div className="pt-4">
                    <h3 className="text-lg font-semibold text-gray-600">Detalhes por Pagamento:</h3>
                    <div className="mt-2 space-y-1 text-gray-700">
                        {Object.entries(relatorio.faturamentoPorTipoPagamento).map(([tipo, valor]) => (
                            <div key={tipo} className="flex justify-between">
                                <span>{tipo.replace('_', ' ')}:</span>
                                <span className="font-semibold">R$ {valor.toFixed(2).replace('.', ',')}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* --- DETALHAMENTO POR MESA --- */}
            <div className="space-y-4">
                <h2 className="text-xl font-bold text-gray-700">Mesas Atendidas</h2>
                {relatorio.mesasAtendidas.map(mesa => (
                    <div key={mesa.numeroMesa} className="bg-white p-4 rounded-lg shadow-md">
                        <h3 className="font-bold text-lg">Mesa {mesa.numeroMesa} {mesa.nomeCliente && `(${mesa.nomeCliente})`}</h3>
                        {mesa.pedidos.map((pedido, index) => (
                            <div key={pedido.id} className="mt-2 pl-4 border-l-2">
                                <p className="font-semibold">Pedido #{index + 1} - {pedido.tipoPagamento.replace('_', ' ')} - Total: R$ {pedido.totalPedido.toFixed(2).replace('.', ',')}</p>
                                <ul className="list-disc pl-5 mt-1 text-sm text-gray-600">
                                    {pedido.itens.map((item, itemIndex) => (
                                        <li key={itemIndex}>
                                            {item.quantidade}x {item.nomeProduto}
                                            {item.observacao && <span className="italic text-orange-700"> (Obs: {item.observacao})</span>}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default RelatorioPage;