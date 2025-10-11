package br.com.frevonamesa.frevonamesa.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura RuntimeExceptions (erros de regra de negócio, como limites) e retorna 400 Bad Request
     * com a mensagem no formato JSON.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        // Formata o erro em um objeto JSON
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("message", ex.getMessage()); // A mensagem da sua RuntimeException ("Limite de 30 pedidos...")

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}