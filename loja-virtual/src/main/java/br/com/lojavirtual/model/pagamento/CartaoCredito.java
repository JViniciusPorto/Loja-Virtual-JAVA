package br.com.lojavirtual.model.pagamento;

public class CartaoCredito extends PagamentoCartao {
    public CartaoCredito(String numero, String nomeTitular, String validade, String cvv) {
        super(numero, nomeTitular, validade, cvv);
    }
    
    @Override
    public double calcularValorFinal(double valor) {
        return valor;
    }

    @Override
    public boolean processarPagamento() {
        super.dadosValidos();
        return  true;
    }
}
