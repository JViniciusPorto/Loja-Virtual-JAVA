package br.com.lojavirtual.model;

public class ItemCompra {
    private int idProduto;
    private String nomeProduto;
    private int quantidadeProduto;
    private double precoUnitario;

    public ItemCompra(int idProduto, String nomeProduto, int quantidadeProduto, double precoUnitario){
        this.idProduto = idProduto;
        this.nomeProduto = nomeProduto;
        this.quantidadeProduto = quantidadeProduto;
        this.precoUnitario = precoUnitario;
    }

    public int getIdProduto(){
        return idProduto;
    }

    public String getNomeProduto(){
        return nomeProduto;
    }

    public int getQuantidadeProduto(){
        return quantidadeProduto;
    }

    public double getPrecoUnitario(){
        return precoUnitario;
    }

    public double calcularSubtotal(){
        return precoUnitario*quantidadeProduto;
    }
}
