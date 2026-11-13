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

public class ServicoCompra {
    private final ServicoProdutos servicoProdutos;
    private final RepositorioCompra repositorio;

    public ServicoCompra(ServicoProdutos servicoProdutos, RepositorioCompra repositorio) {
        this.servicoProdutos = servicoProdutos;
        this.repositorio = repositorio;
    }

    public Compra finalizarCompra(Carrinho carrinho, String nomeComprador, FormaPagamento pagamento)
            throws EstoqueInsuficienteException, ProdutoNaoEncontradoException,
                   QuantidadeInvalidaException, PagamentoRecusadoException,
                   ErroPersistenciaException {

        if (carrinho == null || carrinho.estaVazio()) {
            throw new CarrinhoVazioException();
        }
        String comprador = validarComprador(nomeComprador);
        
        if (pagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento não informada.");
        }

        carrinho.verificarEstoque();

        List<ItemCarrinho> itensCarrinho = carrinho.getItens();
        int idCompra = repositorio.gerarIdCompra(repositorio.carregarRelatorio());

        Compra compra = new Compra(idCompra, LocalDateTime.now(), comprador, converterItens(itensCarrinho), pagamento);
        compra.calcularSubtotal();
        compra.calcularValorFinal();

        if (!pagamento.processarPagamento()) {
            throw new PagamentoRecusadoException();
        }

        servicoProdutos.baixarEstoque(itensCarrinho);

        try {
            repositorio.salvarCompra(compra);
        } catch (ErroPersistenciaException e) {
            try {
                servicoProdutos.reporEstoque(itensCarrinho);
            } catch (ErroPersistenciaException falhaAoRepor) {
                ErroPersistenciaException grave = new ErroPersistenciaException(
                    "A compra não foi registrada e o estoque não pôde ser devolvido. "
                    + "Confira o arquivo de produtos antes de continuar.",
                    e
                );

                grave.addSuppressed(falhaAoRepor);
                throw grave;
            }

            throw e;
        }

        carrinho.limpar();
        return compra;
    }

    private static final Locale BRASIL = Locale.of("pt", "BR");

    public List<ResumoCompra> consultarHistorico(String nomeComprador) throws ErroPersistenciaException {
        return repositorio.carregarResumoHistorico(validarComprador(nomeComprador));
    }

    public double calcularTotalGasto(String nomeComprador) throws ErroPersistenciaException {
        return repositorio.calcularTotalGasto(repositorio.carregarHistorico(validarComprador(nomeComprador)));
    }

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

}
