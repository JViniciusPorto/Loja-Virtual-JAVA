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

// Regras de negócio dos produtos (cadastrar, consultar, editar, remover) e as operações
// de estoque usadas na finalização da compra. Não lê nem grava arquivo diretamente:
// quem faz isso é o RepositorioProdutos.
//
// Toda alteração segue o mesmo padrão: valida -> altera na memória -> grava no arquivo.
// Se a gravação falhar, a alteração em memória é desfeita, então memória e arquivo
// nunca ficam diferentes.
public class ServicoProdutos {
    private final RepositorioProdutos repositorio;
    private final Map<Integer, Produto> produtos = new TreeMap<>(); // ordenado por id

    // Carrega os produtos do arquivo ao iniciar (o enunciado pede que os dados sejam recuperados)
    public ServicoProdutos(RepositorioProdutos repositorio) throws ErroPersistenciaException {
        this.repositorio = repositorio;
        for (Produto produto : repositorio.carregar()) {
            produtos.put(produto.getId(), produto);
        }
    }

    // ===================== CRUD =====================

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

    // Lista ordenada por id. A lista é uma cópia somente de leitura, mas os Produto dentro dela
    // são os mesmos objetos do sistema: para alterar um produto, usem sempre editar(...).
    public List<Produto> listar() {
        return Collections.unmodifiableList(new ArrayList<>(produtos.values()));
    }

    // O id não muda (é final em Produto); o resto é substituído pelos valores novos
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

    // ============ Estoque (usado pela finalização da compra) ============

    // Passo "atualizar o estoque" da compra. Tudo ou nada:
    //  1) confere TODOS os itens antes de mexer em qualquer um (se um faltar, nada muda);
    //  2) diminui o estoque em memória;
    //  3) grava o arquivo; se falhar, devolve as quantidades e lança ErroPersistenciaException.
    // A conferência usa os produtos DESTE serviço (pelo id), não o objeto guardado no carrinho:
    // assim, um produto removido ou com estoque alterado pelo administrador depois de entrar
    // no carrinho também é detectado.
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

    // Desfaz uma baixarEstoque que já foi gravada. Serve para a finalização da compra:
    // se o estoque foi baixado mas o registro da compra falhou depois, chama-se este método
    // para não ficar uma redução indevida. Produtos que não existem mais são ignorados.
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

    // ===================== Validações =====================

    // Tira espaços das pontas e troca qualquer sequência de espaços/TAB/quebras de linha por um
    // único espaço. Assim o nome nunca contém o TAB que separa os campos do arquivo.
    private String validarNome(String nome) throws ProdutoInvalidoException {
        if (nome == null || nome.isBlank()) {
            throw new ProdutoInvalidoException("O nome do produto não pode ser vazio.");
        }
        return nome.trim().replaceAll("\\s+", " ");
    }

    // Preço precisa ser maior que zero e é arredondado para centavos (2 casas)
    private double validarPreco(double preco) throws ProdutoInvalidoException {
        double arredondado = Math.round(preco * 100) / 100.0;
        if (Double.isNaN(preco) || Double.isInfinite(preco) || arredondado <= 0) {
            throw new ProdutoInvalidoException("Preço inválido: " + preco + ". Informe um valor maior que zero.");
        }
        return arredondado;
    }

    // Estoque zero é permitido (produto esgotado); negativo não
    private void validarEstoque(int estoque) throws QuantidadeInvalidaException {
        if (estoque < 0) {
            throw new QuantidadeInvalidaException(
                    "Estoque inválido: " + estoque + ". Informe um valor maior ou igual a zero.");
        }
    }
}
