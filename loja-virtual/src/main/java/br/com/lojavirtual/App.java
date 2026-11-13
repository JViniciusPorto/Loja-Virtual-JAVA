package br.com.lojavirtual;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.repository.RepositorioCompra;
import br.com.lojavirtual.repository.RepositorioProdutos;
import br.com.lojavirtual.service.ServicoCompra;
import br.com.lojavirtual.service.ServicoProdutos;
import br.com.lojavirtual.ui.Alertas;
import br.com.lojavirtual.ui.Contexto;
import br.com.lojavirtual.ui.Navegador;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

// Ponto de entrada da interface gráfica: monta os serviços (uma única instância de cada,
// compartilhada por todas as telas) e abre a tela inicial.
public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            ServicoProdutos servicoProdutos = new ServicoProdutos(new RepositorioProdutos());
            ServicoCompra servicoCompra = new ServicoCompra(servicoProdutos, new RepositorioCompra());

            Contexto contexto = new Contexto(servicoProdutos, servicoCompra);
            Navegador navegador = new Navegador(stage, contexto);
            contexto.setNavegador(navegador);

            stage.setMinWidth(960);
            stage.setMinHeight(640);
            navegador.mostrarInicio();
            stage.show();
        } catch (ErroPersistenciaException e) {
            Alertas.erro(null, "Erro ao carregar os dados",
                    e.getMessage() + "\n\nCorrija o arquivo na pasta 'data' e abra o programa novamente.");
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
