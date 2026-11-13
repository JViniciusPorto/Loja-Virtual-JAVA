package br.com.lojavirtual.ui;

import javafx.scene.layout.Region;

// Cria ícones vetoriais. A forma de cada ícone está no estilo.css (classe "icone-<nome>") e a cor
// vem do contexto (botão, menu, chip...), então o mesmo ícone serve em qualquer lugar.
// Tamanhos opcionais: "icone-p" (16), padrão (20), "icone-g" (26), "icone-xg" (40), "icone-xxg" (64).
public final class Icones {
    private Icones() {
    }

    public static Region criar(String nome, String... classesExtras) {
        Region icone = new Region();
        icone.getStyleClass().addAll("icone", "icone-" + nome);
        icone.getStyleClass().addAll(classesExtras);
        icone.setMouseTransparent(true);
        return icone;
    }
}
