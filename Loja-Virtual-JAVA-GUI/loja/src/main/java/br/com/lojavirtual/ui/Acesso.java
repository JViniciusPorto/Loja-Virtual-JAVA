package br.com.lojavirtual.ui;

// Controle de acesso do administrador por senha fixa (estática).
// Para trocar a senha, altere apenas a constante abaixo.
// Atenção: a senha fica escrita no código, o que serve para o trabalho, mas não é segurança real.
public final class Acesso {
    private static final String SENHA_ADMIN = "admin123";

    private Acesso() {
    }

    public static boolean senhaAdminCorreta(String tentativa) {
        return SENHA_ADMIN.equals(tentativa);
    }
}
