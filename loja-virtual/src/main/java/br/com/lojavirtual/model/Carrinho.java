package br.com.lojavirtual.model;
 
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Carrinho{
    
    private final Map<Integer, ItemCarrinho> itens = new LinkedHashMap<>();

    public void adicionar(Produto produto, int quantidade) throws QuantidadeInvalidaException, EstoqueInsuficienteException {
        
        validarQuantidadePositiva(quantidade);
        
        ItemCarrinho existente = itens.get(produto.getId());
        int quantidadeTotal = quantidade + (existente != null ? existente.getQuantidade() : 0);
        validarEstoque(produto, quantidadeTotal);

        if (existente != null) {
            existente.setQuantidade(quantidadeTotal);
        } else {
            itens.put(produto.getId(), new ItemCarrinho(produto, quantidade));
        }
     }
 

    public void remover(int idProduto) throws ProdutoNaoEncontradoException{
        if(itens.remove(idProduto) == null){
            throw new ProdutoNaoEncontradoException(idProduto);
        }
    }
    

    public void alterarQuantidade(int idProduto, int novaQuantidade) throws ProdutoNaoEncontradoException, QuantidadeInvalidaException, EstoqueInsuficienteException {
        ItemCarrinho item = itens.get(idProduto);
        if (item == null) {
            throw new ProdutoNaoEncontradoException(idProduto);
        }
        validarQuantidadePositiva(novaQuantidade);
        validarEstoque(item.getProduto(), novaQuantidade);
        item.setQuantidade(novaQuantidade);

    }
    

    public double calcularSubtotal(){
        double total = 0;
        for(ItemCarrinho item :itens.values()){
            total += item.getSubtotal();
        }
        return total;
    }

    public void verificarEstoque() throws EstoqueInsuficienteException {
        for(ItemCarrinho item : itens.values()){
            validarEstoque(item.getProduto(), item.getQuantidade());
        }
    }

    public boolean estaVazio(){
        return itens.isEmpty();
    }


    public void limpar(){
        itens.clear();
    } 


    public List<ItemCarrinho> getItens(){
        return Collections.unmodifiableList(new ArrayList<>(itens.values()));
    }

    @Override 
    public String toString(){
        if(itens.isEmpty()){
            return "Carrinho vazio";
        }
        StringBuilder sb = new StringBuilder();
        for(ItemCarrinho item : itens.values()){
            sb.append(item).append("\n");
        }
        sb.append(String.format("Subtotal: R$ %.2f", calcularSubtotal()));
        return sb.toString();
    }

    private void validarQuantidadePositiva(int quantidade) throws QuantidadeInvalidaException{
        if(quantidade <= 0){
            throw new QuantidadeInvalidaException("Quantidade inválida: " + quantidade + ". Informe um valor maior que zero.");


        }
    }

    private void validarEstoque(Produto produto, int quantidade) throws EstoqueInsuficienteException {
        if(quantidade > produto.getEstoque()){
            throw new EstoqueInsuficienteException(produto.getNome(), produto.getEstoque(), quantidade);
        }
    }

}