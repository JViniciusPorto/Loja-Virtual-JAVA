package br.com.lojavirtual.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.Carrinho;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCarrinho;
import br.com.lojavirtual.model.Produto;
import br.com.lojavirtual.model.ResumoCompra;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Tela do comprador, com três abas: produtos disponíveis, carrinho e histórico de compras.
// As regras (estoque, quantidade, pagamento, gravação) ficam no Carrinho e nos serviços.
public class CompradorController {
    private static final int ABA_HISTORICO = 2;

    private final Contexto contexto;

    @FXML
    private Label labelBoasVindas;
    @FXML
    private TabPane abas;

    // aba Produtos
    @FXML
    private TableView<Produto> tabelaProdutos;
    @FXML
    private TableColumn<Produto, Integer> colProdId;
    @FXML
    private TableColumn<Produto, String> colProdNome;
    @FXML
    private TableColumn<Produto, String> colProdPreco;
    @FXML
    private TableColumn<Produto, Integer> colProdEstoque;
    @FXML
    private TextField campoQtdProduto;
    @FXML
    private Label labelStatus;

    // aba Carrinho
    @FXML
    private TableView<ItemCarrinho> tabelaCarrinho;
    @FXML
    private TableColumn<ItemCarrinho, Integer> colCarId;
    @FXML
    private TableColumn<ItemCarrinho, String> colCarNome;
    @FXML
    private TableColumn<ItemCarrinho, Integer> colCarQtd;
    @FXML
    private TableColumn<ItemCarrinho, String> colCarPreco;
    @FXML
    private TableColumn<ItemCarrinho, String> colCarSubtotal;
    @FXML
    private TextField campoQtdCarrinho;
    @FXML
    private Label labelSubtotal;
    @FXML
    private Button botaoFinalizar;

    // aba Histórico
    @FXML
    private TableView<ResumoCompra> tabelaHistorico;
    @FXML
    private TableColumn<ResumoCompra, Integer> colHistId;
    @FXML
    private TableColumn<ResumoCompra, String> colHistData;
    @FXML
    private TableColumn<ResumoCompra, String> colHistPagamento;
    @FXML
    private TableColumn<ResumoCompra, String> colHistValor;
    @FXML
    private Label labelTotalGasto;

    public CompradorController(Contexto contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        labelBoasVindas.setText("Olá, " + contexto.getNomeComprador() + "!");
        configurarTabelaProdutos();
        configurarTabelaCarrinho();
        configurarTabelaHistorico();

        // sempre que o usuário troca de aba os dados são relidos (o estoque pode ter mudado)
        abas.getSelectionModel().selectedIndexProperty().addListener((obs, antigo, novo) -> atualizarTudo());
        atualizarTudo();
    }

    // ===================== Configuração das tabelas =====================

    private void configurarTabelaProdutos() {
        colProdId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colProdNome.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNome()));
        colProdPreco.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formatos.dinheiro(c.getValue().getPreco())));
        colProdEstoque.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstoque()));
        colProdId.setStyle("-fx-alignment: CENTER;");
        colProdPreco.setStyle("-fx-alignment: CENTER-RIGHT;");
        colProdEstoque.setStyle("-fx-alignment: CENTER;");
        Tabelas.distribuir(tabelaProdutos, new double[] {0.12, 0.46, 0.24, 0.18},
                colProdId, colProdNome, colProdPreco, colProdEstoque);
    }

    private void configurarTabelaCarrinho() {
        colCarId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getProduto().getId()));
        colCarNome.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getProduto().getNome()));
        colCarQtd.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getQuantidade()));
        colCarPreco.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(Formatos.dinheiro(c.getValue().getProduto().getPreco())));
        colCarSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formatos.dinheiro(c.getValue().getSubtotal())));
        colCarId.setStyle("-fx-alignment: CENTER;");
        colCarQtd.setStyle("-fx-alignment: CENTER;");
        colCarPreco.setStyle("-fx-alignment: CENTER-RIGHT;");
        colCarSubtotal.setStyle("-fx-alignment: CENTER-RIGHT;");
        Tabelas.distribuir(tabelaCarrinho, new double[] {0.10, 0.38, 0.12, 0.20, 0.20},
                colCarId, colCarNome, colCarQtd, colCarPreco, colCarSubtotal);

        // clicar num item já mostra a quantidade dele no campo, pronta para alterar
        tabelaCarrinho.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                campoQtdCarrinho.setText(String.valueOf(novo.getQuantidade()));
            }
        });
    }

    private void configurarTabelaHistorico() {
        colHistId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().idCompra()));
        colHistData.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formatos.dataHora(c.getValue().horarioCompra())));
        colHistPagamento.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(Formatos.formaPagamento(c.getValue().formaPagamento())));
        colHistValor.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formatos.dinheiro(c.getValue().valorFinal())));
        colHistId.setStyle("-fx-alignment: CENTER;");
        colHistValor.setStyle("-fx-alignment: CENTER-RIGHT;");
        Tabelas.distribuir(tabelaHistorico, new double[] {0.14, 0.30, 0.30, 0.26},
                colHistId, colHistData, colHistPagamento, colHistValor);
    }

    // ===================== Ações: aba Produtos =====================

    @FXML
    private void adicionarAoCarrinho() {
        Produto produto = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (produto == null) {
            Alertas.erro(tabelaProdutos, "Nenhum produto selecionado", "Selecione um produto na tabela.");
            return;
        }
        try {
            int quantidade = Integer.parseInt(campoQtdProduto.getText().trim());
            contexto.getCarrinho().adicionar(produto, quantidade);
            atualizarCarrinho();
            labelStatus.setText(quantidade + " x " + produto.getNome() + " adicionado ao carrinho.");
        } catch (NumberFormatException e) {
            Alertas.erro(tabelaProdutos, "Quantidade inválida", "Informe a quantidade como número inteiro (ex.: 2).");
        } catch (QuantidadeInvalidaException | EstoqueInsuficienteException e) {
            Alertas.erro(tabelaProdutos, "Não foi possível adicionar", e.getMessage());
        }
    }

    // ===================== Ações: aba Carrinho =====================

    @FXML
    private void alterarQuantidade() {
        ItemCarrinho item = tabelaCarrinho.getSelectionModel().getSelectedItem();
        if (item == null) {
            Alertas.erro(tabelaCarrinho, "Nenhum item selecionado", "Selecione um item do carrinho.");
            return;
        }
        try {
            int quantidade = Integer.parseInt(campoQtdCarrinho.getText().trim());
            contexto.getCarrinho().alterarQuantidade(item.getProduto().getId(), quantidade);
            atualizarCarrinho();
        } catch (NumberFormatException e) {
            Alertas.erro(tabelaCarrinho, "Quantidade inválida", "Informe a quantidade como número inteiro (ex.: 2).");
        } catch (ProdutoNaoEncontradoException | QuantidadeInvalidaException | EstoqueInsuficienteException e) {
            Alertas.erro(tabelaCarrinho, "Não foi possível alterar a quantidade", e.getMessage());
        }
    }

    @FXML
    private void removerItem() {
        ItemCarrinho item = tabelaCarrinho.getSelectionModel().getSelectedItem();
        if (item == null) {
            Alertas.erro(tabelaCarrinho, "Nenhum item selecionado", "Selecione um item do carrinho.");
            return;
        }
        try {
            contexto.getCarrinho().remover(item.getProduto().getId());
            atualizarCarrinho();
        } catch (ProdutoNaoEncontradoException e) {
            Alertas.erro(tabelaCarrinho, "Não foi possível remover", e.getMessage());
        }
    }

    @FXML
    private void limparCarrinho() {
        if (contexto.getCarrinho().estaVazio()) {
            return;
        }
        if (Alertas.confirmar(tabelaCarrinho, "Limpar carrinho", "Remover todos os itens do carrinho?")) {
            contexto.getCarrinho().limpar();
            atualizarCarrinho();
        }
    }

    @FXML
    private void finalizarCompra() {
        if (contexto.getCarrinho().estaVazio()) {
            Alertas.erro(tabelaCarrinho, "Carrinho vazio", "Adicione produtos ao carrinho antes de finalizar.");
            return;
        }
        try {
            Compra compra = abrirPagamento();
            if (compra != null) {
                Alertas.texto(tabelaCarrinho, "Compra concluída", contexto.getServicoCompra().gerarResumo(compra));
                atualizarTudo();
                abas.getSelectionModel().select(ABA_HISTORICO);
            }
        } catch (IOException e) {
            Alertas.erro(tabelaCarrinho, "Erro ao abrir o pagamento",
                    "Não foi possível abrir a tela de pagamento: " + e.getMessage());
        }
    }

    // Abre a janela de pagamento e devolve a compra concluída (null se o usuário cancelou ou deu erro)
    private Compra abrirPagamento() throws IOException {
        FXMLLoader loader = contexto.getNavegador().novoLoader("pagamento.fxml");
        Parent raiz = loader.load();
        PagamentoController controller = loader.getController();
        Stage dialogo = contexto.getNavegador().novoDialogo(raiz, "Pagamento");
        dialogo.showAndWait();
        return controller.getCompraConcluida();
    }

    // ===================== Ações: aba Histórico / cabeçalho =====================

    @FXML
    private void atualizarHistorico() {
        String nome = contexto.getNomeComprador();
        try {
            List<ResumoCompra> compras = new ArrayList<>(contexto.getServicoCompra().consultarHistorico(nome));
            Collections.reverse(compras); // mais recente primeiro
            Tabelas.atualizar(tabelaHistorico, compras);
            labelTotalGasto.setText("Total gasto: " + Formatos.dinheiro(contexto.getServicoCompra().calcularTotalGasto(nome)));
        } catch (ErroPersistenciaException e) {
            Alertas.erro(tabelaHistorico, "Não foi possível ler o histórico", e.getMessage());
        }
    }

    @FXML
    private void sair() {
        if (!contexto.getCarrinho().estaVazio()
                && !Alertas.confirmar(labelBoasVindas, "Sair", "Seu carrinho será descartado. Deseja sair?")) {
            return;
        }
        contexto.sairDoComprador();
        contexto.getNavegador().mostrarInicio();
    }

    // ===================== Atualização das telas =====================

    private void atualizarTudo() {
        atualizarCatalogo();
        atualizarCarrinho();
        atualizarHistorico();
    }

    private void atualizarCatalogo() {
        Tabelas.atualizar(tabelaProdutos, contexto.getServicoProdutos().listar());
    }

    private void atualizarCarrinho() {
        Carrinho carrinho = contexto.getCarrinho();
        Tabelas.atualizar(tabelaCarrinho, carrinho.getItens());
        labelSubtotal.setText("Subtotal: " + Formatos.dinheiro(carrinho.calcularSubtotal()));
        botaoFinalizar.setDisable(carrinho.estaVazio());
    }
}
