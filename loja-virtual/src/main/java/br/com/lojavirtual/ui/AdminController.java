package br.com.lojavirtual.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.ProdutoInvalidoException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.Produto;
import br.com.lojavirtual.service.ServicoProdutos;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

// Tela do administrador: cadastrar, consultar, editar e remover produtos.
// Toda regra e toda gravação em arquivo ficam no ServicoProdutos; aqui só há leitura dos
// campos, chamada do serviço e exibição do resultado ou da mensagem de erro.
public class AdminController {
    private static final double LARGURA_DUAS_COLUNAS = 1020;

    private final Contexto contexto;

    // produto selecionado na tabela (null = cadastrando um novo)
    private Produto emEdicao;
    private boolean colunaUnica;

    // foto escolhida no formulário que ainda não foi gravada (ela só é copiada ao salvar o produto)
    private Path fotoPendente;
    private boolean removerFotoPendente;
    private File ultimaPasta;

    @FXML
    private StackPane areaCentral;
    @FXML
    private ScrollPane rolagem;
    @FXML
    private GridPane colunas;
    @FXML
    private VBox cartaoTabela;
    @FXML
    private VBox cartaoFormulario;
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
    private TableColumn<Produto, Void> colAcoes;
    @FXML
    private Label labelTotal;
    @FXML
    private Label statProdutos;
    @FXML
    private Label statUnidades;
    @FXML
    private Label statValorEstoque;
    @FXML
    private Label statEstoqueBaixo;
    @FXML
    private Label rotuloEstoqueBaixo;
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
    @FXML
    private StackPane previaFoto;
    @FXML
    private Button botaoRemoverFoto;

    public AdminController(Contexto contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        configurarTabela();
        previaFoto.setCursor(Cursor.HAND);
        previaFoto.setOnMouseClicked(e -> escolherFoto());

        // clicar numa linha carrega o produto no formulário para editar
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                iniciarEdicao(novo);
            }
        });

        // em janelas estreitas o formulário passa para baixo da tabela
        rolagem.widthProperty().addListener((obs, antigo, novo) -> ajustarColunas(novo.doubleValue()));

        recarregar();
        limparFormulario();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colNome.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNome()));
        colPreco.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formatos.dinheiro(c.getValue().getPreco())));
        colEstoque.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstoque()));
        colAcoes.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(null));
        colId.setStyle("-fx-alignment: CENTER-LEFT;");
        colPreco.setStyle("-fx-alignment: CENTER-LEFT;");

        // nome do produto acompanhado da miniatura da foto
        colNome.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(String nome, boolean vazio) {
                super.updateItem(nome, vazio);
                Produto produto = getTableRow() == null ? null : getTableRow().getItem();
                if (vazio || produto == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                StackPane miniatura = Miniaturas.criar(produto, 40);
                miniatura.setMinWidth(40);
                miniatura.setPrefWidth(40);
                miniatura.setMaxWidth(40);
                HBox caixa = new HBox(12, miniatura, new Label(nome));
                caixa.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caixa);
                setText(null);
            }
        });

        // estoque como "pílula": verde (normal), laranja (baixo) ou vermelha (zerado)
        colEstoque.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Integer estoque, boolean vazio) {
                super.updateItem(estoque, vazio);
                if (vazio || estoque == null) {
                    setGraphic(null);
                    return;
                }
                Label pilula = new Label(estoque.toString());
                pilula.getStyleClass().addAll("pilula",
                        estoque <= 0 ? "pilula-alerta" : estoque <= Visual.ESTOQUE_BAIXO ? "pilula-laranja" : "pilula-ok");
                setGraphic(pilula);
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        // ações de cada linha: editar e remover
        colAcoes.setCellFactory(coluna -> new TableCell<>() {
            private final Button editar = botaoDeAcao("editar", "Editar produto", false);
            private final Button remover = botaoDeAcao("lixeira", "Remover produto", true);
            private final HBox caixa = new HBox(6, editar, remover);

            {
                caixa.setAlignment(Pos.CENTER_LEFT);
                editar.setOnAction(e -> {
                    Produto produto = getTableView().getItems().get(getIndex());
                    tabela.getSelectionModel().select(produto);
                    iniciarEdicao(produto);
                });
                remover.setOnAction(e -> {
                    tabela.getSelectionModel().select(getTableView().getItems().get(getIndex()));
                    remover();
                });
            }

            @Override
            protected void updateItem(Void item, boolean vazio) {
                super.updateItem(item, vazio);
                setGraphic(vazio ? null : caixa);
            }
        });

        Tabelas.distribuir(tabela, new double[] {0.10, 0.34, 0.20, 0.16, 0.20},
                colId, colNome, colPreco, colEstoque, colAcoes);
    }

    private Button botaoDeAcao(String icone, String dica, boolean perigo) {
        Button botao = new Button();
        botao.setGraphic(Icones.criar(icone, "icone-p"));
        botao.getStyleClass().add("botao-icone");
        if (perigo) {
            botao.getStyleClass().add("botao-icone-perigo");
        }
        botao.setTooltip(new Tooltip(dica));
        return botao;
    }

    private void ajustarColunas(double largura) {
        boolean estreito = largura < LARGURA_DUAS_COLUNAS;
        if (estreito == colunaUnica) {
            return;
        }
        colunaUnica = estreito;
        if (estreito) {
            GridPane.setColumnIndex(cartaoFormulario, 0);
            GridPane.setRowIndex(cartaoFormulario, 1);
            ColumnConstraints unica = new ColumnConstraints();
            unica.setHgrow(Priority.ALWAYS);
            unica.setMinWidth(320);
            colunas.getColumnConstraints().setAll(unica);
        } else {
            GridPane.setColumnIndex(cartaoFormulario, 1);
            GridPane.setRowIndex(cartaoFormulario, 0);
            ColumnConstraints principal = new ColumnConstraints();
            principal.setHgrow(Priority.ALWAYS);
            principal.setMinWidth(320);
            ColumnConstraints lateral = new ColumnConstraints();
            lateral.setHgrow(Priority.NEVER);
            lateral.setMinWidth(300);
            lateral.setPrefWidth(330);
            colunas.getColumnConstraints().setAll(principal, lateral);
        }
    }

    @FXML
    private void salvar() {
        try {
            String nome = campoNome.getText();
            double preco = Formatos.lerDinheiro(campoPreco.getText());
            int estoque = Integer.parseInt(campoEstoque.getText().trim());
            ServicoProdutos servico = contexto.getServicoProdutos();

            String mensagem;
            int idSalvo;
            if (emEdicao == null) {
                Produto novo = servico.cadastrar(nome, preco, estoque);
                idSalvo = novo.getId();
                mensagem = "Produto cadastrado: " + novo.getNome() + " (ID " + novo.getId() + ").";
            } else {
                Produto editado = servico.editar(emEdicao.getId(), nome, preco, estoque);
                idSalvo = editado.getId();
                mensagem = "Produto atualizado: " + editado.getNome() + " (ID " + editado.getId() + ").";
            }
            aplicarFoto(idSalvo);

            recarregar();
            limparFormulario();
            Toast.sucesso(areaCentral, mensagem);
        } catch (NumberFormatException e) {
            Alertas.erro(areaCentral, "Valor inválido",
                    "Informe o preço como número (ex.: 89,90) e o estoque como número inteiro (ex.: 10).");
        } catch (ProdutoInvalidoException | QuantidadeInvalidaException | ProdutoNaoEncontradoException
                 | ErroPersistenciaException e) {
            Alertas.erro(areaCentral, "Não foi possível salvar o produto", e.getMessage());
        }
    }

    @FXML
    private void novo() {
        limparFormulario();
    }

    @FXML
    private void remover() {
        if (emEdicao == null) {
            Alertas.erro(areaCentral, "Nenhum produto selecionado", "Selecione um produto na tabela para remover.");
            return;
        }
        String nome = emEdicao.getNome();
        if (!Alertas.confirmar(areaCentral, "Remover produto", "Deseja remover \"" + nome + "\"?",
                "Remover", true)) {
            return;
        }
        try {
            int idRemovido = emEdicao.getId();
            contexto.getServicoProdutos().remover(idRemovido);
            try {
                Fotos.remover(idRemovido);
            } catch (IOException e) {
                // a foto que sobrou não atrapalha: o produto já foi removido
            }
            recarregar();
            limparFormulario();
            Toast.sucesso(areaCentral, "Produto removido: " + nome + ".");
        } catch (ProdutoNaoEncontradoException | ErroPersistenciaException e) {
            Alertas.erro(areaCentral, "Não foi possível remover o produto", e.getMessage());
        }
    }

    @FXML
    private void sair() {
        contexto.getNavegador().mostrarInicio();
    }

    private void recarregar() {
        List<Produto> produtos = contexto.getServicoProdutos().listar();
        Tabelas.atualizar(tabela, produtos);
        labelTotal.setText(produtos.size() + (produtos.size() == 1 ? " produto" : " produtos"));
        atualizarIndicadores(produtos);
    }

    // Indicadores calculados só com o que existe no cadastro de produtos
    private void atualizarIndicadores(List<Produto> produtos) {
        int unidades = 0;
        double valorEstoque = 0;
        int baixo = 0;
        for (Produto produto : produtos) {
            unidades += produto.getEstoque();
            valorEstoque += produto.getPreco() * produto.getEstoque();
            if (produto.getEstoque() <= Visual.ESTOQUE_BAIXO) {
                baixo++;
            }
        }
        statProdutos.setText(String.valueOf(produtos.size()));
        statUnidades.setText(String.valueOf(unidades));
        statValorEstoque.setText(Formatos.dinheiro(valorEstoque));
        statEstoqueBaixo.setText(String.valueOf(baixo));
        rotuloEstoqueBaixo.setText("Estoque baixo (até " + Visual.ESTOQUE_BAIXO + " un.)");
    }

    private void iniciarEdicao(Produto produto) {
        emEdicao = produto;
        labelModo.setText("Editando produto #" + produto.getId());
        campoNome.setText(produto.getNome());
        campoPreco.setText(Formatos.numeroComVirgula(produto.getPreco()));
        campoEstoque.setText(String.valueOf(produto.getEstoque()));
        botaoSalvar.setText("Salvar alterações");
        botaoRemover.setDisable(false);
        fotoPendente = null;
        removerFotoPendente = false;
        atualizarPrevia();
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
        fotoPendente = null;
        removerFotoPendente = false;
        atualizarPrevia();
    }

    // ===================== Foto do produto =====================

    @FXML
    private void escolherFoto() {
        FileChooser seletor = new FileChooser();
        seletor.setTitle("Escolher foto do produto");
        seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens (PNG, JPG, GIF, BMP)",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        if (ultimaPasta != null && ultimaPasta.isDirectory()) {
            seletor.setInitialDirectory(ultimaPasta);
        }
        File arquivo = seletor.showOpenDialog(previaFoto.getScene().getWindow());
        if (arquivo == null) {
            return;
        }
        ultimaPasta = arquivo.getParentFile();
        String problema = Fotos.validar(arquivo.toPath());
        if (problema != null) {
            Alertas.erro(areaCentral, "Foto inválida", problema);
            return;
        }
        fotoPendente = arquivo.toPath();
        removerFotoPendente = false;
        atualizarPrevia();
    }

    @FXML
    private void removerFoto() {
        fotoPendente = null;
        removerFotoPendente = emEdicao != null && Fotos.localizar(emEdicao.getId()) != null;
        atualizarPrevia();
    }

    // Mostra no formulário a foto escolhida ou a que o produto em edição já tem
    private void atualizarPrevia() {
        Path mostrada = fotoPendente;
        if (mostrada == null && !removerFotoPendente && emEdicao != null) {
            mostrada = Fotos.localizar(emEdicao.getId());
        }
        previaFoto.getChildren().setAll(Miniaturas.criarPrevia(mostrada, 120));
        botaoRemoverFoto.setDisable(mostrada == null);
    }

    // Grava (ou apaga) a foto do produto já salvo; se falhar, o produto continua salvo
    private void aplicarFoto(int idProduto) {
        try {
            if (fotoPendente != null) {
                Fotos.salvar(idProduto, fotoPendente);
            } else if (removerFotoPendente) {
                Fotos.remover(idProduto);
            }
        } catch (IOException e) {
            Alertas.erro(areaCentral, "Produto salvo, mas a foto não foi gravada",
                    "Não foi possível copiar a foto para a pasta data/imagens: " + e.getMessage());
        }
    }
}
