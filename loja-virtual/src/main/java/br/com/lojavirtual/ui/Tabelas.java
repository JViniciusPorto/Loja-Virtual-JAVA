package br.com.lojavirtual.ui;

import java.util.Collection;

import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class Tabelas {
    private Tabelas() {
    }

    // Troca o conteúdo da tabela e força o redesenho das células.
    // Sem o refresh(), objetos que continuam na lista mas mudaram por dentro (quantidade no carrinho,
    // estoque do produto) seguiriam aparecendo com o valor antigo.
    public static <T> void atualizar(TableView<T> tabela, Collection<? extends T> itens) {
        tabela.setItems(FXCollections.observableArrayList(itens));
        tabela.refresh();
    }

    // Reparte a largura da tabela entre as colunas, na proporção dos pesos (ex.: 0.2, 0.5, 0.3).
    // A tabela acompanha o tamanho da janela e as colunas ficam fixas (sem arrastar nem ordenar).
    public static void distribuir(TableView<?> tabela, double[] pesos, TableColumn<?, ?>... colunas) {
        for (int i = 0; i < colunas.length; i++) {
            TableColumn<?, ?> coluna = colunas[i];
            coluna.setResizable(false);
            coluna.setSortable(false);
            coluna.prefWidthProperty().bind(tabela.widthProperty().subtract(20).multiply(pesos[i]));
        }
    }
}
