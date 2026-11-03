package br.com.lojavirtual.model.pagamento;

public class PagamentoPix implements FormaPagamento{
    @Override
    public double calcularValorFinal(double valor) {
        return valor*0.95;
    }
    @Override
    public boolean processarPagamento() {
        return true;
    }
}
