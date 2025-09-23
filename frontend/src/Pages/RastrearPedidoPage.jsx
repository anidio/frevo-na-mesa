import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { FaCheckCircle, FaClipboardCheck, FaUtensils, FaMotorcycle } from 'react-icons/fa';
import apiClient from '../services/apiClient';

const RastrearPedidoPage = () => {
  const { uuid } = useParams(); // CORRIGIDO: Use `uuid` do parâmetro
  const [pedido, setPedido] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchPedido = async () => {
      try {
        // CORREÇÃO AQUI: Use o endpoint público correto com o UUID
        const response = await apiClient.get(`/api/publico/pedido/${uuid}/rastrear`);
        setPedido(response); // ATUALIZADO: a resposta do apiClient já é o dado, não response.data
      } catch (err) {
        setError('Não foi possível carregar o pedido.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchPedido();
    const interval = setInterval(fetchPedido, 5000);

    return () => clearInterval(interval);
  }, [uuid]); // ATUALIZADO: o useEffect depende do `uuid`

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 dark:bg-gray-900 text-gray-900 dark:text-white">
        Carregando...
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 dark:bg-gray-900 text-red-500">
        {error}
      </div>
    );
  }

  if (!pedido) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 dark:bg-gray-900 text-gray-900 dark:text-white">
        Pedido não encontrado.
      </div>
    );
  }

  const steps = [
    { name: 'Pedido enviado', status: 'PENDENTE', icon: FaClipboardCheck },
    { name: 'Pedido aceito/preparando', status: 'EM_PREPARO', icon: FaUtensils },
    { name: 'Pronto para entrega', status: 'PRONTO_PARA_ENTREGA', icon: FaMotorcycle },
    { name: 'Pedido entregue', status: 'FINALIZADO', icon: FaCheckCircle },
  ];

  const statusIndex = steps.findIndex(step => step.status === pedido.status);

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-tema-fundo-dark p-4 md:p-8">
      <div className="container mx-auto max-w-4xl bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 md:p-8">
        <h1 className="text-3xl font-bold text-center text-tema-text dark:text-tema-text-dark mb-6">
          Rastrear Pedido #{pedido.id}
        </h1>
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-xl font-semibold text-tema-text dark:text-tema-text-dark">
            Status do Pedido
          </h2>
        </div>
        <div className="flex justify-between items-start space-x-2 md:space-x-4 mb-8">
          {steps.map((step, index) => (
            <div key={index} className="flex flex-col items-center flex-1">
              <div
                className={`flex items-center justify-center w-12 h-12 rounded-full transition-colors duration-300 ${
                  index <= statusIndex ? 'bg-green-500' : 'bg-gray-300 dark:bg-gray-600'
                }`}
              >
                <step.icon className="text-white text-2xl" />
              </div>
              <div className="text-center mt-2">
                <span
                  className={`text-sm font-medium transition-colors duration-300 ${
                    index <= statusIndex ? 'text-gray-900 dark:text-white' : 'text-gray-500 dark:text-gray-400'
                  }`}
                >
                  {step.name}
                </span>
              </div>
            </div>
          ))}
        </div>
        <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg shadow-inner">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200 mb-2">Detalhes do Pedido</h3>
          <p className="text-gray-600 dark:text-gray-300 mb-1">
            <strong className="font-semibold">Valor Total:</strong> R$ {pedido.total.toFixed(2).replace('.', ',')}
          </p>
          <p className="text-gray-600 dark:text-gray-300">
            <strong className="font-semibold">Itens:</strong>
          </p>
          <ul className="list-disc list-inside ml-4 mt-2 text-gray-600 dark:text-gray-300">
            {pedido.itens.map((item, index) => (
              <li key={item.id}>
                {item.quantidade} x {item.produto.nome}
                {item.observacao && (
                  <span className="text-sm italic ml-2">
                    (Obs: {item.observacao})
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
};

export default RastrearPedidoPage;