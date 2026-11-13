package br.com.lojavirtual.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import javafx.scene.image.Image;

// Fotos dos produtos. O cadastro (Produto) não guarda imagem, então a foto fica como um arquivo
// na pasta data/imagens, com o ID do produto no nome (ex.: data/imagens/1001.png).
// Só a interface usa isso: model, service e repository continuam como estavam.
public final class Fotos {
    public static final String[] EXTENSOES = {"png", "jpg", "jpeg", "gif", "bmp"};
    public static final long TAMANHO_MAXIMO = 8L * 1024 * 1024;
    private static final Path PASTA = Path.of("data", "imagens");

    private Fotos() {
    }

    // Arquivo da foto do produto, ou null se ele não tem foto
    public static Path localizar(int idProduto) {
        for (String extensao : EXTENSOES) {
            Path caminho = PASTA.resolve(idProduto + "." + extensao);
            if (Files.isRegularFile(caminho)) {
                return caminho;
            }
        }
        return null;
    }

    // Devolve a mensagem do problema, ou null se o arquivo pode ser usado como foto
    public static String validar(Path origem) {
        String extensao = extensao(origem);
        boolean aceita = false;
        for (String permitida : EXTENSOES) {
            aceita |= permitida.equals(extensao);
        }
        if (!aceita) {
            return "Formato não suportado. Use PNG, JPG, GIF ou BMP.";
        }
        try {
            if (Files.size(origem) > TAMANHO_MAXIMO) {
                return "A imagem é muito grande. O limite é de 8 MB.";
            }
        } catch (IOException e) {
            return "Não foi possível ler o arquivo: " + e.getMessage();
        }
        Image teste = new Image(origem.toUri().toString());
        if (teste.isError() || teste.getWidth() <= 0) {
            return "Não foi possível abrir esse arquivo como imagem.";
        }
        return null;
    }

    // Copia a imagem para data/imagens/<id>.<extensão> e apaga fotos antigas do mesmo produto
    public static void salvar(int idProduto, Path origem) throws IOException {
        String extensao = extensao(origem);
        Files.createDirectories(PASTA);
        Path destino = PASTA.resolve(idProduto + "." + extensao);
        if (!origem.toAbsolutePath().normalize().equals(destino.toAbsolutePath().normalize())) {
            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        for (String outra : EXTENSOES) {
            if (!outra.equals(extensao)) {
                Files.deleteIfExists(PASTA.resolve(idProduto + "." + outra));
            }
        }
    }

    public static void remover(int idProduto) throws IOException {
        for (String extensao : EXTENSOES) {
            Files.deleteIfExists(PASTA.resolve(idProduto + "." + extensao));
        }
    }

    private static String extensao(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1);
    }
}
