package br.com.lojavirtual.model.pagamento;

public interface FormaPagamento {
    double calcularValorFinal(double valor);
    boolean processarPagamento(); 
}
