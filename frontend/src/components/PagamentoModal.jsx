import React, { useState } from 'react';

const PagamentoModal = ({ mesa, onClose, onConfirm }) => {
  if (!mesa) return null;

  // Estado para guardar a forma de pagamento selecionada. Começa com 'DINHEIRO'.
  const [tipoPagamento, setTipoPagamento] = useState('DINHEIRO');

  return (
    // Backdrop (fundo escuro semi-transparente)
    <div 
      className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50"
      onClick={onClose} // Fecha o modal se clicar fora
    >
      {/* Painel do Modal */}
      <div 
        className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md"
        onClick={(e) => e.stopPropagation()} // Impede que o clique dentro do modal o feche
      >
        <h2 className="text-2xl font-bold text-gray-800 mb-4">
          Confirmar Pagamento - Mesa {mesa.numero}
        </h2>
        
        <div className="space-y-2 text-lg">
          <div className="flex justify-between items-center border-b pb-2">
            <span className="text-gray-600">Status Atual:</span>
            <span className="font-semibold text-yellow-600">{mesa.status}</span>
          </div>
          <div className="flex justify-between items-center text-2xl font-bold pt-2">
            <span className="text-gray-800">Total a Pagar:</span>
            <span className="text-green-600">R$ {mesa.valorTotal.toFixed(2).replace('.', ',')}</span>
          </div>
        </div>

        {/* Opções de Pagamento */}
        <div className="mt-6">
          <p className="font-semibold text-gray-700 mb-2">Forma de Pagamento:</p>
          <div className="flex gap-2">
            <button onClick={() => setTipoPagamento('DINHEIRO')} className={`flex-1 py-2 border rounded-lg transition-colors ${tipoPagamento === 'DINHEIRO' ? 'bg-orange-500 text-white border-orange-500' : 'hover:bg-gray-100'}`}>Dinheiro</button>
            <button onClick={() => setTipoPagamento('CARTAO_DEBITO')} className={`flex-1 py-2 border rounded-lg transition-colors ${tipoPagamento === 'CARTAO_DEBITO' ? 'bg-orange-500 text-white border-orange-500' : 'hover:bg-gray-100'}`}>Cartão</button>
            <button onClick={() => setTipoPagamento('PIX')} className={`flex-1 py-2 border rounded-lg transition-colors ${tipoPagamento === 'PIX' ? 'bg-orange-500 text-white border-orange-500' : 'hover:bg-gray-100'}`}>PIX</button>
          </div>
        </div>

        {/* Botões de Ação */}
        <div className="mt-8 flex justify-end gap-4">
          <button 
            onClick={onClose}
            className="px-6 py-2 rounded-lg text-gray-700 bg-gray-200 hover:bg-gray-300 font-semibold"
          >
            Cancelar
          </button>
          <button 
            onClick={() => onConfirm(mesa.id, tipoPagamento)}
            className="px-6 py-2 rounded-lg text-white bg-green-600 hover:bg-green-700 font-semibold"
          >
            Confirmar Pagamento
          </button>
        </div>
      </div>
    </div>
  );
};

export default PagamentoModal;