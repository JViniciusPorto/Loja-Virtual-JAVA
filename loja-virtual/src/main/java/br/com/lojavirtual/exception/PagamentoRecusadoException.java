package br.com.lojavirtual.exception;

public class PagamentoRecusadoException extends Exception {
    public PagamentoRecusadoException() {
        super("Pagamento não aprovado.");
    }
}
