package br.com.lojavirtual.ui;

import java.nio.file.Path;

import br.com.lojavirtual.model.Produto;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// Imagem do produto: a foto de data/imagens (ver Fotos) ou, se não houver, um espaço reservado
// colorido (nunca uma imagem quebrada). A imagem mantém a proporção original e fica centralizada
// numa área de altura fixa.
public final class Miniaturas {
    private Miniaturas() {
    }

    public static StackPane criar(Produto produto, double altura) {
        StackPane area = base(altura);
        Path arquivo = Fotos.localizar(produto.getId());
        if (arquivo != null) {
            mostrarImagem(area, arquivo, altura, () -> espacoReservado(area, produto));
        } else {
            espacoReservado(area, produto);
        }
        return area;
    }

    // Prévia de um arquivo qualquer (usada no formulário antes de salvar); null mostra o quadro vazio
    public static StackPane criarPrevia(Path arquivo, double altura) {
        StackPane area = base(altura);
        if (arquivo != null) {
            mostrarImagem(area, arquivo, altura, () -> quadroVazio(area, altura));
        } else {
            quadroVazio(area, altura);
        }
        return area;
    }

    private static StackPane base(double altura) {
        StackPane area = new StackPane();
        area.getStyleClass().add("miniatura");
        area.setMinHeight(altura);
        area.setPrefHeight(altura);
        area.setMaxHeight(altura);
        area.setMaxWidth(Double.MAX_VALUE);
        return area;
    }

    private static void mostrarImagem(StackPane area, Path arquivo, double altura, Runnable aoFalhar) {
        double lado = Math.max(120, altura * 3);
        Image imagem = new Image(arquivo.toUri().toString(), lado, lado, true, true, true);
        ImageView vista = new ImageView(imagem);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        vista.setFitHeight(altura - 8);
        vista.fitWidthProperty().bind(area.widthProperty().subtract(8));
        imagem.errorProperty().addListener((obs, antigo, erro) -> {
            if (erro) {
                aoFalhar.run();
            }
        });
        area.getChildren().setAll(vista);
    }

    private static void espacoReservado(StackPane area, Produto produto) {
        double matiz = (produto.getId() * 47L) % 360;
        Color claro = Color.hsb(matiz, 0.10, 1.00);
        Color medio = Color.hsb((matiz + 24) % 360, 0.24, 0.97);
        Color icone = Color.hsb(matiz, 0.45, 0.78);
        area.setStyle("-fx-background-color: linear-gradient(to bottom right, " + web(claro) + ", " + web(medio) + ");");

        Region imagem = Icones.criar("imagem", tamanhoDoIcone(area.getMaxHeight()));
        imagem.setStyle("-fx-background-color: " + web(icone) + ";");
        area.getChildren().setAll(imagem);
    }

    private static void quadroVazio(StackPane area, double altura) {
        area.getStyleClass().add("miniatura-vazia");
        Label dica = new Label("Clique para escolher uma foto");
        dica.getStyleClass().add("texto-pequeno");
        VBox conteudo = new VBox(8, Icones.criar("imagem", tamanhoDoIcone(altura), "icone-medio"), dica);
        conteudo.setAlignment(Pos.CENTER);
        area.getChildren().setAll(conteudo);
    }

    private static String tamanhoDoIcone(double altura) {
        return altura > 100 ? "icone-xg" : altura > 48 ? "icone-g" : "icone-p";
    }

    private static String web(Color cor) {
        return String.format("#%02X%02X%02X", Math.round(cor.getRed() * 255), Math.round(cor.getGreen() * 255),
                Math.round(cor.getBlue() * 255));
    }
}
