import React from 'react';

const PedidoDetalhesModal = ({ pedido, onClose }) => {
  if (!pedido) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50" onClick={onClose}>
      <div className="bg-white dark:bg-tema-surface-dark rounded-lg shadow-xl p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
        <h2 className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mb-4">
          Detalhes do Pedido #{pedido.id}
        </h2>
        
        <div className="space-y-3 text-sm">
          <p><span className="font-semibold">Cliente:</span> {pedido.nomeClienteDelivery}</p>
          <p><span className="font-semibold">Telefone:</span> {pedido.telefoneClienteDelivery}</p>
          <p><span className="font-semibold">Endereço:</span> {pedido.enderecoClienteDelivery}</p>
          <p><span className="font-semibold">Data:</span> {new Date(pedido.dataHora).toLocaleString('pt-BR')}</p>
        </div>

        <div className="mt-4 pt-4 border-t dark:border-gray-700">
          <h3 className="font-semibold mb-2">Itens do Pedido:</h3>
          <ul className="space-y-1 text-sm">
            {pedido.itens.map(item => (
              <li key={item.id} className="flex justify-between">
                <span>{item.quantidade}x {item.produto.nome}</span>
                <span>R$ {(item.precoUnitario * item.quantidade).toFixed(2).replace('.', ',')}</span>
              </li>
            ))}
          </ul>
        </div>
        
        <div className="flex justify-between items-center text-xl font-bold mt-4 pt-4 border-t dark:border-gray-700">
          <span>Total:</span>
          <span className="text-tema-success">R$ {pedido.total.toFixed(2).replace('.', ',')}</span>
        </div>

        <div className="mt-6 flex justify-end">
          <button onClick={onClose} className="px-4 py-2 rounded-lg text-white bg-tema-primary hover:bg-opacity-80 font-semibold">
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};

export default PedidoDetalhesModal;