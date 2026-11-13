package br.com.lojavirtual.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

// Tela inicial: escolher entre comprador (informando o nome) e administrador (com senha).
public class InicioController {
    private final Contexto contexto;

    @FXML
    private ToggleGroup grupoPerfil;
    @FXML
    private ToggleButton abaAdmin;
    @FXML
    private VBox formComprador;
    @FXML
    private VBox formAdmin;
    @FXML
    private TextField campoNome;
    @FXML
    private PasswordField campoSenha;
    @FXML
    private Label erroNome;
    @FXML
    private Label erroSenha;

    public InicioController(Contexto contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        Campos.limparAoDigitar(campoNome, erroNome);
        Campos.limparAoDigitar(campoSenha, erroSenha);

        // os dois "botões de aba" funcionam como um seletor: sempre há um perfil escolhido
        grupoPerfil.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) {
                antigo.setSelected(true);
                return;
            }
            boolean admin = novo == abaAdmin;
            formComprador.setVisible(!admin);
            formComprador.setManaged(!admin);
            formAdmin.setVisible(admin);
            formAdmin.setManaged(admin);
            Platform.runLater(() -> (admin ? campoSenha : campoNome).requestFocus());
        });
        Platform.runLater(campoNome::requestFocus);
    }

    @FXML
    private void entrarComoComprador() {
        String nome = campoNome.getText();
        if (nome == null || nome.isBlank()) {
            Campos.erro(campoNome, erroNome, "Informe seu nome para entrar como comprador.");
            return;
        }
        contexto.entrarComoComprador(nome.trim().replaceAll("\\s+", " "));
        contexto.getNavegador().mostrarComprador();
    }

    @FXML
    private void fecharPrograma() {
        Platform.exit();
    }

    @FXML
    private void entrarComoAdmin() {
        if (!Acesso.senhaAdminCorreta(campoSenha.getText())) {
            campoSenha.clear();
            Campos.erro(campoSenha, erroSenha, "A senha do administrador não confere.");
            return;
        }
        campoSenha.clear();
        contexto.getNavegador().mostrarAdmin();
    }
}
