import React from 'react';
import PedidoDeliveryCard from './PedidoDeliveryCard';

const Coluna = ({ titulo, pedidos, onStatusChange }) => (
    <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-3 flex-1 min-w-[300px]">
        <h3 className="font-bold text-tema-text dark:text-tema-text-dark mb-3">{titulo} ({pedidos.length})</h3>
        <div className="space-y-3">
            {pedidos.length > 0 ? (
                pedidos.map(pedido => (
                    <PedidoDeliveryCard key={pedido.id} pedido={pedido} onStatusChange={onStatusChange} />
                ))
            ) : (
                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark p-4 text-center">Nenhum pedido aqui.</p>
            )}
        </div>
    </div>
);

const PainelDelivery = ({ pedidos, onStatusChange }) => {
    // A API nos devolve um objeto com os pedidos já agrupados, então só precisamos renderizar
    const pendentes = pedidos['PENDENTE'] || [];
    const emPreparo = pedidos['EM_PREPARO'] || [];
    const prontos = pedidos['PRONTO_PARA_ENTREGA'] || [];

    return (
        <div className="flex gap-4 overflow-x-auto pb-4">
            <Coluna titulo="Novos Pedidos" pedidos={pendentes} onStatusChange={onStatusChange} />
            <Coluna titulo="Em Preparo" pedidos={emPreparo} onStatusChange={onStatusChange} />
            <Coluna titulo="Pronto para Entrega/Retirada" pedidos={prontos} onStatusChange={onStatusChange} />
        </div>
    );
};

export default PainelDelivery;