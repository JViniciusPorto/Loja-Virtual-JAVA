package br.com.lojavirtual.exception;

// A forma de pagamento foi analisada, mas o pagamento não foi aprovado.
// (Dados inválidos de cartão continuam sendo lançados pelas exceções próprias do cartão.)
public class PagamentoRecusadoException extends Exception {
    public PagamentoRecusadoException() {
        super("Pagamento não aprovado.");
    }
}
