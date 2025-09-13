import React from 'react';

const PrinterIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm7-5a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2h2" /></svg>;
const UserIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>;

const PedidoDeliveryCard = ({ pedido, onStatusChange, onImprimir }) => {
    const statusInfo = {
        PENDENTE: {
            borderColor: 'border-blue-500',
            buttonText: 'Iniciar Preparo',
            nextStatus: 'EM_PREPARO',
        },
        EM_PREPARO: {
            borderColor: 'border-yellow-500',
            buttonText: 'Finalizar e Enviar',
            nextStatus: 'PRONTO_PARA_ENTREGA',
        },
        PRONTO_PARA_ENTREGA: {
            borderColor: 'border-green-500',
            buttonText: 'Marcar como Entregue',
            nextStatus: 'FINALIZADO',
        },
    };

    const info = statusInfo[pedido.status];

    if (!info) return null;

    return (
        <div className={`bg-white dark:bg-tema-surface-dark p-4 rounded-lg shadow-md border-l-4 ${info.borderColor}`}>
            <div className="flex justify-between items-start gap-2">
                <div>
                    <p className="font-bold text-tema-text dark:text-tema-text-dark flex items-center gap-2">
                        <UserIcon /> {pedido.nomeClienteDelivery}
                    </p>
                    <p className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark">{pedido.telefoneClienteDelivery}</p>
                    <p className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark mt-1">{pedido.enderecoClienteDelivery}</p>
                </div>
                <div className="flex flex-col gap-2">
                    <button
                        onClick={() => onStatusChange(pedido.id, info.nextStatus)}
                        className="bg-tema-primary text-white font-bold py-2 px-3 rounded-lg hover:bg-opacity-80 transition-colors text-sm whitespace-nowrap"
                    >
                        {info.buttonText}
                    </button>
                    {pedido.status === 'PENDENTE' && (
                        <button
                            onClick={() => onImprimir(pedido)}
                            className="bg-gray-600 text-white font-bold py-2 px-3 rounded-lg hover:bg-opacity-80 flex items-center justify-center gap-2 text-sm"
                        >
                            <PrinterIcon />
                        </button>
                    )}
                </div>
            </div>
            <ul className="mt-2 text-sm text-tema-text dark:text-tema-text-dark list-disc list-inside space-y-1">
                {pedido.itens.map(item => (
                    <li key={item.id}>
                        {item.quantidade}x {item.produto.nome}
                        {item.observacao && <span className="italic text-tema-primary"> (Obs: {item.observacao})</span>}
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default PedidoDeliveryCard;