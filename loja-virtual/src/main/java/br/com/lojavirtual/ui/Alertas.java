package br.com.lojavirtual.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Caixas de mensagem padronizadas, com o visual da loja. O primeiro parâmetro é qualquer componente
// da tela que está aberta: serve para a mensagem aparecer por cima dela (pode ser null).
public final class Alertas {
    private static final double LARGURA_TEXTO = 340;

    private Alertas() {
    }

    public static void erro(Node origem, String titulo, String mensagem) {
        aviso(origem, "alerta", "dialogo-icone-erro", titulo, mensagem);
    }

    public static void info(Node origem, String titulo, String mensagem) {
        aviso(origem, "info", "dialogo-icone-info", titulo, mensagem);
    }

    public static boolean confirmar(Node origem, String titulo, String mensagem) {
        return confirmar(origem, titulo, mensagem, "Sim", false);
    }

    // "perigo" deixa o botão de confirmação vermelho (exclusões e ações sem volta)
    public static boolean confirmar(Node origem, String titulo, String mensagem, String textoConfirmar,
            boolean perigo) {
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("botao-secundario");
        cancelar.setCancelButton(true);

        Button confirmar = new Button(textoConfirmar);
        confirmar.getStyleClass().add(perigo ? "botao-perigo" : "botao-primario");
        confirmar.setDefaultButton(!perigo);

        HBox acoes = new HBox(10, cancelar, confirmar);
        acoes.setAlignment(Pos.CENTER_RIGHT);

        VBox raiz = new VBox(22,
                cabecalho(perigo ? "alerta" : "info", perigo ? "dialogo-icone-erro" : "dialogo-icone-aviso",
                        titulo, mensagem),
                acoes);
        raiz.getStyleClass().add("dialogo");

        Stage palco = Dialogo.criar(origem, raiz);
        boolean[] resposta = {false};
        cancelar.setOnAction(e -> palco.close());
        confirmar.setOnAction(e -> {
            resposta[0] = true;
            palco.close();
        });
        palco.showAndWait();
        return resposta[0];
    }

    // Texto longo com fonte de largura fixa (mantido para quem precisar mostrar um resumo em texto puro)
    public static void texto(Node origem, String titulo, String conteudo) {
        Label corpo = new Label(conteudo);
        corpo.getStyleClass().add("dialogo-texto-mono");
        ScrollPane rolagem = new ScrollPane(corpo);
        rolagem.setFitToWidth(true);
        rolagem.setPrefHeight(Math.min(360, conteudo.split("\n").length * 20 + 24));
        rolagem.setPrefWidth(480);

        Button ok = new Button("Fechar");
        ok.getStyleClass().add("botao-primario");
        ok.setDefaultButton(true);
        HBox acoes = new HBox(ok);
        acoes.setAlignment(Pos.CENTER_RIGHT);

        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("dialogo-titulo");
        VBox raiz = new VBox(16, rotulo, rolagem, acoes);
        raiz.getStyleClass().add("dialogo");

        Stage palco = Dialogo.criar(origem, raiz);
        ok.setOnAction(e -> palco.close());
        palco.showAndWait();
    }

    private static void aviso(Node origem, String icone, String classeIcone, String titulo, String mensagem) {
        Button ok = new Button("Entendi");
        ok.getStyleClass().add("botao-primario");
        ok.setDefaultButton(true);
        HBox acoes = new HBox(ok);
        acoes.setAlignment(Pos.CENTER_RIGHT);

        VBox raiz = new VBox(22, cabecalho(icone, classeIcone, titulo, mensagem), acoes);
        raiz.getStyleClass().add("dialogo");

        Stage palco = Dialogo.criar(origem, raiz);
        ok.setOnAction(e -> palco.close());
        palco.showAndWait();
    }

    // Círculo colorido com ícone + título + mensagem
    private static HBox cabecalho(String icone, String classeIcone, String titulo, String mensagem) {
        StackPane circulo = new StackPane(Icones.criar(icone, "icone-g"));
        circulo.getStyleClass().addAll("dialogo-icone", classeIcone);
        circulo.setAlignment(Pos.CENTER);

        VBox textos = new VBox(6, rotulo(titulo, "dialogo-titulo"));
        if (mensagem != null && !mensagem.isBlank()) {
            textos.getChildren().add(rotulo(mensagem, "dialogo-texto"));
        }
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox linha = new HBox(16, circulo, textos);
        linha.setAlignment(Pos.TOP_LEFT);
        return linha;
    }

    private static Label rotulo(String texto, String classe) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add(classe);
        rotulo.setWrapText(true);
        rotulo.setPrefWidth(LARGURA_TEXTO);
        rotulo.setMaxWidth(LARGURA_TEXTO);
        rotulo.setMinHeight(Region.USE_PREF_SIZE);
        return rotulo;
    }
}
