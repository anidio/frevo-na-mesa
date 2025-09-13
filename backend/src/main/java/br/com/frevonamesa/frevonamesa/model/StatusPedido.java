package br.com.frevonamesa.frevonamesa.model;

public enum StatusPedido {
    PENDENTE,      // Para a fila de impressão (mesa) ou novos pedidos (delivery)
    CONFIRMADO,    // Pedido de mesa que foi impresso e confirmado
    EM_PREPARO,    // Pedido de delivery que está na cozinha
    PRONTO_PARA_ENTREGA, // Pedido de delivery aguardando o motoboy
    FINALIZADO     // Pedido de delivery que foi entregue ou de mesa que foi pago
}