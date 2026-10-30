package br.com.lojavirtual.model.pagamento;

public class PagamentoCredito extends  FormaPagamento{
    @Override
    public double calcularValorFinal(double valor) {
        return  valor;
    }
    @Override
    public boolean processarPagamento() {
        return true;
    }
}
