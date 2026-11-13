package br.com.lojavirtual.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

// Animações curtas e discretas (nada piscante), todas só visuais.
public final class Animacoes {
    private Animacoes() {
    }

    // O componente "sobe" alguns pixels enquanto o mouse está sobre ele
    public static void elevarAoPassarMouse(Node no, double deslocamento) {
        no.setOnMouseEntered(e -> mover(no, -deslocamento));
        no.setOnMouseExited(e -> mover(no, 0));
    }

    private static void mover(Node no, double y) {
        Object anterior = no.getProperties().get("animacao-mover");
        if (anterior instanceof TranslateTransition t) {
            t.stop();
        }
        TranslateTransition t = new TranslateTransition(Duration.millis(140), no);
        t.setToY(y);
        no.getProperties().put("animacao-mover", t);
        t.play();
    }

    // Entrada suave de uma tela ou de um bloco de conteúdo
    public static void aparecer(Node no) {
        no.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), no);
        fade.setToValue(1);
        fade.play();
    }

    // "Pulinho" usado como feedback (ex.: contador do carrinho ao adicionar um produto)
    public static void pulsar(Node no) {
        ScaleTransition t = new ScaleTransition(Duration.millis(160), no);
        t.setFromX(1);
        t.setFromY(1);
        t.setToX(1.3);
        t.setToY(1.3);
        t.setAutoReverse(true);
        t.setCycleCount(2);
        t.play();
    }

    // Recorta um painel com cantos arredondados (para enfeites não vazarem para fora do banner)
    public static void recortarArredondado(Region regiao, double raio) {
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(raio * 2);
        recorte.setArcHeight(raio * 2);
        recorte.widthProperty().bind(regiao.widthProperty());
        recorte.heightProperty().bind(regiao.heightProperty());
        regiao.setClip(recorte);
    }

    // Rola suavemente um ScrollPane até que o componente alvo apareça no topo
    public static void rolarPara(ScrollPane scroll, Node alvo) {
        Node conteudo = scroll.getContent();
        if (conteudo == null || alvo.getScene() == null) {
            return;
        }
        double sobra = conteudo.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight();
        if (sobra <= 0) {
            return;
        }
        double y = alvo.localToScene(0, 0).getY() - conteudo.localToScene(0, 0).getY();
        double destino = Math.max(0, Math.min(1, (y - 12) / sobra));
        Timeline linha = new Timeline(new KeyFrame(Duration.millis(320),
                new KeyValue(scroll.vvalueProperty(), destino, Interpolator.EASE_BOTH)));
        linha.play();
    }
}
