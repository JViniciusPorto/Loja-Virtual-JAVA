package br.com.lojavirtual.exception;

public class CarrinhoVazioException extends IllegalArgumentException{

    public CarrinhoVazioException() {
        super("O carrinho está vazio.");
    }
    
}
