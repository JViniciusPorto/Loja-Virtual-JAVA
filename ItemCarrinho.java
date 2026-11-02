package modelos;

public class ItemCarrinho {
    private final Produto produto;
    private int quantidade;
 
    public ItemCarrinho(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Produto getProduto(){ 
        return produto;
     }

    public int getQuantidade(){ 
        return quantidade;
     }
 
    void setQuantidade(int quantidade){ 
        this.quantidade = quantidade;
    }
 
    public double getSubtotal() {
        return produto.getPreco() * quantidade;
    }
 
    @Override
    public String toString() {
        return String.format("[%d] %-20s %3d x R$ %8.2f = R$ %9.2f", produto.getId(), produto.getNome(), quantidade, produto.getPreco(), getSubtotal());
    }
}
 