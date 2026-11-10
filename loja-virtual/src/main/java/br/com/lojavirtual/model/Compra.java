package br.com.lojavirtual.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import br.com.lojavirtual.model.pagamento.FormaPagamento;

public class Compra {
    private int idCompra;

    private LocalDateTime horarioCompra;
    private String nomeComprador;
    private List<ItemCompra> itensComprados;
    private FormaPagamento pagamento;
    private double subtotal;
    private double desconto;
    private double valorFinal;

    public  Compra(int idCompra, LocalDateTime horarioCompra, String nomeComprador, List<ItemCompra> itensComprados, FormaPagamento pagamento){
        this.idCompra = idCompra;
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
        this.subtotal = arredondar(subtotal);
    }

    public void calcularValorFinal(){
        valorFinal = arredondar(pagamento.calcularValorFinal(subtotal));
        desconto = arredondar(subtotal - valorFinal);
    }

    // Dinheiro sempre com 2 casas (arredonda para cima a partir de 5 no terceiro dígito).
    // Evita valores como 80.91000000000001 nos arquivos e diferenças de centavos entre
    // subtotal, desconto e valor final.
    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
