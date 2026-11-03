package br.com.lojavirtual.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.lojavirtual.exception.CarrinhoVazioException;
import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.PagamentoRecusadoException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.Carrinho;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCarrinho;
import br.com.lojavirtual.model.ItemCompra;
import br.com.lojavirtual.model.ResumoCompra;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.repository.RepositorioCompra;

// Orquestra a finalização da compra. Não lê nem grava arquivo diretamente:
//  - estoque  -> ServicoProdutos (que usa o RepositorioProdutos)
//  - compras  -> RepositorioCompra
//
// Estratégia de consistência (enunciado, item 5): tudo que pode falhar sem deixar rastro
// vem ANTES de qualquer gravação; depois, as duas gravações têm desfazer:
//   1) validações e pagamento (nada é gravado ainda; se falhar, nada mudou);
//   2) baixarEstoque (tudo ou nada, grava produtos.txt; se falhar, já se desfaz sozinho);
//   3) salvarCompra (relatório + histórico; se falhar, desfaz o que gravou);
//   4) se o passo 3 falhar, reporEstoque devolve o estoque do passo 2.
// Limite: isso cobre erros (exceções). Uma queda do computador exatamente entre o passo 2 e
// o 3 não é coberta, pois exigiria transação de verdade.
//
// Exceções sem "throws" (IllegalArgumentException): CarrinhoVazioException e as de cartão
// (número, CVV, titular, validade) -> quem chama (menu) precisa tratá-las também.
public class ServicoCompra {
    private final ServicoProdutos servicoProdutos;
    private final RepositorioCompra repositorio;

    // Deve receber o MESMO ServicoProdutos usado pelo administrador e pelo carrinho:
    // ele guarda os produtos em memória, e uma segunda instância não enxergaria o estoque atual.
    public ServicoCompra(ServicoProdutos servicoProdutos, RepositorioCompra repositorio) {
        this.servicoProdutos = servicoProdutos;
        this.repositorio = repositorio;
    }

    public Compra finalizarCompra(Carrinho carrinho, String nomeComprador, FormaPagamento pagamento)
            throws EstoqueInsuficienteException, ProdutoNaoEncontradoException,
                   QuantidadeInvalidaException, PagamentoRecusadoException,
                   ErroPersistenciaException {

        // 1) carrinho com produtos (e dados básicos informados)
        if (carrinho == null || carrinho.estaVazio()) {
            throw new CarrinhoVazioException();
        }
        String comprador = validarComprador(nomeComprador);
        if (pagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento não informada.");
        }

        // 2) estoque suficiente para todos os itens (confere de novo, como pede o enunciado)
        carrinho.verificarEstoque();

        // 3) a 5) subtotal, desconto e pagamento: ainda sem gravar nada
        List<ItemCarrinho> itensCarrinho = carrinho.getItens();
        int idCompra = repositorio.gerarIdCompra(repositorio.carregarRelatorio());

        Compra compra = new Compra(idCompra, LocalDateTime.now(), comprador,
                converterItens(itensCarrinho), pagamento);
        compra.calcularSubtotal();
        compra.calcularValorFinal();

        if (!compra.processarPagamento()) {
            throw new PagamentoRecusadoException();
        }

        // 6) baixa o estoque e já grava produtos.txt (confere tudo antes de mexer em qualquer item)
        servicoProdutos.baixarEstoque(itensCarrinho);

        // 7) registra a compra; se não der, devolve o estoque
        try {
            repositorio.salvarCompra(compra);
        } catch (ErroPersistenciaException e) {
            desfazerBaixaDeEstoque(itensCarrinho, e);
            throw e;
        }

        carrinho.limpar();
        return compra;
    }

    // ===================== Consultas (histórico do comprador) =====================

    private static final Locale BRASIL = Locale.of("pt", "BR");

    // Compras do comprador: ID, data/hora, forma de pagamento e valor final (vazio se não comprou nada)
    public List<ResumoCompra> consultarHistorico(String nomeComprador) throws ErroPersistenciaException {
        return repositorio.carregarResumoHistorico(validarComprador(nomeComprador));
    }

    // Total gasto em todas as compras do comprador
    public double calcularTotalGasto(String nomeComprador) throws ErroPersistenciaException {
        return repositorio.calcularTotalGasto(repositorio.carregarHistorico(validarComprador(nomeComprador)));
    }

    // Resumo mostrado ao comprador no fim da compra
    public String gerarResumo(Compra compra) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(BRASIL, "COMPRA Nº %d%n", compra.getIdCompra()));
        sb.append(String.format(BRASIL, "Comprador: %s%n", compra.getNomeComprador()));
        sb.append(String.format(BRASIL, "Data: %s%n%n",
                compra.getHorarioCompra().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        for (ItemCompra item : compra.getItensComprados()) {
            sb.append(String.format(BRASIL, "%-22s %3d x R$ %9.2f = R$ %10.2f%n", item.getNomeProduto(),
                    item.getQuantidadeProduto(), item.getPrecoUnitario(), item.calcularSubtotal()));
        }
        sb.append(String.format(BRASIL, "%nSubtotal:    R$ %.2f%n", compra.getSubtotal()));
        sb.append(String.format(BRASIL, "Desconto:    R$ %.2f%n", compra.getDesconto()));
        sb.append(String.format(BRASIL, "Valor final: R$ %.2f", compra.getValorFinal()));
        return sb.toString();
    }

    // ===================== Internos =====================

    // O nome vira pasta de histórico e linha de arquivo: tira espaços repetidos/quebras de linha
    private String validarComprador(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do comprador.");
        }
        return nome.trim().replaceAll("\\s+", " ");
    }

    private List<ItemCompra> converterItens(List<ItemCarrinho> itensCarrinho) {
        List<ItemCompra> itensCompra = new ArrayList<>();

        for (ItemCarrinho item : itensCarrinho) {
            itensCompra.add(new ItemCompra(item.getProduto().getId(), item.getProduto().getNome(),
                    item.getQuantidade(), item.getProduto().getPreco()));
        }

        return itensCompra;
    }

    private void desfazerBaixaDeEstoque(List<ItemCarrinho> itens, ErroPersistenciaException causa)
            throws ErroPersistenciaException {
        try {
            servicoProdutos.reporEstoque(itens);
        } catch (ErroPersistenciaException falhaAoRepor) {
            ErroPersistenciaException grave = new ErroPersistenciaException(
                    "A compra não foi registrada e o estoque não pôde ser devolvido. "
                            + "Confira o arquivo de produtos antes de continuar.", causa);
            grave.addSuppressed(falhaAoRepor);
            throw grave;
        }
    }
}
