package br.com.lojavirtual.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.repository.RepositorioCompra;

// Lê as compras de um comprador com todos os detalhes (itens, quantidades, preços e valores).
// Usa só o que já existe: RepositorioCompra.carregarHistorico(nome), que devolve as linhas do arquivo
// do histórico. Esta classe apenas transforma essas linhas em objetos para a tela mostrar; nada é
// gravado e nenhuma regra de negócio é alterada. Fica na pasta ui de propósito: model, service e
// repository não foram modificados.
public class LeitorCompras {
    // Mesmo formato gravado pelo repositório: ITEM: 1001 NOME: Mouse Gamer QTD.: 2 PRECO: 89.9
    private static final Pattern LINHA_ITEM =
            Pattern.compile("^ITEM:\\s*(\\d+)\\s+NOME:\\s*(.*?)\\s+QTD\\.:\\s*(\\d+)\\s+PRECO:\\s*(\\S+)$");

    public record ItemDetalhe(int idProduto, String nome, int quantidade, double precoUnitario) {
        public double subtotal() {
            return Compra.arredondar(quantidade * precoUnitario);
        }
    }

    public record DetalheCompra(int idCompra, LocalDateTime horario, String formaPagamento, double valorFinal,
            List<ItemDetalhe> itens) {
        // O histórico não grava subtotal nem desconto: o subtotal é a soma dos itens e o desconto
        // é a diferença para o valor final gravado.
        public double subtotal() {
            double soma = 0;
            for (ItemDetalhe item : itens) {
                soma += item.subtotal();
            }
            return Compra.arredondar(soma);
        }

        public double desconto() {
            return Compra.arredondar(Math.max(0, subtotal() - valorFinal));
        }
    }

    private final RepositorioCompra repositorio = new RepositorioCompra();

    // Compras do comprador na ordem do arquivo; a chave do mapa é o ID da compra
    public Map<Integer, DetalheCompra> comprasDoComprador(String nomeComprador) throws ErroPersistenciaException {
        Map<Integer, DetalheCompra> compras = new LinkedHashMap<>();

        Integer id = null;
        LocalDateTime horario = null;
        String pagamento = null;
        Double valorFinal = null;
        List<ItemDetalhe> itens = new ArrayList<>();
        int numeroLinha = 0;

        for (String linha : repositorio.carregarHistorico(nomeComprador)) {
            numeroLinha++;
            String texto = linha.trim();
            try {
                if (texto.startsWith("ID = ")) {
                    id = Integer.valueOf(texto.substring("ID = ".length()).trim());
                    horario = null;
                    pagamento = null;
                    valorFinal = null;
                    itens = new ArrayList<>();
                } else if (texto.startsWith("DATA_E_HORA = ")) {
                    horario = LocalDateTime.parse(texto.substring("DATA_E_HORA = ".length()).trim());
                } else if (texto.startsWith("PAGAMENTO = ")) {
                    pagamento = texto.substring("PAGAMENTO = ".length()).trim();
                } else if (texto.startsWith("VALOR_FINAL = ")) {
                    valorFinal = Double.valueOf(texto.substring("VALOR_FINAL = ".length()).trim());
                } else if (texto.startsWith("ITEM:")) {
                    Matcher m = LINHA_ITEM.matcher(texto);
                    if (!m.matches()) {
                        throw new NumberFormatException("item fora do formato esperado");
                    }
                    itens.add(new ItemDetalhe(Integer.parseInt(m.group(1)), m.group(2),
                            Integer.parseInt(m.group(3)), Double.parseDouble(m.group(4))));
                } else if (texto.equals("FIM_COMPRA")) {
                    if (id != null && horario != null && pagamento != null && valorFinal != null) {
                        compras.put(id, new DetalheCompra(id, horario, pagamento, valorFinal, List.copyOf(itens)));
                    }
                    id = null;
                }
            } catch (NumberFormatException | DateTimeParseException e) {
                throw new ErroPersistenciaException(
                        "Histórico inválido na linha " + numeroLinha + ": " + e.getMessage(), e);
            }
        }
        return compras;
    }
}
