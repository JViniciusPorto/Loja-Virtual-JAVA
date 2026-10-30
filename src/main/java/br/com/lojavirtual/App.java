package br.com.lojavirtual;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {

        Label mensagem = new Label("Loja Virtual");

        StackPane root = new StackPane(mensagem);

        Scene scene = new Scene(root, 800, 500);

        stage.setTitle("Loja Virtual");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}