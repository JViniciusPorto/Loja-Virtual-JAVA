package br.com.lojavirtual.model.pagamento;

public class CartaoDebito extends PagamentoCartao{
    public CartaoDebito(String numero, String nomeTitular, String validade, String cvv) {
        super(numero, nomeTitular, validade, cvv);
    }
    
    @Override
    public double calcularValorFinal(double valor) {
        return valor*0.9;
    }

    @Override
    public boolean processarPagamento() {
        super.dadosValidos();
        return  true;
    }
}
