package br.com.lojavirtual.exception;

public class TitularCartaoInvalidoException extends  IllegalArgumentException{

    public TitularCartaoInvalidoException() {
        super("Nome do titular não informado.");
    }
    
}
