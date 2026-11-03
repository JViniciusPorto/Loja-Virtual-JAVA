package br.com.lojavirtual.exception;

public class NumeroCartaoInvalidoException extends IllegalArgumentException {

    public NumeroCartaoInvalidoException() {
        super("Número do cartão inválido. O cartão deve possuir 16 dígitos.");
    }
    
}
