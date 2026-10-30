package br.com.lojavirtual.model.pagamento;

public abstract class FormaPagamento {
    public abstract double calcularValorFinal(double valor);
    public abstract boolean processarPagamento();
}
