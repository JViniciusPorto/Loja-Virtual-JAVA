package br.com.lojavirtual.model;

import java.time.LocalDateTime;
import java.util.List;

import br.com.lojavirtual.model.pagamento.FormaPagamento;

public class Compra {
    //lembrar de que se o sistema fechar o contador reseta
    private static int proximoId = 1;
    private int idCompra;

    private LocalDateTime horarioCompra;
    private String nomeComprador;
    private List<ItemCompra> itensComprados;
    private FormaPagamento pagamento;
    private double subtotal;
    private double desconto;
    private double valorFinal;

    public  Compra(LocalDateTime horarioCompra, String nomeComprador, List<ItemCompra> itensComprados, FormaPagamento pagamento){
        this.idCompra = Compra.proximoId++;
        this.horarioCompra = horarioCompra;
        this.nomeComprador = nomeComprador;
        this.itensComprados = List.copyOf(itensComprados);
        this.pagamento = pagamento;
    }

    public void calcularSubtotal(){
        double subtotal = 0;
        for(ItemCompra item : itensComprados){
            subtotal += item.calcularSubtotal();
        }
        this.subtotal = subtotal;
    }

    public void calcularValorFinal(){
        valorFinal = pagamento.calcularValorFinal(subtotal);
        desconto = subtotal - valorFinal;
    }

    public boolean processarPagamento() {
        return pagamento.processarPagamento();
    }

    public int getIdCompra() {
        return idCompra;
    }

    public LocalDateTime getHorarioCompra() {
        return horarioCompra;
    }

    public String getNomeComprador() {
        return nomeComprador;
    }

    public List<ItemCompra> getItensComprados() {
        return itensComprados;
    }

    public FormaPagamento getPagamento() {
        return pagamento;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDesconto() {
        return desconto;
    }

    public double getValorFinal() {
        return valorFinal;
    }
    
}
