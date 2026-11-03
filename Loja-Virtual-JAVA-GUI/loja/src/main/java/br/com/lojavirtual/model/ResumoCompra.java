package br.com.lojavirtual.model;

import java.time.LocalDateTime;

// Uma linha do histórico de compras do comprador (o que o enunciado pede mostrar):
// identificador, data e hora, forma de pagamento (PIX, CARTAO_CREDITO, CARTAO_DEBITO) e valor final pago.
public record ResumoCompra(int idCompra, LocalDateTime horarioCompra, String formaPagamento, double valorFinal) {
}
