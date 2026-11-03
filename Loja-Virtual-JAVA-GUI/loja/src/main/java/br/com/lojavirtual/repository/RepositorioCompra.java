package br.com.lojavirtual.repository;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCompra;
import br.com.lojavirtual.model.ResumoCompra;
import br.com.lojavirtual.model.pagamento.CartaoCredito;
import br.com.lojavirtual.model.pagamento.CartaoDebito;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.model.pagamento.PagamentoPix;

public class RepositorioCompra {

    private static final Path CAMINHO_RELATORIO = Path.of("data", "relatorio", "compras.txt");

    private static final String FIM_COMPRA = "FIM_COMPRA";

    private static final Path CAMINHO_HISTORICO = Path.of("data", "historico");

    // Grava a compra no relatório geral e no histórico do comprador.
    // Se a segunda gravação falhar, a primeira é desfeita (o arquivo volta ao tamanho de antes),
    // para nunca ficar uma compra registrada só em um dos arquivos.
    public void salvarCompra(Compra compra) throws ErroPersistenciaException {
        String textoCompra = transformarTextoCompra(compra);
        String textoHistorico = transformarTextoHistorico(compra);
        Path caminhoHistorico = obterCaminhoHistorico(compra.getNomeComprador());

        long tamanhoRelatorio = anexar(CAMINHO_RELATORIO, textoCompra);

        try {
            anexar(caminhoHistorico, textoHistorico);
        } catch (ErroPersistenciaException e) {
            try {
                restaurarTamanho(CAMINHO_RELATORIO, tamanhoRelatorio);
            } catch (ErroPersistenciaException falhaAoDesfazer) {
                e.addSuppressed(falhaAoDesfazer);
            }
            throw e;
        }
    }

    // Acrescenta texto ao fim do arquivo e devolve o tamanho que ele tinha ANTES (para poder desfazer).
    // Se a escrita falhar no meio, o próprio arquivo é devolvido ao tamanho anterior.
    private long anexar(Path arquivo, String texto) throws ErroPersistenciaException {
        long tamanhoAntes = 0;
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            tamanhoAntes = Files.exists(arquivo) ? Files.size(arquivo) : 0;
            Files.writeString(arquivo, texto, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return tamanhoAntes;
        } catch (IOException e) {
            ErroPersistenciaException erro = new ErroPersistenciaException(
                    "Não foi possível gravar a compra no arquivo " + arquivo + ".", e);
            try {
                restaurarTamanho(arquivo, tamanhoAntes);
            } catch (ErroPersistenciaException falhaAoDesfazer) {
                erro.addSuppressed(falhaAoDesfazer);
            }
            throw erro;
        }
    }

    private void restaurarTamanho(Path arquivo, long tamanho) throws ErroPersistenciaException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
            canal.truncate(tamanho);
        } catch (IOException e) {
            throw new ErroPersistenciaException("Não foi possível desfazer a gravação em " + arquivo
                    + ". Confira o final do arquivo.", e);
        }
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
        if (nomeComprador == null || nomeComprador.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do comprador.");
        }

        // O nome vira nome de pasta: troca caracteres que não valem em pasta (/, \\, :, .)
        String nomePasta = nomeComprador.trim().replaceAll("[^\\p{L}\\p{N} _-]", "_");

        return CAMINHO_HISTORICO.resolve(nomePasta).resolve("compras.txt");
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

    public List<String> carregarRelatorio() throws ErroPersistenciaException {
        return lerLinhas(CAMINHO_RELATORIO);
    }

    public List<String> carregarHistorico(String nomeComprador) throws ErroPersistenciaException {
        return lerLinhas(obterCaminhoHistorico(nomeComprador));
    }

    // Arquivo que ainda não existe (nenhuma compra feita) = lista vazia
    private List<String> lerLinhas(Path caminho) throws ErroPersistenciaException {
        if (!Files.exists(caminho)) {
            return List.of();
        }
        try {
            return Files.readAllLines(caminho);
        } catch (IOException e) {
            throw new ErroPersistenciaException("Não foi possível ler o arquivo " + caminho + ".", e);
        }
    }

    public double calcularTotalGasto(List<String> historico) throws ErroPersistenciaException {
        double total = 0;

        for (String linha : historico) {
            if (linha.startsWith("VALOR_FINAL = ")) {
                try {
                    total += Double.parseDouble(linha.substring("VALOR_FINAL = ".length()).trim());
                } catch (NumberFormatException e) {
                    throw new ErroPersistenciaException("Histórico inválido: valor malformado em \"" + linha + "\".", e);
                }
            }
        }

        return Compra.arredondar(total);
    }

    // Histórico do comprador já organizado por compra (ID, data/hora, pagamento e valor final),
    // pronto para a tela. Blocos sem FIM_COMPRA (gravação interrompida) são ignorados.
    public List<ResumoCompra> carregarResumoHistorico(String nomeComprador) throws ErroPersistenciaException {
        List<ResumoCompra> resumos = new ArrayList<>();

        Integer id = null;
        LocalDateTime horario = null;
        String pagamento = null;
        Double valorFinal = null;
        int numeroLinha = 0;

        for (String linha : carregarHistorico(nomeComprador)) {
            numeroLinha++;
            try {
                if (linha.startsWith("ID = ")) {
                    id = Integer.valueOf(linha.substring("ID = ".length()).trim());
                    horario = null;
                    pagamento = null;
                    valorFinal = null;
                } else if (linha.startsWith("DATA_E_HORA = ")) {
                    horario = LocalDateTime.parse(linha.substring("DATA_E_HORA = ".length()).trim());
                } else if (linha.startsWith("PAGAMENTO = ")) {
                    pagamento = linha.substring("PAGAMENTO = ".length()).trim();
                } else if (linha.startsWith("VALOR_FINAL = ")) {
                    valorFinal = Double.valueOf(linha.substring("VALOR_FINAL = ".length()).trim());
                } else if (linha.trim().equals(FIM_COMPRA)) {
                    if (id != null && horario != null && pagamento != null && valorFinal != null) {
                        resumos.add(new ResumoCompra(id, horario, pagamento, valorFinal));
                    }
                    id = null;
                    horario = null;
                    pagamento = null;
                    valorFinal = null;
                }
            } catch (NumberFormatException | DateTimeParseException e) {
                throw new ErroPersistenciaException(
                        "Histórico inválido na linha " + numeroLinha + ": " + e.getMessage(), e);
            }
        }

        return resumos;
    }

    // Próximo ID = maior ID do relatório + 1 (nunca repete, mesmo depois de reiniciar o programa)
    public int gerarIdCompra(List<String> relatorio) throws ErroPersistenciaException {
        int maiorID = 0;
        for (String linha : relatorio) {
            if (linha.startsWith("ID = ")) {
                try {
                    maiorID = Math.max(maiorID, Integer.parseInt(linha.substring("ID = ".length()).trim()));
                } catch (NumberFormatException e) {
                    throw new ErroPersistenciaException("Relatório inválido: ID malformado em \"" + linha + "\".", e);
                }
            }
        }
        return maiorID + 1;
    }
}
