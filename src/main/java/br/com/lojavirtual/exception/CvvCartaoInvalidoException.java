package br.com.lojavirtual.exception;

public class CvvCartaoInvalidoException extends IllegalArgumentException{

    public CvvCartaoInvalidoException() {
        super("CVV inválido. O CVV deve possuir 3 dígitos.");
    }
    
}
