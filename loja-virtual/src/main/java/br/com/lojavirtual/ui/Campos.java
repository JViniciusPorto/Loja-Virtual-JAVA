package br.com.lojavirtual.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

// Mensagens de validação exibidas logo abaixo do campo com problema (borda vermelha + texto).
public final class Campos {
    private static final String CLASSE_ERRO = "campo-erro";

    private Campos() {
    }

    public static void erro(TextInputControl campo, Label mensagem, String texto) {
        if (!campo.getStyleClass().contains(CLASSE_ERRO)) {
            campo.getStyleClass().add(CLASSE_ERRO);
        }
        mensagem.setText(texto);
        mensagem.setVisible(true);
        mensagem.setManaged(true);
        campo.requestFocus();
    }

    public static void limpar(TextInputControl campo, Label mensagem) {
        campo.getStyleClass().remove(CLASSE_ERRO);
        mensagem.setText("");
        mensagem.setVisible(false);
        mensagem.setManaged(false);
    }

    // O erro some assim que a pessoa volta a digitar no campo
    public static void limparAoDigitar(TextInputControl campo, Label mensagem) {
        campo.textProperty().addListener((obs, antigo, novo) -> limpar(campo, mensagem));
        limpar(campo, mensagem);
    }
}
