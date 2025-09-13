import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient';

const RelatorioPage = () => {
    const [relatorio, setRelatorio] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchRelatorio = async () => {
            try {
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
                await apiClient.post('/api/caixa/fechar');
                toast.success("Caixa fechado e sistema reiniciado com sucesso!");
                navigate('/');
            } catch (error) {
                console.error("Erro de comunicação:", error);
                toast.error("Erro de comunicação com o servidor.");
            }
        }
    };

    if (loading) {
        return <div className="p-8 text-center font-semibold text-tema-text-muted dark:text-tema-text-muted-dark">Gerando relatório do dia...</div>;
    }

    if (!relatorio || (relatorio.faturamentoTotal === 0 && !relatorio.relatorioDelivery)) {
        return (
            <div className="w-full p-4 md:p-8 max-w-4xl mx-auto">
                 <div className="flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark mb-2">Relatório do Dia</h1>
                        <p className="text-tema-text-muted dark:text-tema-text-muted-dark">Nenhuma venda registrada hoje ainda para o seu restaurante.</p>
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
                    <h1 className="text-3xl font-bold text-tema-text dark:text-tema-text-dark">Relatório do Dia</h1>
                    <p className="text-lg text-tema-text-muted dark:text-tema-text-muted-dark">Resumo de vendas para: {new Date(relatorio.data).toLocaleDateString('pt-BR', { timeZone: 'UTC' })}</p>
                </div>
                <button
                    onClick={handleFecharCaixa}
                    className="bg-red-600 text-white font-bold py-2 px-4 rounded-lg hover:bg-red-700 transition-colors"
                >
                    Fechar Caixa e Reiniciar Dia
                </button>
            </div>

            <div className="bg-white dark:bg-tema-surface-dark p-6 rounded-xl shadow-lg space-y-4 border border-gray-200 dark:border-gray-700">
                <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark border-b dark:border-gray-700 pb-2">Resumo Financeiro</h2>
                <div className="flex justify-between items-center text-2xl font-bold text-tema-success">
                    <span>Faturamento Total (Mesas):</span>
                    <span>R$ {relatorio.faturamentoTotal.toFixed(2).replace('.', ',')}</span>
                </div>
                <div className="pt-4">
                    <h3 className="text-lg font-semibold text-tema-text-muted dark:text-tema-text-muted-dark">Detalhes por Pagamento (Mesas):</h3>
                    <div className="mt-2 space-y-1 text-tema-text dark:text-tema-text-dark">
                        {Object.entries(relatorio.faturamentoPorTipoPagamento).map(([tipo, valor]) => (
                            <div key={tipo} className="flex justify-between">
                                <span>{tipo.replace('_', ' ')}:</span>
                                <span className="font-semibold">R$ {valor.toFixed(2).replace('.', ',')}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="space-y-4">
                <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark">Mesas Atendidas</h2>
                {relatorio.mesasAtendidas.map(mesa => (
                    <div key={mesa.numeroMesa} className="bg-white dark:bg-tema-surface-dark p-4 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700">
                        <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark">Mesa {mesa.numeroMesa} {mesa.nomeCliente && `(${mesa.nomeCliente})`}</h3>
                        {mesa.pedidos.map((pedido, index) => (
                            <div key={pedido.id} className="mt-2 pl-4 border-l-2 border-gray-200 dark:border-gray-700">
                                <p className="font-semibold text-tema-text dark:text-tema-text-dark">Pedido #{index + 1} - {pedido.tipoPagamento.replace('_', ' ')} - Total: R$ {pedido.totalPedido.toFixed(2).replace('.', ',')}</p>
                                <ul className="list-disc pl-5 mt-1 text-sm text-tema-text-muted dark:text-tema-text-muted-dark">
                                    {pedido.itens.map((item, itemIndex) => (
                                        <li key={itemIndex}>
                                            {item.quantidade}x {item.nomeProduto}
                                            {item.observacao && <span className="italic text-tema-primary"> (Obs: {item.observacao})</span>}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                ))}
            </div>

            {relatorio.relatorioDelivery && (
                <div className="space-y-4">
                    <h2 className="text-xl font-bold text-tema-text dark:text-tema-text-dark pt-4 border-t dark:border-gray-700">Pedidos de Delivery</h2>
                    {relatorio.relatorioDelivery.pedidos.map(pedido => (
                        <div key={pedido.id} className="bg-white dark:bg-tema-surface-dark p-4 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700">
                            <h3 className="font-bold text-lg text-tema-text dark:text-tema-text-dark">Pedido #{pedido.id} ({pedido.nomeCliente})</h3>
                            <div className="mt-2 pl-4 border-l-2 border-gray-200 dark:border-gray-700">
                                <p className="font-semibold text-tema-text dark:text-tema-text-dark">{pedido.tipoPagamento.replace('_', ' ')} - Total: R$ {pedido.totalPedido.toFixed(2).replace('.', ',')}</p>
                                <ul className="list-disc pl-5 mt-1 text-sm text-tema-text-muted dark:text-tema-text-muted-dark">
                                    {pedido.itens.map((item, itemIndex) => (
                                        <li key={itemIndex}>
                                            {item.quantidade}x {item.nomeProduto}
                                            {item.observacao && <span className="italic text-tema-primary"> (Obs: {item.observacao})</span>}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default RelatorioPage;