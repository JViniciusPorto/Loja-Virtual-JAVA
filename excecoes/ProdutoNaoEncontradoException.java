package excecoes;

public class ProdutoNaoEncontradoException extends Exception {
    public ProdutoNaoEncontradoException(int idProduto) {
        super("Produto com id " + idProduto + " não encontrado.");
    }
}