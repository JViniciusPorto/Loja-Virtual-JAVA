package br.com.lojavirtual.ui;

import br.com.lojavirtual.ui.LeitorCompras.DetalheCompra;
import br.com.lojavirtual.ui.LeitorCompras.ItemDetalhe;
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

// Janela com os detalhes de uma compra feita: itens, quantidades, valores e forma de pagamento.
public final class DetalhesCompra {
    private DetalhesCompra() {
    }

    public static void mostrar(Node origem, DetalheCompra compra) {
        StackPane circulo = new StackPane(Icones.criar("recibo", "icone-g"));
        circulo.getStyleClass().addAll("dialogo-icone", "dialogo-icone-aviso");
        circulo.setAlignment(Pos.CENTER);

        Label titulo = new Label("Compra nº " + compra.idCompra());
        titulo.getStyleClass().add("dialogo-titulo");
        Label subtitulo = new Label(Formatos.dataHora(compra.horario()));
        subtitulo.getStyleClass().add("texto-suave");
        Label pagamento = new Label(Formatos.formaPagamento(compra.formaPagamento()));
        pagamento.getStyleClass().addAll("pilula", "pilula-laranja");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox cabecalho = new HBox(16, circulo, new VBox(4, titulo, subtitulo), espaco, pagamento);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        VBox itens = new VBox(12);
        for (ItemDetalhe item : compra.itens()) {
            Label nome = new Label(item.nome());
            nome.getStyleClass().add("item-nome");
            Label detalhe = new Label(item.quantidade() + " × " + Formatos.dinheiro(item.precoUnitario()));
            detalhe.getStyleClass().add("texto-suave");
            VBox esquerda = new VBox(2, nome, detalhe);
            HBox.setHgrow(esquerda, Priority.ALWAYS);
            Label subtotal = new Label(Formatos.dinheiro(item.subtotal()));
            subtotal.getStyleClass().add("subtitulo");
            HBox linha = new HBox(12, esquerda, subtotal);
            linha.setAlignment(Pos.CENTER_LEFT);
            itens.getChildren().add(linha);
        }
        ScrollPane rolagem = new ScrollPane(itens);
        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagem.setPrefHeight(Math.min(240, Math.max(1, compra.itens().size()) * 52 + 8));
        rolagem.setMinHeight(60);

        VBox totais = new VBox(8,
                linhaValor("Subtotal dos itens", Formatos.dinheiro(compra.subtotal()), false),
                linhaValor("Desconto", Formatos.dinheiro(compra.desconto()), false),
                linhaValor("Valor final", Formatos.dinheiro(compra.valorFinal()), true));

        Label nota = new Label("O histórico guarda os itens e o valor final; o subtotal e o desconto "
                + "são calculados a partir deles.");
        nota.getStyleClass().add("texto-pequeno");
        nota.setWrapText(true);
        nota.setMinHeight(Region.USE_PREF_SIZE);

        Button fechar = new Button("Fechar");
        fechar.getStyleClass().add("botao-primario");
        fechar.setDefaultButton(true);
        fechar.setMaxWidth(Double.MAX_VALUE);

        VBox raiz = new VBox(18, cabecalho, separador(), rolagem, separador(), totais, nota, fechar);
        raiz.getStyleClass().add("dialogo");
        raiz.setPrefWidth(480);
        raiz.setMinWidth(380);

        Stage palco = Dialogo.criar(origem, raiz);
        fechar.setOnAction(e -> palco.close());
        palco.showAndWait();
    }

    private static HBox linhaValor(String rotulo, String valor, boolean destaque) {
        Label esquerda = new Label(rotulo);
        Label direita = new Label(valor);
        if (destaque) {
            esquerda.getStyleClass().add("subtitulo");
            direita.getStyleClass().add("resumo-total");
        } else {
            esquerda.getStyleClass().add("texto-suave");
        }
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linha = new HBox(esquerda, espaco, direita);
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    private static Region separador() {
        Region linha = new Region();
        linha.getStyleClass().add("separador");
        return linha;
    }
}
