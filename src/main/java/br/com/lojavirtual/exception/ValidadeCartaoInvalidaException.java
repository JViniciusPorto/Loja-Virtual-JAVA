package br.com.lojavirtual.exception;

public class ValidadeCartaoInvalidaException extends IllegalArgumentException{

    public ValidadeCartaoInvalidaException() {
        super("Validade do cartão inválida. Utilize o formato MM/AA.");
    }

    
}
