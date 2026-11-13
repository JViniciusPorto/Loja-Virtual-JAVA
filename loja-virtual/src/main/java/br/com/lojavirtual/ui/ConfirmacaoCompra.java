package br.com.lojavirtual.ui;

import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCompra;
import br.com.lojavirtual.model.pagamento.CartaoCredito;
import br.com.lojavirtual.model.pagamento.CartaoDebito;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.model.pagamento.PagamentoPix;
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

// Resumo da compra concluída: identificador, produtos, quantidades, valores e forma de pagamento.
// Só apresenta os dados da Compra devolvida pelo ServicoCompra; nada é calculado aqui.
public final class ConfirmacaoCompra {
    private ConfirmacaoCompra() {
    }

    public static void mostrar(Node origem, Compra compra) {
        StackPane circulo = new StackPane(Icones.criar("check", "icone-g"));
        circulo.getStyleClass().addAll("dialogo-icone", "dialogo-icone-sucesso");
        circulo.setAlignment(Pos.CENTER);

        Label titulo = new Label("Compra concluída!");
        titulo.getStyleClass().add("dialogo-titulo");
        Label subtitulo = new Label("Compra nº " + compra.getIdCompra() + "  •  "
                + Formatos.dataHora(compra.getHorarioCompra()));
        subtitulo.getStyleClass().add("texto-suave");
        HBox cabecalho = new HBox(16, circulo, new VBox(4, titulo, subtitulo));
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        VBox itens = new VBox(12);
        for (ItemCompra item : compra.getItensComprados()) {
            Label nome = new Label(item.getNomeProduto());
            nome.getStyleClass().add("item-nome");
            Label detalhe = new Label(item.getQuantidadeProduto() + " × " + Formatos.dinheiro(item.getPrecoUnitario()));
            detalhe.getStyleClass().add("texto-suave");
            VBox esquerda = new VBox(2, nome, detalhe);
            HBox.setHgrow(esquerda, Priority.ALWAYS);
            Label subtotal = new Label(Formatos.dinheiro(item.calcularSubtotal()));
            subtotal.getStyleClass().add("subtitulo");
            HBox linha = new HBox(12, esquerda, subtotal);
            linha.setAlignment(Pos.CENTER_LEFT);
            itens.getChildren().add(linha);
        }
        ScrollPane rolagem = new ScrollPane(itens);
        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagem.setPrefHeight(Math.min(230, compra.getItensComprados().size() * 52 + 8));
        rolagem.setMinHeight(60);

        VBox totais = new VBox(8,
                linhaValor("Subtotal", Formatos.dinheiro(compra.getSubtotal()), false),
                linhaValor("Desconto", Formatos.dinheiro(compra.getDesconto()), false),
                linhaValor("Valor final", Formatos.dinheiro(compra.getValorFinal()), true));

        Label pagamento = new Label(nomeDaForma(compra.getPagamento()));
        pagamento.getStyleClass().addAll("pilula", "pilula-laranja");
        HBox formaPagamento = new HBox(8, rotuloSuave("Forma de pagamento"), pagamento);
        formaPagamento.setAlignment(Pos.CENTER_LEFT);

        Button concluir = new Button("Concluir");
        concluir.getStyleClass().add("botao-primario");
        concluir.setDefaultButton(true);
        concluir.setMaxWidth(Double.MAX_VALUE);

        VBox raiz = new VBox(18, cabecalho, separador(), rolagem, separador(), totais, formaPagamento, concluir);
        raiz.getStyleClass().add("dialogo");
        raiz.setPrefWidth(460);
        raiz.setMinWidth(380);

        Stage palco = Dialogo.criar(origem, raiz);
        concluir.setOnAction(e -> palco.close());
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

    private static Label rotuloSuave(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("texto-suave");
        return rotulo;
    }

    private static Region separador() {
        Region linha = new Region();
        linha.getStyleClass().add("separador");
        return linha;
    }

    private static String nomeDaForma(FormaPagamento forma) {
        if (forma instanceof PagamentoPix) {
            return Formatos.formaPagamento("PIX");
        }
        if (forma instanceof CartaoDebito) {
            return Formatos.formaPagamento("CARTAO_DEBITO");
        }
        if (forma instanceof CartaoCredito) {
            return Formatos.formaPagamento("CARTAO_CREDITO");
        }
        return "Não informada";
    }
}
