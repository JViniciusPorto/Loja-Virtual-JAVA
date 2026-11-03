package br.com.lojavirtual.ui;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Conversões entre o que o usuário digita/vê (R$ 89,90; 03/11/2026 14:30) e o que o sistema guarda.
public final class Formatos {
    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {
    }

    public static String dinheiro(double valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor);
    }

    // Aceita "89,90", "89.90", "R$ 89,90" e "1.234,56". Lança NumberFormatException se não for número.
    public static double lerDinheiro(String texto) {
        if (texto == null) {
            throw new NumberFormatException("Valor vazio.");
        }
        String limpo = texto.replace("R$", "").replaceAll("[\\s\\u00A0]", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        if (!limpo.matches("-?\\d+(\\.\\d+)?")) {
            throw new NumberFormatException("Valor inválido: " + texto);
        }
        return Double.parseDouble(limpo);
    }

    // Para preencher campos de edição (sem o símbolo R$)
    public static String numeroComVirgula(double valor) {
        return String.format(BRASIL, "%.2f", valor);
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora.format(DATA_HORA);
    }

    // Nome amigável do tipo gravado nos arquivos (PIX, CARTAO_CREDITO, CARTAO_DEBITO)
    public static String formaPagamento(String tipo) {
        return switch (tipo) {
            case "PIX" -> "Pix";
            case "CARTAO_CREDITO" -> "Cartão de crédito";
            case "CARTAO_DEBITO" -> "Cartão de débito";
            default -> tipo;
        };
    }
}
