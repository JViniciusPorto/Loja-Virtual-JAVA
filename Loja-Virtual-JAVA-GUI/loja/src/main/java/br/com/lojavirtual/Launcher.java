package br.com.lojavirtual;

// Use esta classe para executar pelo botão "Run" do VS Code: uma classe que NÃO estende
// Application evita o erro "JavaFX runtime components are missing".
// Pelo Maven o comando continua sendo:  mvn javafx:run
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
