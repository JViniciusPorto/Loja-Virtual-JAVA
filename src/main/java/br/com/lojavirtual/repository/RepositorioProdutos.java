package br.com.lojavirtual.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.com.lojavirtual.model.Produto;
import br.com.lojavirtual.exception.ErroPersistenciaException;

// Só lê e grava o arquivo de produtos. Nenhuma regra de negócio mora aqui
// (as validações ficam no ServicoProdutos).
//
// Formato de cada linha (separado por TAB):  ID <TAB> NOME <TAB> PRECO <TAB> ESTOQUE
// Exemplo:                                   7	Mouse Gamer	89.90	15
public class RepositorioProdutos {
    private static final String SEPARADOR = "\t";
    private final Path arquivo;

    public RepositorioProdutos() {
        this(Path.of("dados", "produtos.txt"));
    }

    // Recebe o caminho para poder usar outro arquivo em testes
    public RepositorioProdutos(Path arquivo) {
        this.arquivo = arquivo;
    }

    // Se o arquivo ainda não existe (primeira execução), devolve lista vazia.
    // Se existe mas tem uma linha inválida ou id repetido, lança ErroPersistenciaException
    // dizendo qual linha é, em vez de ignorar a linha e perder dados sem avisar.
    public List<Produto> carregar() throws ErroPersistenciaException {
        if (!Files.exists(arquivo)) {
            return new ArrayList<>();
        }

        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ErroPersistenciaException("Não foi possível ler o arquivo de produtos (" + arquivo + ").", e);
        }

        List<Produto> produtos = new ArrayList<>();
        Set<Integer> idsLidos = new HashSet<>();

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (linha.isBlank()) {
                continue;
            }

            Produto produto = converterLinha(linha, i + 1);
            if (!idsLidos.add(produto.getId())) {
                throw new ErroPersistenciaException(
                        "Arquivo de produtos inválido: id " + produto.getId() + " repetido (linha " + (i + 1) + ").");
            }
            produtos.add(produto);
        }
        return produtos;
    }

    // Grava TODOS os produtos, substituindo o conteúdo anterior.
    // Escreve primeiro num arquivo temporário e só depois troca pelo definitivo: se der erro
    // no meio da escrita, o produtos.txt antigo continua intacto (nunca fica pela metade).
    public void salvar(Collection<Produto> produtos) throws ErroPersistenciaException {
        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }

            List<String> linhas = new ArrayList<>();
            for (Produto produto : produtos) {
                linhas.add(converterProduto(produto));
            }
            Files.write(temporario, linhas, StandardCharsets.UTF_8);
            substituirArquivo(temporario);
        } catch (IOException e) {
            apagarTemporario(temporario);
            throw new ErroPersistenciaException("Não foi possível salvar o arquivo de produtos (" + arquivo + ").", e);
        }
    }

    private void substituirArquivo(Path temporario) throws IOException {
        try {
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // alguns sistemas de arquivos não suportam troca atômica; usa a troca comum
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void apagarTemporario(Path temporario) {
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException e) {
            // limpeza "melhor esforço": o erro que importa para o usuário é o da gravação, não este
        }
    }

    private Produto converterLinha(String linha, int numeroLinha) throws ErroPersistenciaException {
        String[] partes = linha.split(SEPARADOR, -1);
        if (partes.length != 4) {
            throw new ErroPersistenciaException("Arquivo de produtos inválido: a linha " + numeroLinha
                    + " deveria ter 4 campos separados por TAB, mas tem " + partes.length + ".");
        }

        try {
            int id = Integer.parseInt(partes[0].trim());
            String nome = partes[1].trim();
            double preco = Double.parseDouble(partes[2].trim());
            int estoque = Integer.parseInt(partes[3].trim());

            if (nome.isEmpty()) {
                throw new ErroPersistenciaException(
                        "Arquivo de produtos inválido: nome vazio na linha " + numeroLinha + ".");
            }
            return new Produto(id, nome, preco, estoque);
        } catch (NumberFormatException e) {
            throw new ErroPersistenciaException(
                    "Arquivo de produtos inválido: número malformado na linha " + numeroLinha + ".", e);
        }
    }

    // Locale.US garante o ponto decimal ("89.90") em qualquer computador, mesmo com Windows em português
    private String converterProduto(Produto produto) {
        return produto.getId() + SEPARADOR
                + produto.getNome() + SEPARADOR
                + String.format(Locale.US, "%.2f", produto.getPreco()) + SEPARADOR
                + produto.getEstoque();
    }
}
