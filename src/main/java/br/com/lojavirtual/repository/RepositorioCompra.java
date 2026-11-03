package br.com.lojavirtual.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCompra;
import br.com.lojavirtual.model.pagamento.CartaoCredito;
import br.com.lojavirtual.model.pagamento.CartaoDebito;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.model.pagamento.PagamentoPix;

public class RepositorioCompra {

    private static final Path CAMINHO_RELATORIO = Path.of("persistencia", "relatorio", "compras.txt");

    private static final Path CAMINHO_HISTORICO = Path.of("persistencia", "historico");

    public void salvarCompra(Compra compra) throws IOException {
        Files.createDirectories(CAMINHO_RELATORIO.getParent());

        String textoCompra = transformarTextoCompra(compra);

        Files.writeString(CAMINHO_RELATORIO,textoCompra,StandardOpenOption.CREATE,StandardOpenOption.APPEND);

        Path caminhoHistorico = obterCaminhoHistorico(compra.getNomeComprador());

        Files.createDirectories(caminhoHistorico.getParent());

        String textoHistorico = transformarTextoHistorico(compra);

        Files.writeString(caminhoHistorico,textoHistorico,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }

    private String transformarTextoCompra(Compra compra) {
        StringBuilder texto = new StringBuilder();

        texto.append("ID = ").append(compra.getIdCompra()).append("\n");

        texto.append("DATA_E_HORARIO = ").append(compra.getHorarioCompra()).append("\n");

        texto.append("COMPRADOR = ").append(compra.getNomeComprador()).append("\n");

        texto.append("PAGAMENTO = ").append(obterTipoPagamento(compra.getPagamento())).append("\n");

        texto.append("SUBTOTAL = ").append(compra.getSubtotal()).append("\n");

        texto.append("DESCONTO = ").append(compra.getDesconto()).append("\n");

        texto.append("VALOR_FINAL = ").append(compra.getValorFinal()).append("\n");

        for (ItemCompra item : compra.getItensComprados()) {
            texto.append("ITEM = ").append(item.getIdProduto()).append(" | ").append(item.getNomeProduto()).append(" | ").append(item.getQuantidadeProduto()).append(" | ").append(item.getPrecoUnitario()).append("\n");
        }

        texto.append("FIM_COMPRA\n\n");

        return texto.toString();
    }

    private String transformarTextoHistorico(Compra compra) {
        StringBuilder texto = new StringBuilder();

        texto.append("ID = ").append(compra.getIdCompra()).append("\n");

        texto.append("DATA_E_HORA = ").append(compra.getHorarioCompra()).append("\n");

        texto.append("PAGAMENTO = ").append(obterTipoPagamento(compra.getPagamento())).append("\n");

        texto.append("VALOR_FINAL = ").append(compra.getValorFinal()).append("\n");

        for (ItemCompra item : compra.getItensComprados()) {
            texto.append("ITEM: ").append(item.getIdProduto()).append(" NOME: ").append(item.getNomeProduto()).append(" QTD.: ").append(item.getQuantidadeProduto()).append(" PRECO: ").append(item.getPrecoUnitario()).append("\n");
        }

        texto.append("FIM_COMPRA\n\n");

        return texto.toString();
    }

    private Path obterCaminhoHistorico(String nomeComprador) {
        return CAMINHO_HISTORICO.resolve(nomeComprador).resolve("compras.txt");
    }

    private String obterTipoPagamento(FormaPagamento pagamento) {

        if (pagamento instanceof PagamentoPix) {
            return "PIX";
        }

        if (pagamento instanceof CartaoCredito) {
            return "CARTAO_CREDITO";
        }

        if (pagamento instanceof CartaoDebito) {
            return "CARTAO_DEBITO";
        }

        return "DESCONHECIDO";
    }

    public List<String> carregarRelatorio() throws IOException{
        if (!Files.exists(CAMINHO_RELATORIO)) {
            return List.of();
        }
        List<String> textoCompleto = Files.readAllLines(CAMINHO_RELATORIO);
        return textoCompleto;
    }

    public List<String> carregarHistorico(String nomeComprador) throws IOException{
        Path caminho = obterCaminhoHistorico(nomeComprador);
        if(!Files.exists(caminho)){
            return List.of();
        }
        List<String> textoCompleto = Files.readAllLines(caminho);
        return textoCompleto;
    }

    public double calcularTotalGasto(List<String> historico) {
        double total = 0;

        for (String linha : historico) {
            if (linha.startsWith("VALOR_FINAL = ")) {
                double valor = Double.parseDouble(linha.substring("VALOR_FINAL = ".length()));

                total += valor;
            }
        }

        return total;
    }

    public int gerarIdCompra(List<String> relatorio){
        int maiorID = 0;
        int id;
        for (String linha : relatorio) {
            if(linha.startsWith("ID = ")){
                id = Integer.parseInt(linha.substring("ID = ".length()));
                maiorID = Math.max(maiorID, id);
            }
        }
        return maiorID+1;
    }
}