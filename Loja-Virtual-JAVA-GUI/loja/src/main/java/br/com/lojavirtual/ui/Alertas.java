package br.com.lojavirtual.ui;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

// Caixas de mensagem padronizadas. O primeiro parâmetro é qualquer componente da tela que está
// aberta: serve para a mensagem aparecer por cima dela (pode ser null).
public final class Alertas {
    private Alertas() {
    }

    public static void erro(Node origem, String titulo, String mensagem) {
        criar(origem, Alert.AlertType.ERROR, titulo, mensagem).showAndWait();
    }

    public static void info(Node origem, String titulo, String mensagem) {
        criar(origem, Alert.AlertType.INFORMATION, titulo, mensagem).showAndWait();
    }

    public static boolean confirmar(Node origem, String titulo, String mensagem) {
        Alert alerta = criar(origem, Alert.AlertType.CONFIRMATION, titulo, mensagem);
        ButtonType sim = new ButtonType("Sim", ButtonBar.ButtonData.YES);
        ButtonType nao = new ButtonType("Não", ButtonBar.ButtonData.NO);
        alerta.getButtonTypes().setAll(sim, nao);
        Optional<ButtonType> resposta = alerta.showAndWait();
        return resposta.isPresent() && resposta.get() == sim;
    }

    // Texto longo com fonte de largura fixa (usado no resumo da compra)
    public static void texto(Node origem, String titulo, String conteudo) {
        Alert alerta = criar(origem, Alert.AlertType.INFORMATION, titulo, "");
        TextArea area = new TextArea(conteudo);
        area.setEditable(false);
        area.setWrapText(false);
        area.setStyle("-fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 13px;");
        area.setPrefColumnCount(52);
        area.setPrefRowCount(Math.min(22, conteudo.split("\n").length + 1));
        alerta.getDialogPane().setContent(area);
        alerta.showAndWait();
    }

    private static Alert criar(Node origem, Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensagem);
        if (origem != null && origem.getScene() != null) {
            alerta.initOwner(origem.getScene().getWindow());
        }
        alerta.getDialogPane().getStylesheets().add(Navegador.CSS);
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alerta;
    }
}
