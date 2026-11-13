package br.com.lojavirtual.model;

import java.time.LocalDateTime;

public record ResumoCompra(int idCompra, LocalDateTime horarioCompra, String formaPagamento, double valorFinal) {
}
