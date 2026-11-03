# Loja Virtual (Java + JavaFX)

Sistema de compras de uma loja virtual com carrinho, formas de pagamento (crédito, débito e Pix),
controle de estoque e persistência em arquivos de texto. Interface gráfica em JavaFX.

## Como executar

- **VS Code:** abra `src/main/java/br/com/lojavirtual/Launcher.java` e clique em *Run*.
- **Maven:** `mvn javafx:run` (requer Maven instalado).

Execute sempre com a **raiz do projeto** como pasta de trabalho: é nela que fica a pasta `data/`.

## Estrutura

```
data/                                  arquivos de texto (criados automaticamente)
  produtos.txt                         produtos e estoque
  relatorio/compras.txt                relatório geral de compras
  historico/<comprador>/compras.txt    histórico de cada comprador
src/main/java/br/com/lojavirtual/
  App.java, Launcher.java              inicialização da interface
  model/                               entidades (Produto, Carrinho, Compra, pagamentos...)
  repository/                          leitura e gravação dos arquivos
  service/                             regras de negócio (ServicoProdutos, ServicoCompra)
  exception/                           exceções do sistema
  ui/                                  controllers e classes de apoio da interface
src/main/resources/
  fxml/                                telas (abrem no Scene Builder)
  css/estilo.css                       tema visual
```

## Fluxo da compra (ServicoCompra.finalizarCompra)

1. valida carrinho, comprador e estoque;
2. calcula subtotal, desconto e processa o pagamento (nada é gravado ainda);
3. baixa o estoque (`ServicoProdutos.baixarEstoque`, tudo ou nada);
4. grava relatório e histórico; se falhar, desfaz a gravação e devolve o estoque.
