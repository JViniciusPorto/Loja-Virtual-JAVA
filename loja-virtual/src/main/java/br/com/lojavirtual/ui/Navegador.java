package br.com.lojavirtual.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

// Troca as telas da janela principal e cria janelas de diálogo.
// Os controllers são criados aqui, recebendo o Contexto pelo construtor.
public class Navegador {
    public static final String CSS = Navegador.class.getResource("/css/estilo.css").toExternalForm();

    private static final double LARGURA_INICIAL = 1240;
    private static final double ALTURA_INICIAL = 780;
    private static final double LARGURA_MINIMA = 860;
    private static final double ALTURA_MINIMA = 620;

    static {
        carregarFontes();
    }

    private final Stage stage;
    private final Contexto contexto;

    public Navegador(Stage stage, Contexto contexto) {
        this.stage = stage;
        this.contexto = contexto;
    }

    public void mostrarInicio() {
        mostrar("inicio.fxml", "Loja Virtual");
    }

    public void mostrarAdmin() {
        mostrar("admin.fxml", "Loja Virtual - Administrador");
    }

    public void mostrarComprador() {
        mostrar("comprador.fxml", "Loja Virtual - Comprador");
    }

    // Loader de um arquivo da pasta resources/fxml, já ligado à criação de controllers com Contexto
    public FXMLLoader novoLoader(String arquivo) {
        URL url = Navegador.class.getResource("/fxml/" + arquivo);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(this::criarController);
        return loader;
    }

    // Janela modal (bloqueia a principal até ser fechada), com o visual de diálogo da loja
    public Stage novoDialogo(Region raiz, String titulo) {
        Stage dialogo = Dialogo.criar(stage, raiz);
        dialogo.setTitle(titulo);
        return dialogo;
    }

    private Object criarController(Class<?> tipo) {
        try {
            return tipo.getConstructor(Contexto.class).newInstance(contexto);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível criar o controller " + tipo.getName(), e);
        }
    }

    private void mostrar(String arquivo, String titulo) {
        try {
            Parent raiz = novoLoader(arquivo).load();
            if (stage.getScene() == null) {
                // a janela nunca passa do tamanho útil da tela (senão a barra de título fica escondida)
                Rectangle2D tela = Screen.getPrimary().getVisualBounds();
                double largura = Math.min(LARGURA_INICIAL, tela.getWidth() * 0.92);
                double altura = Math.min(ALTURA_INICIAL, tela.getHeight() * 0.88);
                Scene cena = new Scene(raiz, largura, altura);
                cena.getStylesheets().add(CSS);
                stage.setScene(cena);
                stage.setX(tela.getMinX() + (tela.getWidth() - largura) / 2);
                stage.setY(tela.getMinY() + (tela.getHeight() - altura) / 2);
            } else {
                stage.getScene().setRoot(raiz);
            }
            Rectangle2D area = Screen.getPrimary().getVisualBounds();
            stage.setMinWidth(Math.min(LARGURA_MINIMA, area.getWidth() * 0.9));
            stage.setMinHeight(Math.min(ALTURA_MINIMA, area.getHeight() * 0.8));
            stage.setTitle(titulo);
            Animacoes.aparecer(raiz);
        } catch (IOException e) {
            Alertas.erro(null, "Erro ao abrir a tela", "Não foi possível abrir " + arquivo + ": " + e.getMessage());
        }
    }

    // Fonte Inter (opcional): se os arquivos .ttf estiverem em resources/fonts, são usados; se não,
    // o tema cai automaticamente para a Segoe UI / fonte do sistema.
    private static void carregarFontes() {
        String[] arquivos = {"Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf"};
        for (String arquivo : arquivos) {
            try (InputStream entrada = Navegador.class.getResourceAsStream("/fonts/" + arquivo)) {
                if (entrada != null) {
                    Font.loadFont(entrada, 14);
                }
            } catch (IOException e) {
                // sem a fonte opcional o tema continua funcionando com a fonte padrão
            }
        }
    }
}
