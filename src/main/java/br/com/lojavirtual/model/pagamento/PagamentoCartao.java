package br.com.lojavirtual.model.pagamento;

import br.com.lojavirtual.exception.CvvCartaoInvalido;
import br.com.lojavirtual.exception.NumeroCartaoInvalidoException;
import br.com.lojavirtual.exception.TitularCartaoInvalidoException;
import br.com.lojavirtual.exception.ValidadeCartaoInvalidaException;

public abstract class PagamentoCartao implements FormaPagamento{
    private String numero;
    private String nomeTitular;
    private String validade;
    private String cvv;

    public  PagamentoCartao(String numero, String nomeTitular, String validade, String cvv){
        this.numero = numero;
        this.nomeTitular = nomeTitular;
        this.validade = validade;
        this.cvv = cvv;
    }

    protected void dadosValidos() {
        numeroValido();
        titularValido();
        cvvValido();
        validadeValida();
    }

    private void numeroValido() {
        if (numero == null || !numero.matches("\\d{16}")){
            throw new NumeroCartaoInvalidoException();
        }
    }

    private void titularValido() {
        if (nomeTitular == null || nomeTitular.isBlank()){
            throw new TitularCartaoInvalidoException();
        }
    }

    private void cvvValido() {
        if (cvv == null || !cvv.matches("\\d{3}")){
            throw new CvvCartaoInvalido();
        }
    }

    private void validadeValida() {
        if (validade == null || !validade.matches("(0[1-9]|1[0-2])/\\d{2}")){
            throw new ValidadeCartaoInvalidaException();
        }
    }

    public String getNumero() {
        return numero;
    }

    public String getNomeTitular() {
        return nomeTitular;
    }

    public String getValidade() {
        return validade;
    }

    public String getCvv() {
        return cvv;
    }
    
}
