package br.com.lojavirtual.model;

public class Produto {
    private final int id;
    private String nome;
    private double preco;
    private int estoque;

    public Produto(int id, String nome, double preco, int estoque) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.estoque = estoque;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getPreco() {
        return preco;
    }

    public int getEstoque() {
        return estoque;
    }

    public void alterarNome(String nome) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome inválido.");
        }

        this.nome = nome.trim().replaceAll("\\s+", " ");
    }

    public void alterarPreco(double preco) {

        if (!Double.isFinite(preco) || preco <= 0) {
            throw new IllegalArgumentException("Preço inválido.");
        }

        this.preco = Math.round(preco * 100) / 100.0;
    }

    public void alterarEstoque(int estoque) {

        if (estoque < 0) {
            throw new IllegalArgumentException("Estoque inválido.");
        }

        this.estoque = estoque;
    }
}
