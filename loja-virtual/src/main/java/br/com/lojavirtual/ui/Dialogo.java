package br.com.lojavirtual.ui;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

// Base das janelas de diálogo personalizadas: janela sem moldura do sistema, com cartão branco
// arredondado e sombra, bloqueando a tela de trás (modal), centralizada sobre ela e fechada com Esc.
public final class Dialogo {
    private Dialogo() {
    }

    // "origem" é qualquer componente da tela aberta (pode ser null); "conteudo" deve ter a classe "dialogo"
    public static Stage criar(Node origem, Region conteudo) {
        Window dono = (origem != null && origem.getScene() != null) ? origem.getScene().getWindow() : null;
        return criar(dono, conteudo);
    }

    // Variante usada pelo Navegador, que conhece a janela principal em vez de um componente
    public static Stage criar(Window dono, Region conteudo) {
        Stage palco = new Stage(StageStyle.TRANSPARENT);
        if (dono != null) {
            palco.initOwner(dono);
        }
        palco.initModality(Modality.APPLICATION_MODAL);

        StackPane moldura = new StackPane(conteudo);
        moldura.setPadding(new Insets(30)); // espaço para a sombra aparecer
        moldura.setStyle("-fx-background-color: transparent;");

        Scene cena = new Scene(moldura);
        cena.setFill(Color.TRANSPARENT);
        cena.getStylesheets().add(Navegador.CSS);
        cena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                palco.close();
            }
        });
        palco.setScene(cena);

        Rectangle2D tela = Screen.getPrimary().getVisualBounds();
        palco.setMaxHeight(tela.getHeight() * 0.96);
        palco.setOnShown(e -> centralizar(palco, dono));
        return palco;
    }

    private static void centralizar(Stage palco, Window dono) {
        if (dono != null) {
            palco.setX(dono.getX() + (dono.getWidth() - palco.getWidth()) / 2);
            palco.setY(dono.getY() + (dono.getHeight() - palco.getHeight()) / 2);
        } else {
            palco.centerOnScreen();
        }
    }
}
