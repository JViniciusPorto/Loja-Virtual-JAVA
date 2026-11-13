package br.com.lojavirtual.ui;

import br.com.lojavirtual.exception.CvvCartaoInvalidoException;
import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.NumeroCartaoInvalidoException;
import br.com.lojavirtual.exception.PagamentoRecusadoException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.exception.TitularCartaoInvalidoException;
import br.com.lojavirtual.exception.ValidadeCartaoInvalidaException;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.pagamento.CartaoCredito;
import br.com.lojavirtual.model.pagamento.CartaoDebito;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.model.pagamento.PagamentoPix;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

// Janela de pagamento: o comprador escolhe a forma de pagamento, vê o desconto e confirma.
// Quem cria o objeto da forma de pagamento é este controller; o cálculo do valor final, a validação
// dos dados do cartão e o processamento são do modelo (FormaPagamento) e a finalização fica no
// ServicoCompra. Aqui só se mostra o erro ao lado do campo correspondente.
public class PagamentoController {
    private static final double ALTURA_COM_CARTAO = 430;
    private static final double ALTURA_SEM_CARTAO = 130;

    private final Contexto contexto;
    private Compra compraConcluida;

    @FXML
    private ToggleGroup grupoPagamento;
    @FXML
    private ToggleButton tbCredito;
    @FXML
    private ToggleButton tbDebito;
    @FXML
    private ToggleButton tbPix;
    @FXML
    private ScrollPane rolagem;
    @FXML
    private Label infoForma;
    @FXML
    private VBox painelCartao;
    @FXML
    private TextField campoNumero;
    @FXML
    private TextField campoTitular;
    @FXML
    private TextField campoValidade;
    @FXML
    private PasswordField campoCvv;
    @FXML
    private Label erroNumero;
    @FXML
    private Label erroTitular;
    @FXML
    private Label erroValidade;
    @FXML
    private Label erroCvv;
    @FXML
    private Label labelSubtotal;
    @FXML
    private Label labelDesconto;
    @FXML
    private Label labelValorFinal;
    @FXML
    private Button botaoConfirmar;

    public PagamentoController(Contexto contexto) {
        this.contexto = contexto;
    }

    // A compra concluída, ou null se o pagamento foi cancelado
    public Compra getCompraConcluida() {
        return compraConcluida;
    }

    @FXML
    private void initialize() {
        Campos.limparAoDigitar(campoNumero, erroNumero);
        Campos.limparAoDigitar(campoTitular, erroTitular);
        Campos.limparAoDigitar(campoValidade, erroValidade);
        Campos.limparAoDigitar(campoCvv, erroCvv);

        // os campos do cartão só aparecem para crédito e débito
        painelCartao.visibleProperty().bind(tbPix.selectedProperty().not());
        painelCartao.managedProperty().bind(painelCartao.visibleProperty());

        grupoPagamento.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) { // uma forma de pagamento sempre fica escolhida
                if (antigo != null) {
                    antigo.setSelected(true);
                }
                return;
            }
            atualizarValores();
            ajustarAltura();
        });
        tbCredito.setSelected(true);
        atualizarValores();
        rolagem.setPrefHeight(ALTURA_COM_CARTAO);
    }

    @FXML
    private void confirmar() {
        FormaPagamento forma = criarFormaPagamento();
        try {
            compraConcluida = contexto.getServicoCompra()
                    .finalizarCompra(contexto.getCarrinho(), contexto.getNomeComprador(), forma);
            fechar();
        } catch (NumeroCartaoInvalidoException e) {
            Campos.erro(campoNumero, erroNumero, e.getMessage());
        } catch (TitularCartaoInvalidoException e) {
            Campos.erro(campoTitular, erroTitular, e.getMessage());
        } catch (ValidadeCartaoInvalidaException e) {
            Campos.erro(campoValidade, erroValidade, e.getMessage());
        } catch (CvvCartaoInvalidoException e) {
            Campos.erro(campoCvv, erroCvv, e.getMessage());
        } catch (IllegalArgumentException e) {
            // qualquer outro dado inválido do pagamento
            Alertas.erro(botaoConfirmar, "Dados do pagamento inválidos", e.getMessage());
        } catch (EstoqueInsuficienteException | ProdutoNaoEncontradoException | QuantidadeInvalidaException
                 | PagamentoRecusadoException e) {
            Alertas.erro(botaoConfirmar, "Não foi possível concluir a compra", e.getMessage());
        } catch (ErroPersistenciaException e) {
            Alertas.erro(botaoConfirmar, "Erro ao gravar os dados", e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        fechar();
    }

    private void fechar() {
        ((Stage) botaoConfirmar.getScene().getWindow()).close();
    }

    private FormaPagamento criarFormaPagamento() {
        if (tbPix.isSelected()) {
            return new PagamentoPix();
        }
        String numero = campoNumero.getText().replaceAll("[\\s-]", "");
        String titular = campoTitular.getText();
        String validade = campoValidade.getText().trim();
        String cvv = campoCvv.getText().trim();
        if (tbDebito.isSelected()) {
            return new CartaoDebito(numero, titular, validade, cvv);
        }
        return new CartaoCredito(numero, titular, validade, cvv);
    }

    // Mostra subtotal, desconto e valor final da forma escolhida (mesma regra de arredondamento da Compra)
    private void atualizarValores() {
        double subtotal = Compra.arredondar(contexto.getCarrinho().calcularSubtotal());
        double valorFinal = Compra.arredondar(criarFormaPagamento().calcularValorFinal(subtotal));
        double desconto = Compra.arredondar(subtotal - valorFinal);

        labelSubtotal.setText(Formatos.dinheiro(subtotal));
        labelDesconto.setText(Formatos.dinheiro(desconto));
        labelValorFinal.setText(Formatos.dinheiro(valorFinal));

        if (tbPix.isSelected()) {
            infoForma.setText("Pix: 5% de desconto sobre o subtotal.");
        } else if (tbDebito.isSelected()) {
            infoForma.setText("Cartão de débito: 10% de desconto sobre o subtotal.");
        } else {
            infoForma.setText("Cartão de crédito: pagamento pelo valor integral, sem desconto.");
        }
    }

    // A janela encolhe quando os campos do cartão somem (Pix) e volta a crescer quando reaparecem
    private void ajustarAltura() {
        rolagem.setPrefHeight(tbPix.isSelected() ? ALTURA_SEM_CARTAO : ALTURA_COM_CARTAO);
        Platform.runLater(() -> {
            if (rolagem.getScene() != null) {
                Window janela = rolagem.getScene().getWindow();
                if (janela != null && janela.isShowing()) {
                    janela.sizeToScene();
                }
            }
        });
    }
}
