package br.com.lojavirtual.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.lojavirtual.exception.CarrinhoVazioException;
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.model.Carrinho;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCarrinho;
import br.com.lojavirtual.model.ItemCompra;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.repository.RepositorioCompra;

public class ServicoCompra {
    public Compra finalizarCompra(Carrinho carrinho, String nomeComprador, FormaPagamento pagamento) throws EstoqueInsuficienteException, IOException{
        if (carrinho == null || carrinho.estaVazio()){
            throw new CarrinhoVazioException();
        }

        carrinho.verificarEstoque();

        List<ItemCompra> itensCompra = converterItens(carrinho);

        RepositorioCompra gerenciador = new RepositorioCompra();
        
        List<String> relatorio = gerenciador.carregarRelatorio();
        
        int idCompra = gerenciador.gerarIdCompra(relatorio);
        
        Compra compra = new Compra(idCompra,LocalDateTime.now(), nomeComprador, itensCompra, pagamento);
        
        compra.calcularSubtotal();
        
        compra.calcularValorFinal();
        
        compra.processarPagamento();
        
        gerenciador.salvarCompra(compra);

        atualizarEstoque(carrinho);

        return compra;
    }

    private List<ItemCompra> converterItens(Carrinho carrinho) {
        List<ItemCompra> itensCompra = new ArrayList<>();

        for (ItemCarrinho item : carrinho.getItens()) {
            itensCompra.add(new ItemCompra(item.getProduto().getId(),item.getProduto().getNome(),item.getQuantidade(),item.getProduto().getPreco()));
        }

        return itensCompra;
    }

    private void atualizarEstoque(Carrinho carrinho) {
        for (ItemCarrinho item : carrinho.getItens()) {
            item.getProduto().reduzirEstoque(item.getQuantidade());
        }
    }
}
