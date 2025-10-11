import React from 'react';
import PedidoDeliveryCard from './PedidoDeliveryCard';

// Adicione onOpenUpgradeModal como prop
const Coluna = ({ titulo, pedidos, onStatusChange, onImprimir, userProfile, onOpenUpgradeModal }) => (
    <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-3 flex-1 min-w-[300px]">
        <h3 className="font-bold text-tema-text dark:text-tema-text-dark mb-3">{titulo} ({pedidos.length})</h3>
        <div className="space-y-3">
            {pedidos.length > 0 ? (
                pedidos.map(pedido => (
                    <PedidoDeliveryCard 
                      key={pedido.id} 
                      pedido={pedido} 
                      onStatusChange={onStatusChange}
                      onImprimir={onImprimir}
                      userProfile={userProfile}
                      onOpenUpgradeModal={onOpenUpgradeModal} // NOVO PROP PASSADO AQUI
                    />
                ))
            ) : (
                <p className="text-sm text-tema-text-muted dark:text-tema-text-muted-dark p-4 text-center">Nenhum pedido aqui.</p>
            )}
        </div>
    </div>
);

// Adicione onOpenUpgradeModal como prop
const PainelDelivery = ({ pedidos, onStatusChange, onImprimir, userProfile, onOpenUpgradeModal }) => {
    const pendentes = pedidos['PENDENTE'] || [];
    const emPreparo = pedidos['EM_PREPARO'] || [];
    const prontos = pedidos['PRONTO_PARA_ENTREGA'] || [];

    return (
        <div className="flex gap-4 overflow-x-auto pb-4">
            {/* Passe o prop para cada coluna */}
            <Coluna titulo="Novos Pedidos" pedidos={pendentes} onStatusChange={onStatusChange} onImprimir={onImprimir} userProfile={userProfile} onOpenUpgradeModal={onOpenUpgradeModal} />
            <Coluna titulo="Em Preparo" pedidos={emPreparo} onStatusChange={onStatusChange} onImprimir={onImprimir} userProfile={userProfile} onOpenUpgradeModal={onOpenUpgradeModal} />
            {/* O modal só é relevante nas colunas PENDENTE e EM PREPARO, mas passamos para ser seguro */}
            <Coluna titulo="Pronto para Entrega/Retirada" pedidos={prontos} onStatusChange={onStatusChange} onOpenUpgradeModal={onOpenUpgradeModal} />
        </div>
    );
};

export default PainelDelivery;