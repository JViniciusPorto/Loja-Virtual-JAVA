package br.com.lojavirtual.model.pagamento;

public class PagamentoDebito extends  FormaPagamento{
    @Override
    public double calcularValorFinal(double valor) {
        return valor*0.9;
    }
    @Override
    public boolean processarPagamento() {
        return true;
    }
}
