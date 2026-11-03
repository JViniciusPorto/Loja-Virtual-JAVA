package br.com.lojavirtual.exception;

// Falha ao ler ou gravar um arquivo de dados (produtos, compras...).
// Guarda a causa original (IOException, NumberFormatException...) para facilitar a depuração.
public class ErroPersistenciaException extends Exception {
    public ErroPersistenciaException(String mensagem) {
        super(mensagem);
    }

    public ErroPersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
