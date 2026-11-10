package br.com.lojavirtual.ui;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

// Troca as telas da janela principal e cria janelas de diálogo.
// Os controllers são criados aqui, recebendo o Contexto pelo construtor.
public class Navegador {
    public static final String CSS = Navegador.class.getResource("/css/estilo.css").toExternalForm();

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

    // Janela modal (bloqueia a principal até ser fechada)
    public Stage novoDialogo(Parent raiz, String titulo) {
        Stage dialogo = new Stage();
        dialogo.initOwner(stage);
        dialogo.initModality(Modality.APPLICATION_MODAL);
        dialogo.setTitle(titulo);
        Scene cena = new Scene(raiz);
        cena.getStylesheets().add(CSS);
        dialogo.setScene(cena);
        dialogo.setResizable(false);
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
                Scene cena = new Scene(raiz, 1000, 680);
                cena.getStylesheets().add(CSS);
                stage.setScene(cena);
            } else {
                stage.getScene().setRoot(raiz);
            }
            stage.setTitle(titulo);
        } catch (IOException e) {
            Alertas.erro(null, "Erro ao abrir a tela", "Não foi possível abrir " + arquivo + ": " + e.getMessage());
        }
    }
}
