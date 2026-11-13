package br.com.lojavirtual.ui;

import java.util.function.BiConsumer;

import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.ItemCarrinho;
import br.com.lojavirtual.model.Produto;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Janela de detalhes do produto: imagem grande, nome, preço, estoque e escolha da quantidade.
// O produto não tem descrição no cadastro, então nenhuma descrição é inventada aqui.
// A regra de estoque continua sendo do Carrinho: o botão "+" só evita cliques inúteis.
public final class DetalheProduto {
    private DetalheProduto() {
    }

    // aposAdicionar recebe o produto e a quantidade quando o item entrou no carrinho
    public static void mostrar(Node origem, Contexto contexto, Produto produto,
            BiConsumer<Produto, Integer> aposAdicionar) {
        int[] quantidade = {1};

        StackPane imagem = Miniaturas.criar(produto, 280);
        imagem.setMinWidth(280);
        imagem.setPrefWidth(280);
        imagem.setMaxWidth(280);

        Label codigo = new Label("Código " + produto.getId());
        codigo.getStyleClass().add("pilula");
        Label nome = new Label(produto.getNome());
        nome.getStyleClass().add("titulo-medio");
        nome.setWrapText(true);
        nome.setPrefWidth(300);
        nome.setMinHeight(Region.USE_PREF_SIZE);
        Label preco = new Label(Formatos.dinheiro(produto.getPreco()));
        preco.getStyleClass().add("titulo-pagina");

        Label estoque = new Label();
        estoque.getStyleClass().add("pilula");
        if (produto.getEstoque() <= 0) {
            estoque.setText("Esgotado");
            estoque.getStyleClass().add("pilula-alerta");
        } else if (produto.getEstoque() <= Visual.ESTOQUE_BAIXO) {
            estoque.setText("Últimas " + produto.getEstoque() + " unidades");
            estoque.getStyleClass().add("pilula-laranja");
        } else {
            estoque.setText(produto.getEstoque() + " unidades em estoque");
            estoque.getStyleClass().add("pilula-ok");
        }

        int noCarrinho = contexto.getCarrinho().getItens().stream()
                .filter(i -> i.getProduto().getId() == produto.getId())
                .mapToInt(ItemCarrinho::getQuantidade).sum();
        Label jaNoCarrinho = new Label(noCarrinho > 0 ? noCarrinho + " deste produto já está no seu carrinho." : "");
        jaNoCarrinho.getStyleClass().add("texto-suave");
        jaNoCarrinho.setManaged(noCarrinho > 0);
        jaNoCarrinho.setVisible(noCarrinho > 0);

        // seletor de quantidade
        Button menos = botaoStepper("menos");
        Button mais = botaoStepper("mais");
        Label valorQuantidade = new Label("1");
        valorQuantidade.getStyleClass().add("stepper-qtd");
        HBox stepper = new HBox(4, menos, valorQuantidade, mais);
        stepper.getStyleClass().add("stepper");
        stepper.setAlignment(Pos.CENTER);
        stepper.setMaxWidth(Region.USE_PREF_SIZE);

        Label erro = new Label();
        erro.getStyleClass().add("msg-erro");
        erro.setWrapText(true);
        erro.setPrefWidth(300);
        erro.setMinHeight(Region.USE_PREF_SIZE);
        erro.setManaged(false);
        erro.setVisible(false);

        Button adicionar = new Button("Adicionar ao carrinho");
        adicionar.setGraphic(Icones.criar("carrinho"));
        adicionar.getStyleClass().add("botao-primario");
        adicionar.setDefaultButton(true);
        adicionar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(adicionar, Priority.ALWAYS);
        adicionar.setDisable(produto.getEstoque() <= 0);

        Button fechar = new Button("Fechar");
        fechar.getStyleClass().add("botao-secundario");
        fechar.setCancelButton(true);

        Runnable atualizarQuantidade = () -> {
            valorQuantidade.setText(String.valueOf(quantidade[0]));
            menos.setDisable(quantidade[0] <= 1);
            mais.setDisable(quantidade[0] >= produto.getEstoque());
            erro.setVisible(false);
            erro.setManaged(false);
        };
        menos.setOnAction(e -> {
            quantidade[0]--;
            atualizarQuantidade.run();
        });
        mais.setOnAction(e -> {
            quantidade[0]++;
            atualizarQuantidade.run();
        });
        atualizarQuantidade.run();

        Label rotuloQuantidade = new Label("Quantidade");
        rotuloQuantidade.getStyleClass().add("rotulo-campo");

        VBox informacoes = new VBox(12, codigo, nome, preco, estoque, jaNoCarrinho, rotuloQuantidade, stepper, erro,
                new HBox(10, adicionar, fechar));
        informacoes.setFillWidth(true);
        HBox.setHgrow(informacoes, Priority.ALWAYS);

        HBox raiz = new HBox(26, imagem, informacoes);
        raiz.getStyleClass().add("dialogo");
        raiz.setAlignment(Pos.TOP_LEFT);

        Stage palco = Dialogo.criar(origem, raiz);
        fechar.setOnAction(e -> palco.close());
        adicionar.setOnAction(e -> {
            try {
                contexto.getCarrinho().adicionar(produto, quantidade[0]);
                palco.close();
                if (aposAdicionar != null) {
                    aposAdicionar.accept(produto, quantidade[0]);
                }
            } catch (QuantidadeInvalidaException | EstoqueInsuficienteException ex) {
                erro.setText(ex.getMessage());
                erro.setVisible(true);
                erro.setManaged(true);
            }
        });
        palco.showAndWait();
    }

    private static Button botaoStepper(String icone) {
        Button botao = new Button();
        botao.setGraphic(Icones.criar(icone, "icone-p"));
        botao.getStyleClass().add("stepper-btn");
        return botao;
    }
}
