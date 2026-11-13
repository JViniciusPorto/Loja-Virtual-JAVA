package br.com.lojavirtual.ui;

import java.text.Normalizer;
import java.util.Locale;

// Constantes e pequenos utilitários visuais compartilhados pelas telas.
public final class Visual {
    // A partir de quantas unidades o produto aparece como "estoque baixo" (só afeta a aparência)
    public static final int ESTOQUE_BAIXO = 10;

    private Visual() {
    }

    // Minúsculas e sem acentos, para a pesquisa achar "teclado" em "Teclado Mecânico" e "cafe" em "Café"
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }
}
