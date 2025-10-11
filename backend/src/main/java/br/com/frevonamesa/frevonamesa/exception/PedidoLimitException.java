package br.com.frevonamesa.frevonamesa.exception;

public class PedidoLimitException extends RuntimeException {
    private final int currentCount;
    private final int limit;

    public PedidoLimitException(String message, int currentCount, int limit) {
        super(message);
        this.currentCount = currentCount;
        this.limit = limit;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getLimit() {
        return limit;
    }
}