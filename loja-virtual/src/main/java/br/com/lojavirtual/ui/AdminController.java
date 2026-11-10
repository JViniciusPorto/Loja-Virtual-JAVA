package br.com.lojavirtual.ui;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.ProdutoInvalidoException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.Produto;
import br.com.lojavirtual.service.ServicoProdutos;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

// Tela do administrador: cadastrar, consultar, editar e remover produtos.
// Toda regra e toda gravação em arquivo ficam no ServicoProdutos; aqui só há leitura dos
// campos, chamada do serviço e exibição do resultado ou da mensagem de erro.
public class AdminController {
    private final Contexto contexto;

    // produto selecionado na tabela (null = cadastrando um novo)
    private Produto emEdicao;

    @FXML
    private TableView<Produto> tabela;
    @FXML
    private TableColumn<Produto, Integer> colId;
    @FXML
    private TableColumn<Produto, String> colNome;
    @FXML
    private TableColumn<Produto, String> colPreco;
    @FXML
    private TableColumn<Produto, Integer> colEstoque;
    @FXML
    private Label labelTotal;
    @FXML
    private Label labelStatus;
    @FXML
    private Label labelModo;
    @FXML
    private TextField campoNome;
    @FXML
    private TextField campoPreco;
    @FXML
    private TextField campoEstoque;
    @FXML
    private Button botaoSalvar;
    @FXML
    private Button botaoRemover;

    public AdminController(Contexto contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colNome.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNome()));
        colPreco.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formatos.dinheiro(c.getValue().getPreco())));
        colEstoque.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstoque()));
        colId.setStyle("-fx-alignment: CENTER;");
        colPreco.setStyle("-fx-alignment: CENTER-RIGHT;");
        colEstoque.setStyle("-fx-alignment: CENTER;");
        Tabelas.distribuir(tabela, new double[] {0.12, 0.44, 0.24, 0.20}, colId, colNome, colPreco, colEstoque);

        // clicar numa linha carrega o produto no formulário para editar
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                iniciarEdicao(novo);
            }
        });

        recarregar();
        limparFormulario();
    }

    @FXML
    private void salvar() {
        try {
            String nome = campoNome.getText();
            double preco = Formatos.lerDinheiro(campoPreco.getText());
            int estoque = Integer.parseInt(campoEstoque.getText().trim());
            ServicoProdutos servico = contexto.getServicoProdutos();

            String mensagem;
            if (emEdicao == null) {
                Produto novo = servico.cadastrar(nome, preco, estoque);
                mensagem = "Produto cadastrado: " + novo.getNome() + " (ID " + novo.getId() + ").";
            } else {
                Produto editado = servico.editar(emEdicao.getId(), nome, preco, estoque);
                mensagem = "Produto atualizado: " + editado.getNome() + " (ID " + editado.getId() + ").";
            }

            recarregar();
            limparFormulario();
            labelStatus.setText(mensagem);
        } catch (NumberFormatException e) {
            Alertas.erro(tabela, "Valor inválido",
                    "Informe o preço como número (ex.: 89,90) e o estoque como número inteiro (ex.: 10).");
        } catch (ProdutoInvalidoException | QuantidadeInvalidaException | ProdutoNaoEncontradoException
                 | ErroPersistenciaException e) {
            Alertas.erro(tabela, "Não foi possível salvar o produto", e.getMessage());
        }
    }

    @FXML
    private void novo() {
        limparFormulario();
        labelStatus.setText("");
    }

    @FXML
    private void remover() {
        if (emEdicao == null) {
            Alertas.erro(tabela, "Nenhum produto selecionado", "Selecione um produto na tabela para remover.");
            return;
        }
        String nome = emEdicao.getNome();
        if (!Alertas.confirmar(tabela, "Remover produto", "Deseja remover \"" + nome + "\"?")) {
            return;
        }
        try {
            contexto.getServicoProdutos().remover(emEdicao.getId());
            recarregar();
            limparFormulario();
            labelStatus.setText("Produto removido: " + nome + ".");
        } catch (ProdutoNaoEncontradoException | ErroPersistenciaException e) {
            Alertas.erro(tabela, "Não foi possível remover o produto", e.getMessage());
        }
    }

    @FXML
    private void sair() {
        contexto.getNavegador().mostrarInicio();
    }

    private void recarregar() {
        Tabelas.atualizar(tabela, contexto.getServicoProdutos().listar());
        labelTotal.setText(tabela.getItems().size() + " produto(s) cadastrado(s)");
    }

    private void iniciarEdicao(Produto produto) {
        emEdicao = produto;
        labelModo.setText("Editando produto #" + produto.getId());
        campoNome.setText(produto.getNome());
        campoPreco.setText(Formatos.numeroComVirgula(produto.getPreco()));
        campoEstoque.setText(String.valueOf(produto.getEstoque()));
        botaoSalvar.setText("Salvar alterações");
        botaoRemover.setDisable(false);
    }

    private void limparFormulario() {
        emEdicao = null;
        tabela.getSelectionModel().clearSelection();
        labelModo.setText("Novo produto");
        campoNome.clear();
        campoPreco.clear();
        campoEstoque.clear();
        botaoSalvar.setText("Cadastrar produto");
        botaoRemover.setDisable(true);
    }
}
