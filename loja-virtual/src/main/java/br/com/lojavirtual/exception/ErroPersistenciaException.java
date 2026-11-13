package br.com.lojavirtual.exception;

public class ErroPersistenciaException extends Exception {
    public ErroPersistenciaException(String mensagem) {
        super(mensagem);
    }

    public ErroPersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
