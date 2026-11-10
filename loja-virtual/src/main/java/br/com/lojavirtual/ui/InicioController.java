package br.com.lojavirtual.ui;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

// Tela inicial: escolher entre comprador (informando o nome) e administrador (com senha).
public class InicioController {
    private final Contexto contexto;

    @FXML
    private TextField campoNome;
    @FXML
    private PasswordField campoSenha;

    public InicioController(Contexto contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void entrarComoComprador() {
        String nome = campoNome.getText();
        if (nome == null || nome.isBlank()) {
            Alertas.erro(campoNome, "Nome obrigatório", "Informe seu nome para entrar como comprador.");
            return;
        }
        contexto.entrarComoComprador(nome.trim().replaceAll("\\s+", " "));
        contexto.getNavegador().mostrarComprador();
    }

    @FXML
    private void entrarComoAdmin() {
        if (!Acesso.senhaAdminCorreta(campoSenha.getText())) {
            Alertas.erro(campoSenha, "Senha incorreta", "A senha do administrador não confere.");
            campoSenha.clear();
            campoSenha.requestFocus();
            return;
        }
        campoSenha.clear();
        contexto.getNavegador().mostrarAdmin();
    }
}
