package br.com.lojavirtual.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

// Mensagem rápida que aparece na parte de baixo da tela e some sozinha (feedback sem interromper o uso).
public final class Toast {
    private Toast() {
    }

    public static void sucesso(StackPane area, String texto) {
        mostrar(area, texto, false);
    }

    public static void erro(StackPane area, String texto) {
        mostrar(area, texto, true);
    }

    private static void mostrar(StackPane area, String texto, boolean erro) {
        Label rotulo = new Label(texto);
        rotulo.setWrapText(true);
        rotulo.setMaxWidth(420);
        rotulo.setMinHeight(Region.USE_PREF_SIZE);

        HBox caixa = new HBox(10, Icones.criar(erro ? "alerta" : "check", "icone-branco"), rotulo);
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.getStyleClass().addAll("toast", erro ? "toast-erro" : "toast-sucesso");
        caixa.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        caixa.setMouseTransparent(true);
        StackPane.setAlignment(caixa, Pos.BOTTOM_CENTER);
        StackPane.setMargin(caixa, new Insets(0, 0, 28, 0));

        // só um aviso por vez: o novo substitui o anterior
        Object anterior = area.getProperties().get("toast-atual");
        if (anterior instanceof Node no) {
            area.getChildren().remove(no);
        }
        area.getProperties().put("toast-atual", caixa);

        caixa.setOpacity(0);
        area.getChildren().add(caixa);

        FadeTransition entrada = new FadeTransition(Duration.millis(160), caixa);
        entrada.setToValue(1);
        PauseTransition pausa = new PauseTransition(Duration.millis(erro ? 3400 : 2100));
        FadeTransition saida = new FadeTransition(Duration.millis(300), caixa);
        saida.setToValue(0);
        saida.setOnFinished(e -> area.getChildren().remove(caixa));
        new SequentialTransition(entrada, pausa, saida).play();
    }
}
