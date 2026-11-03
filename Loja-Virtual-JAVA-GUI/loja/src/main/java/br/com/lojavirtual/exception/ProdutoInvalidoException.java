package br.com.lojavirtual.exception;

// Dados inválidos no cadastro/edição de um produto (id, nome ou preço).
// Estoque inválido usa a QuantidadeInvalidaException, que já existe.
public class ProdutoInvalidoException extends Exception {
    public ProdutoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
