package br.com.lojavirtual.ui;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.PagamentoRecusadoException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.pagamento.CartaoCredito;
import br.com.lojavirtual.model.pagamento.CartaoDebito;
import br.com.lojavirtual.model.pagamento.FormaPagamento;
import br.com.lojavirtual.model.pagamento.PagamentoPix;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Janela de pagamento: o comprador escolhe a forma de pagamento, vê o desconto e confirma.
// Quem cria o objeto da forma de pagamento é este controller; o cálculo do valor final e o
// processamento são polimórficos (FormaPagamento) e a finalização fica no ServicoCompra.
public class PagamentoController {
    private final Contexto contexto;
    private Compra compraConcluida;

    @FXML
    private ToggleGroup grupoPagamento;
    @FXML
    private RadioButton rbCredito;
    @FXML
    private RadioButton rbDebito;
    @FXML
    private RadioButton rbPix;
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
        // os campos do cartão só aparecem para crédito e débito
        painelCartao.visibleProperty().bind(rbPix.selectedProperty().not());
        painelCartao.managedProperty().bind(painelCartao.visibleProperty());

        grupoPagamento.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            atualizarValores();
            if (painelCartao.getScene() != null) {
                painelCartao.getScene().getWindow().sizeToScene();
            }
        });
        rbCredito.setSelected(true);
        atualizarValores();
    }

    @FXML
    private void confirmar() {
        FormaPagamento forma = criarFormaPagamento();
        try {
            compraConcluida = contexto.getServicoCompra()
                    .finalizarCompra(contexto.getCarrinho(), contexto.getNomeComprador(), forma);
            fechar();
        } catch (IllegalArgumentException e) {
            // dados do cartão inválidos (número, titular, CVV, validade)
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
        if (rbPix.isSelected()) {
            return new PagamentoPix();
        }
        String numero = campoNumero.getText().replaceAll("[\\s-]", "");
        String titular = campoTitular.getText();
        String validade = campoValidade.getText().trim();
        String cvv = campoCvv.getText().trim();
        if (rbDebito.isSelected()) {
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
    }
}
