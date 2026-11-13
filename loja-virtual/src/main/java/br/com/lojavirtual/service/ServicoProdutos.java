package br.com.lojavirtual.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import br.com.lojavirtual.model.ItemCarrinho;
import br.com.lojavirtual.model.Produto;
import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.ProdutoInvalidoException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.repository.RepositorioProdutos;

public class ServicoProdutos {
    private final RepositorioProdutos repositorio;
    private final Map<Integer, Produto> produtos = new TreeMap<>(); // ordenado por id

    public ServicoProdutos(RepositorioProdutos repositorio) throws ErroPersistenciaException {
        this.repositorio = repositorio;
        for (Produto produto : repositorio.carregar()) {
            produtos.put(produto.getId(), produto);
        }
    }

    public Produto cadastrar(String nome, double preco, int estoque)
            throws ProdutoInvalidoException, QuantidadeInvalidaException,
                   ErroPersistenciaException {
        int id = gerarIdUnico();
        String nomeTratado = validarNome(nome);
        double precoTratado = validarPreco(preco);
        validarEstoque(estoque);

        Produto novo = new Produto(id, nomeTratado, precoTratado, estoque);
        produtos.put(id, novo);
        try {
            repositorio.salvar(produtos.values());
        } catch (ErroPersistenciaException e) {
            produtos.remove(id);
            throw e;
        }
        return novo;
    }

    public Produto buscar(int id) throws ProdutoNaoEncontradoException {
        Produto produto = produtos.get(id);
        if (produto == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
        return produto;
    }

    public List<Produto> listar() {
        return Collections.unmodifiableList(new ArrayList<>(produtos.values()));
    }

    public Produto editar(int id, String novoNome, double novoPreco, int novoEstoque)
            throws ProdutoNaoEncontradoException, ProdutoInvalidoException,
                   QuantidadeInvalidaException, ErroPersistenciaException {
        Produto produto = buscar(id);
        String nomeTratado = validarNome(novoNome);
        double precoTratado = validarPreco(novoPreco);
        validarEstoque(novoEstoque);

        String nomeAnterior = produto.getNome();
        double precoAnterior = produto.getPreco();
        int estoqueAnterior = produto.getEstoque();

        produto.alterarNome(nomeTratado);
        produto.alterarPreco(precoTratado);
        produto.alterarEstoque(novoEstoque);
        try {
            repositorio.salvar(produtos.values());
        } catch (ErroPersistenciaException e) {
            produto.alterarNome(nomeAnterior);
            produto.alterarPreco(precoAnterior);
            produto.alterarEstoque(estoqueAnterior);
            throw e;
        }
        return produto;
    }

    public void remover(int id) throws ProdutoNaoEncontradoException, ErroPersistenciaException {
        Produto removido = produtos.remove(id);
        if (removido == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
        try {
            repositorio.salvar(produtos.values());
        } catch (ErroPersistenciaException e) {
            produtos.put(id, removido);
            throw e;
        }
    }

    private int gerarIdUnico() {
        int id;

        do {
            id = ThreadLocalRandom.current().nextInt(1000, 10000);
        } while (produtos.containsKey(id));

        return id;
    }

    public void baixarEstoque(List<ItemCarrinho> itens)
            throws ProdutoNaoEncontradoException, EstoqueInsuficienteException,
                   QuantidadeInvalidaException, ErroPersistenciaException {
        Map<Integer, Integer> pedido = somarQuantidadesPorId(itens);

        for (Map.Entry<Integer, Integer> entrada : pedido.entrySet()) {
            Produto produto = buscar(entrada.getKey());
            int quantidade = entrada.getValue();
            if (quantidade <= 0) {
                throw new QuantidadeInvalidaException(
                        "Quantidade inválida: " + quantidade + ". Informe um valor maior que zero.");
            }
            if (quantidade > produto.getEstoque()) {
                throw new EstoqueInsuficienteException(produto.getNome(), produto.getEstoque(), quantidade);
            }
        }

        for (Map.Entry<Integer, Integer> entrada : pedido.entrySet()) {
            Produto produto = produtos.get(entrada.getKey());
            produto.alterarEstoque(produto.getEstoque() - entrada.getValue());
        }

        try {
            repositorio.salvar(produtos.values());
        } catch (ErroPersistenciaException e) {
            for (Map.Entry<Integer, Integer> entrada : pedido.entrySet()) {
                Produto produto = produtos.get(entrada.getKey());
                produto.alterarEstoque(produto.getEstoque() + entrada.getValue());
            }
            throw e;
        }
    }

    public void reporEstoque(List<ItemCarrinho> itens) throws ErroPersistenciaException {
        Map<Integer, Integer> devolucao = somarQuantidadesPorId(itens);

        for (Map.Entry<Integer, Integer> entrada : devolucao.entrySet()) {
            Produto produto = produtos.get(entrada.getKey());
            if (produto != null) {
                produto.alterarEstoque(produto.getEstoque() + entrada.getValue());
            }
        }

        try {
            repositorio.salvar(produtos.values());
        } catch (ErroPersistenciaException e) {
            for (Map.Entry<Integer, Integer> entrada : devolucao.entrySet()) {
                Produto produto = produtos.get(entrada.getKey());
                if (produto != null) {
                    produto.alterarEstoque(produto.getEstoque() - entrada.getValue());
                }
            }
            throw e;
        }
    }

    private Map<Integer, Integer> somarQuantidadesPorId(List<ItemCarrinho> itens) {
        Map<Integer, Integer> soma = new LinkedHashMap<>();
        for (ItemCarrinho item : itens) {
            soma.merge(item.getProduto().getId(), item.getQuantidade(), Integer::sum);
        }
        return soma;
    }

    private String validarNome(String nome) throws ProdutoInvalidoException {
        if (nome == null || nome.isBlank()) {
            throw new ProdutoInvalidoException("O nome do produto não pode ser vazio.");
        }
        return nome.trim().replaceAll("\\s+", " ");
    }

    private double validarPreco(double preco) throws ProdutoInvalidoException {
        double arredondado = Math.round(preco * 100) / 100.0;
        if (Double.isNaN(preco) || Double.isInfinite(preco) || arredondado <= 0) {
            throw new ProdutoInvalidoException("Preço inválido: " + preco + ". Informe um valor maior que zero.");
        }
        return arredondado;
    }

    private void validarEstoque(int estoque) throws QuantidadeInvalidaException {
        if (estoque < 0) {
            throw new QuantidadeInvalidaException(
                    "Estoque inválido: " + estoque + ". Informe um valor maior ou igual a zero.");
        }
    }
}
