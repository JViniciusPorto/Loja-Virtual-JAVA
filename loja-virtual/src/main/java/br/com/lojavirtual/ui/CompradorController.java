package br.com.lojavirtual.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import br.com.lojavirtual.exception.ErroPersistenciaException;
import br.com.lojavirtual.exception.EstoqueInsuficienteException;
import br.com.lojavirtual.exception.ProdutoNaoEncontradoException;
import br.com.lojavirtual.exception.QuantidadeInvalidaException;
import br.com.lojavirtual.model.Carrinho;
import br.com.lojavirtual.model.Compra;
import br.com.lojavirtual.model.ItemCarrinho;
import br.com.lojavirtual.model.Produto;
import br.com.lojavirtual.model.ResumoCompra;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

// Tela do comprador: barra superior (pesquisa, carrinho, usuário), menu lateral e três páginas
// (início com produtos, carrinho e histórico de compras).
// As regras (estoque, quantidade, pagamento, gravação) continuam no Carrinho e nos serviços:
// este controller só chama esses métodos e mostra o resultado ou a mensagem de erro.
public class CompradorController {
    private static final double LARGURA_MENU = 224;
    private static final double LARGURA_MENU_RECOLHIDO = 72;
    private static final double LARGURA_TELA_PEQUENA = 1000;
    private static final double LARGURA_MINIMA_CARTAO = 210;

    private enum Pagina {
        INICIO, CARRINHO, HISTORICO
    }

    // Os produtos não têm categoria no cadastro, então os filtros usam só dados que existem
    // de verdade: o preço e o estoque de cada produto.
    private enum Filtro {
        TODOS("Todos", "grade", p -> true),
        ATE_200("Até R$ 200", "etiqueta", p -> p.getPreco() <= 200),
        DE_200_A_1000("R$ 200 a R$ 1.000", "etiqueta", p -> p.getPreco() > 200 && p.getPreco() <= 1000),
        ACIMA_1000("Acima de R$ 1.000", "etiqueta", p -> p.getPreco() > 1000),
        POUCAS_UNIDADES("Poucas unidades", "alerta", p -> p.getEstoque() <= Visual.ESTOQUE_BAIXO);

        private final String rotulo;
        private final String icone;
        private final Predicate<Produto> teste;

        Filtro(String rotulo, String icone, Predicate<Produto> teste) {
            this.rotulo = rotulo;
            this.icone = icone;
            this.teste = teste;
        }
    }

    private final Contexto contexto;
    private final LeitorCompras leitorCompras = new LeitorCompras();
    private final Map<Filtro, ToggleButton> chips = new EnumMap<>(Filtro.class);
    private List<Produto> catalogo = List.of();
    private Filtro filtroAtual = Filtro.TODOS;
    private Pagina paginaAtual = Pagina.INICIO;
    private boolean telaPequena;

    // barra superior
    @FXML
    private HBox barraTopo;
    @FXML
    private Button botaoMenu;
    @FXML
    private HBox caixaBusca;
    @FXML
    private TextField campoBusca;
    @FXML
    private Button botaoLimparBusca;
    @FXML
    private Button botaoCarrinhoTopo;
    @FXML
    private Label labelBadge;
    @FXML
    private HBox chipUsuario;
    @FXML
    private Label labelBoasVindas;
    @FXML
    private Button botaoSairTopo;

    // menu lateral
    @FXML
    private VBox menuLateral;
    @FXML
    private Label rotuloMenu;
    @FXML
    private Button btnInicio;
    @FXML
    private Button btnCarrinho;
    @FXML
    private Button btnHistorico;
    @FXML
    private Button btnSair;

    // área central
    @FXML
    private StackPane areaCentral;

    // página inicial
    @FXML
    private ScrollPane paginaInicio;
    @FXML
    private StackPane banner;
    @FXML
    private FlowPane filtros;
    @FXML
    private VBox secaoProdutos;
    @FXML
    private Label labelContagem;
    @FXML
    private FlowPane gradeProdutos;
    @FXML
    private VBox vazioProdutos;

    // carrinho
    @FXML
    private VBox paginaCarrinho;
    @FXML
    private Label labelResumoItens;
    @FXML
    private Button botaoLimpar;
    @FXML
    private VBox listaCarrinho;
    @FXML
    private Label labelQtdItens;
    @FXML
    private Label labelSubtotal;
    @FXML
    private Button botaoFinalizar;

    // histórico
    @FXML
    private VBox paginaHistorico;
    @FXML
    private Label labelQtdCompras;
    @FXML
    private Label labelTotalGasto;
    @FXML
    private VBox listaHistorico;

    public CompradorController(Contexto contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        labelBoasVindas.setText("Olá, " + contexto.getNomeComprador());

        configurarMenu();
        configurarBusca();
        configurarGrade();
        configurarFiltros();
        Animacoes.recortarArredondado(banner, 20);

        // a barra superior acompanha a largura da janela: em janelas estreitas o menu recolhe sozinho
        barraTopo.widthProperty().addListener((obs, antigo, novo) -> ajustarAoTamanho(novo.doubleValue()));

        atualizarCatalogo();
        atualizarCarrinho();
        mostrarPagina(Pagina.INICIO);
    }

    // ===================== Configuração dos componentes =====================

    private void configurarMenu() {
        // largura mínima e máxima seguem a preferida, assim a animação só mexe em um valor
        menuLateral.setPrefWidth(LARGURA_MENU);
        menuLateral.minWidthProperty().bind(menuLateral.prefWidthProperty());
        menuLateral.maxWidthProperty().bind(menuLateral.prefWidthProperty());
        Tooltip.install(botaoMenu, new Tooltip("Recolher ou expandir o menu"));
        Tooltip.install(botaoCarrinhoTopo, new Tooltip("Abrir o carrinho"));
    }

    private void configurarBusca() {
        campoBusca.focusedProperty().addListener((obs, antigo, focado) -> {
            caixaBusca.getStyleClass().remove("foco");
            if (focado) {
                caixaBusca.getStyleClass().add("foco");
            }
        });
        campoBusca.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            botaoLimparBusca.setVisible(temTexto);
            botaoLimparBusca.setManaged(temTexto);
            if (temTexto && paginaAtual != Pagina.INICIO) {
                mostrarPagina(Pagina.INICIO);
            }
            renderizarProdutos();
        });
    }

    private void configurarGrade() {
        // os cartões dividem a largura disponível: mais colunas em janelas largas, menos nas estreitas
        gradeProdutos.widthProperty().addListener((obs, antigo, novo) -> ajustarLarguraCartoes());
    }

    private void configurarFiltros() {
        ToggleGroup grupo = new ToggleGroup();
        for (Filtro filtro : Filtro.values()) {
            ToggleButton chip = new ToggleButton(filtro.rotulo);
            chip.setGraphic(Icones.criar(filtro.icone, "icone-p"));
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(grupo);
            chip.setUserData(filtro);
            chips.put(filtro, chip);
            filtros.getChildren().add(chip);
        }
        grupo.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) { // sempre há um filtro escolhido
                if (antigo != null) {
                    antigo.setSelected(true);
                }
                return;
            }
            filtroAtual = (Filtro) novo.getUserData();
            renderizarProdutos();
        });
        chips.get(Filtro.TODOS).setSelected(true);
    }

    // ===================== Navegação e menu lateral =====================

    @FXML
    private void abrirInicio() {
        mostrarPagina(Pagina.INICIO);
    }

    @FXML
    private void abrirCarrinho() {
        mostrarPagina(Pagina.CARRINHO);
    }

    @FXML
    private void abrirHistorico() {
        mostrarPagina(Pagina.HISTORICO);
    }

    private void mostrarPagina(Pagina pagina) {
        paginaAtual = pagina;
        definirVisivel(paginaInicio, pagina == Pagina.INICIO);
        definirVisivel(paginaCarrinho, pagina == Pagina.CARRINHO);
        definirVisivel(paginaHistorico, pagina == Pagina.HISTORICO);
        marcarAtivo(btnInicio, pagina == Pagina.INICIO);
        marcarAtivo(btnCarrinho, pagina == Pagina.CARRINHO);
        marcarAtivo(btnHistorico, pagina == Pagina.HISTORICO);

        // os dados são relidos ao entrar na página (o estoque pode ter mudado)
        switch (pagina) {
            case INICIO -> {
                atualizarCatalogo();
                Animacoes.aparecer(paginaInicio);
            }
            case CARRINHO -> {
                atualizarCarrinho();
                Animacoes.aparecer(paginaCarrinho);
            }
            case HISTORICO -> {
                atualizarHistorico();
                Animacoes.aparecer(paginaHistorico);
            }
        }
    }

    private void definirVisivel(Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
    }

    private void marcarAtivo(Button botao, boolean ativo) {
        botao.getStyleClass().remove("ativo");
        if (ativo) {
            botao.getStyleClass().add("ativo");
        }
    }

    @FXML
    private void alternarMenu() {
        definirMenuRecolhido(menuLateral.getPrefWidth() > LARGURA_MENU_RECOLHIDO + 1);
    }

    // Recolhido: só os ícones (com dica ao passar o mouse). Expandido: ícones e textos.
    private void definirMenuRecolhido(boolean recolher) {
        rotuloMenu.setVisible(!recolher);
        rotuloMenu.setManaged(!recolher);
        for (Button botao : List.of(btnInicio, btnCarrinho, btnHistorico, btnSair)) {
            botao.setContentDisplay(recolher ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            botao.setTooltip(recolher ? new Tooltip(botao.getText()) : null);
        }
        double alvo = recolher ? LARGURA_MENU_RECOLHIDO : LARGURA_MENU;
        new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(menuLateral.prefWidthProperty(), alvo, Interpolator.EASE_BOTH))).play();
    }

    // Janelas estreitas: menu recolhido, barra superior mais enxuta
    private void ajustarAoTamanho(double largura) {
        if (largura <= 0) {
            return;
        }
        boolean pequena = largura < LARGURA_TELA_PEQUENA;
        if (pequena == telaPequena) {
            return;
        }
        telaPequena = pequena;
        definirMenuRecolhido(pequena);
        chipUsuario.setVisible(!pequena);
        chipUsuario.setManaged(!pequena);
        botaoSairTopo.setContentDisplay(pequena ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
    }

    // ===================== Pesquisa e filtros =====================

    @FXML
    private void pesquisar() {
        if (paginaAtual != Pagina.INICIO) {
            mostrarPagina(Pagina.INICIO);
        } else {
            renderizarProdutos();
        }
        Animacoes.rolarPara(paginaInicio, secaoProdutos);
    }

    @FXML
    private void limparBusca() {
        campoBusca.clear();
        campoBusca.requestFocus();
    }

    @FXML
    private void limparFiltros() {
        campoBusca.clear();
        chips.get(Filtro.TODOS).setSelected(true);
    }

    @FXML
    private void irParaProdutos() {
        Animacoes.rolarPara(paginaInicio, secaoProdutos);
    }

    // ===================== Página inicial: produtos =====================

    private void atualizarCatalogo() {
        catalogo = new ArrayList<>(contexto.getServicoProdutos().listar());
        for (Filtro filtro : Filtro.values()) {
            long quantidade = catalogo.stream().filter(filtro.teste).count();
            chips.get(filtro).setText(filtro.rotulo + " (" + quantidade + ")");
        }
        renderizarProdutos();
    }

    private void renderizarProdutos() {
        String termo = Visual.normalizar(campoBusca.getText());
        List<Produto> visiveis = catalogo.stream()
                .filter(filtroAtual.teste)
                .filter(p -> termo.isEmpty() || Visual.normalizar(p.getNome()).contains(termo)
                        || String.valueOf(p.getId()).equals(termo))
                .toList();

        List<Node> cartoes = new ArrayList<>();
        for (Produto produto : visiveis) {
            cartoes.add(criarCartaoProduto(produto));
        }
        gradeProdutos.getChildren().setAll(cartoes);
        ajustarLarguraCartoes();

        boolean filtrado = visiveis.size() != catalogo.size();
        labelContagem.setText(filtrado ? visiveis.size() + " de " + catalogo.size() + " produtos"
                : catalogo.size() + (catalogo.size() == 1 ? " produto" : " produtos"));
        definirVisivel(vazioProdutos, visiveis.isEmpty());
        definirVisivel(gradeProdutos, !visiveis.isEmpty());
    }

    private void ajustarLarguraCartoes() {
        double total = gradeProdutos.getWidth() - gradeProdutos.getPadding().getLeft()
                - gradeProdutos.getPadding().getRight();
        if (total <= 0) {
            return;
        }
        double espaco = gradeProdutos.getHgap();
        int colunas = Math.max(1, (int) Math.floor((total + espaco) / (LARGURA_MINIMA_CARTAO + espaco)));
        double largura = Math.floor((total - espaco * (colunas - 1)) / colunas);
        for (Node no : gradeProdutos.getChildren()) {
            if (no instanceof Region cartao) {
                cartao.setMinWidth(largura);
                cartao.setPrefWidth(largura);
                cartao.setMaxWidth(largura);
            }
        }
    }

    private VBox criarCartaoProduto(Produto produto) {
        StackPane imagem = Miniaturas.criar(produto, 140);
        imagem.setCursor(Cursor.HAND);
        imagem.setOnMouseClicked(e -> abrirDetalhes(produto));

        Label nome = new Label(produto.getNome());
        nome.getStyleClass().add("produto-nome");
        nome.setWrapText(true);
        nome.setAlignment(Pos.TOP_LEFT);
        nome.setMinHeight(44);
        nome.setPrefHeight(44);
        nome.setMaxHeight(44);
        nome.setMaxWidth(Double.MAX_VALUE);
        nome.setCursor(Cursor.HAND);
        nome.setOnMouseClicked(e -> abrirDetalhes(produto));

        Label preco = new Label(Formatos.dinheiro(produto.getPreco()));
        preco.getStyleClass().add("produto-preco");

        Label estoque = new Label();
        estoque.getStyleClass().add("produto-estoque");
        if (produto.getEstoque() <= 0) {
            estoque.setText("Esgotado");
            estoque.getStyleClass().add("estoque-zerado");
        } else if (produto.getEstoque() <= Visual.ESTOQUE_BAIXO) {
            estoque.setText("Últimas " + produto.getEstoque() + " unidades");
            estoque.getStyleClass().add("estoque-baixo");
        } else {
            estoque.setText(produto.getEstoque() + " em estoque");
        }

        Button adicionar = new Button(produto.getEstoque() <= 0 ? "Esgotado" : "Adicionar");
        adicionar.setGraphic(Icones.criar("carrinho", "icone-p"));
        adicionar.getStyleClass().addAll("botao-primario", "botao-compacto");
        adicionar.setMaxWidth(Double.MAX_VALUE);
        adicionar.setDisable(produto.getEstoque() <= 0);
        adicionar.setOnAction(e -> adicionarRapido(produto, adicionar));
        HBox.setHgrow(adicionar, Priority.ALWAYS);

        Button detalhes = new Button();
        detalhes.setGraphic(Icones.criar("olho"));
        detalhes.getStyleClass().add("botao-icone");
        detalhes.setTooltip(new Tooltip("Ver detalhes"));
        detalhes.setOnAction(e -> abrirDetalhes(produto));

        HBox acoes = new HBox(8, adicionar, detalhes);
        acoes.setAlignment(Pos.CENTER);

        VBox cartao = new VBox(10, imagem, nome, preco, estoque, acoes);
        cartao.getStyleClass().add("produto-card");
        cartao.setPadding(new Insets(14));
        Animacoes.elevarAoPassarMouse(cartao, 4);
        return cartao;
    }

    private void abrirDetalhes(Produto produto) {
        DetalheProduto.mostrar(areaCentral, contexto, produto, (item, quantidade) -> {
            atualizarIndicadorCarrinho();
            Toast.sucesso(areaCentral, quantidade + " × " + item.getNome() + " adicionado ao carrinho.");
        });
    }

    // Botão "Adicionar" do cartão: coloca 1 unidade no carrinho (a regra de estoque é do Carrinho)
    private void adicionarRapido(Produto produto, Button botao) {
        try {
            contexto.getCarrinho().adicionar(produto, 1);
            atualizarIndicadorCarrinho();
            Toast.sucesso(areaCentral, produto.getNome() + " adicionado ao carrinho.");
            confirmarNoBotao(botao);
        } catch (QuantidadeInvalidaException | EstoqueInsuficienteException e) {
            Toast.erro(areaCentral, e.getMessage());
        }
    }

    // O botão vira "Adicionado" por um instante
    private void confirmarNoBotao(Button botao) {
        if (botao.getStyleClass().contains("botao-sucesso")) {
            return;
        }
        String textoOriginal = botao.getText();
        Node graficoOriginal = botao.getGraphic();
        botao.setText("Adicionado");
        botao.setGraphic(Icones.criar("check", "icone-p"));
        botao.getStyleClass().add("botao-sucesso");
        PauseTransition pausa = new PauseTransition(Duration.millis(1100));
        pausa.setOnFinished(e -> {
            botao.setText(textoOriginal);
            botao.setGraphic(graficoOriginal);
            botao.getStyleClass().remove("botao-sucesso");
        });
        pausa.play();
    }

    // ===================== Carrinho =====================

    private void atualizarCarrinho() {
        Carrinho carrinho = contexto.getCarrinho();
        List<ItemCarrinho> itens = carrinho.getItens();

        listaCarrinho.getChildren().clear();
        if (itens.isEmpty()) {
            listaCarrinho.getChildren().add(criarCarrinhoVazio());
        } else {
            for (ItemCarrinho item : itens) {
                listaCarrinho.getChildren().add(criarLinhaCarrinho(item));
            }
        }

        int unidades = totalDeUnidades();
        labelResumoItens.setText(itens.isEmpty() ? "Nenhum produto adicionado ainda"
                : itens.size() + (itens.size() == 1 ? " produto" : " produtos") + " no carrinho");
        labelQtdItens.setText(unidades + (unidades == 1 ? " unidade" : " unidades"));
        labelSubtotal.setText(Formatos.dinheiro(carrinho.calcularSubtotal()));
        botaoFinalizar.setDisable(carrinho.estaVazio());
        botaoLimpar.setDisable(carrinho.estaVazio());
        atualizarIndicadorCarrinho();
    }

    private int totalDeUnidades() {
        return contexto.getCarrinho().getItens().stream().mapToInt(ItemCarrinho::getQuantidade).sum();
    }

    // Contador (selo laranja) do ícone do carrinho na barra superior
    private void atualizarIndicadorCarrinho() {
        int unidades = totalDeUnidades();
        String anterior = labelBadge.getText();
        String novo = unidades > 99 ? "99+" : String.valueOf(unidades);
        labelBadge.setText(novo);
        labelBadge.setVisible(unidades > 0);
        if (unidades > 0 && !novo.equals(anterior)) {
            Animacoes.pulsar(labelBadge);
        }
    }

    private Node criarLinhaCarrinho(ItemCarrinho item) {
        Produto produto = item.getProduto();

        StackPane imagem = Miniaturas.criar(produto, 80);
        imagem.setMinWidth(80);
        imagem.setPrefWidth(80);
        imagem.setMaxWidth(80);

        Label nome = new Label(produto.getNome());
        nome.getStyleClass().add("item-nome");
        nome.setMaxWidth(Double.MAX_VALUE);
        Label unitario = new Label(Formatos.dinheiro(produto.getPreco()) + " cada");
        unitario.getStyleClass().add("texto-suave");
        Label disponivel = new Label("Disponível: " + produto.getEstoque());
        disponivel.getStyleClass().add("texto-pequeno");
        VBox informacoes = new VBox(3, nome, unitario, disponivel);
        informacoes.setAlignment(Pos.CENTER_LEFT);
        informacoes.setMinWidth(60);
        HBox.setHgrow(informacoes, Priority.ALWAYS);

        Button menos = botaoStepper("menos");
        menos.setDisable(item.getQuantidade() <= 1);
        menos.setOnAction(e -> alterarQuantidade(item, item.getQuantidade() - 1));
        Button mais = botaoStepper("mais");
        mais.setDisable(item.getQuantidade() >= produto.getEstoque());
        mais.setOnAction(e -> alterarQuantidade(item, item.getQuantidade() + 1));
        Label quantidade = new Label(String.valueOf(item.getQuantidade()));
        quantidade.getStyleClass().add("stepper-qtd");
        HBox stepper = new HBox(4, menos, quantidade, mais);
        stepper.getStyleClass().add("stepper");
        stepper.setAlignment(Pos.CENTER);
        stepper.setMinWidth(Region.USE_PREF_SIZE);

        Label subtotal = new Label(Formatos.dinheiro(item.getSubtotal()));
        subtotal.getStyleClass().add("item-subtotal");
        subtotal.setMinWidth(Region.USE_PREF_SIZE);
        subtotal.setAlignment(Pos.CENTER_RIGHT);

        Button remover = new Button();
        remover.setGraphic(Icones.criar("lixeira"));
        remover.getStyleClass().addAll("botao-icone", "botao-icone-perigo");
        remover.setTooltip(new Tooltip("Remover do carrinho"));
        remover.setOnAction(e -> removerItem(item));

        HBox linha = new HBox(14, imagem, informacoes, stepper, subtotal, remover);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(12));
        linha.getStyleClass().add("item-lista");
        return linha;
    }

    private Button botaoStepper(String icone) {
        Button botao = new Button();
        botao.setGraphic(Icones.criar(icone, "icone-p"));
        botao.getStyleClass().add("stepper-btn");
        return botao;
    }

    private Node criarCarrinhoVazio() {
        Button explorar = new Button("Explorar produtos");
        explorar.getStyleClass().add("botao-primario");
        explorar.setOnAction(e -> mostrarPagina(Pagina.INICIO));

        VBox vazio = new VBox(10, Icones.criar("carrinho", "icone-xg", "icone-claro"),
                rotulo("Seu carrinho está vazio", "subtitulo"),
                rotulo("Adicione produtos para vê-los aqui.", "texto-suave"), explorar);
        vazio.setAlignment(Pos.CENTER);
        vazio.getStyleClass().add("vazio");
        vazio.setMaxWidth(Double.MAX_VALUE);
        return vazio;
    }

    private void alterarQuantidade(ItemCarrinho item, int novaQuantidade) {
        try {
            contexto.getCarrinho().alterarQuantidade(item.getProduto().getId(), novaQuantidade);
            atualizarCarrinho();
        } catch (ProdutoNaoEncontradoException | QuantidadeInvalidaException | EstoqueInsuficienteException e) {
            Toast.erro(areaCentral, e.getMessage());
        }
    }

    private void removerItem(ItemCarrinho item) {
        String nome = item.getProduto().getNome();
        try {
            contexto.getCarrinho().remover(item.getProduto().getId());
            atualizarCarrinho();
            Toast.sucesso(areaCentral, nome + " removido do carrinho.");
        } catch (ProdutoNaoEncontradoException e) {
            Alertas.erro(areaCentral, "Não foi possível remover", e.getMessage());
        }
    }

    @FXML
    private void limparCarrinho() {
        if (contexto.getCarrinho().estaVazio()) {
            return;
        }
        if (Alertas.confirmar(areaCentral, "Limpar carrinho", "Remover todos os itens do carrinho?",
                "Limpar carrinho", true)) {
            contexto.getCarrinho().limpar();
            atualizarCarrinho();
        }
    }

    @FXML
    private void finalizarCompra() {
        if (contexto.getCarrinho().estaVazio()) {
            Alertas.erro(areaCentral, "Carrinho vazio", "Adicione produtos ao carrinho antes de finalizar.");
            return;
        }
        try {
            Compra compra = abrirPagamento();
            if (compra != null) {
                ConfirmacaoCompra.mostrar(areaCentral, compra);
                atualizarCatalogo();
                atualizarCarrinho();
                mostrarPagina(Pagina.HISTORICO);
            }
        } catch (IOException e) {
            Alertas.erro(areaCentral, "Erro ao abrir o pagamento",
                    "Não foi possível abrir a tela de pagamento: " + e.getMessage());
        }
    }

    // Abre a janela de pagamento e devolve a compra concluída (null se o usuário cancelou ou deu erro)
    private Compra abrirPagamento() throws IOException {
        FXMLLoader loader = contexto.getNavegador().novoLoader("pagamento.fxml");
        Region raiz = loader.load();
        PagamentoController controller = loader.getController();
        Stage dialogo = contexto.getNavegador().novoDialogo(raiz, "Pagamento");
        dialogo.showAndWait();
        return controller.getCompraConcluida();
    }

    // ===================== Histórico de compras =====================

    @FXML
    private void atualizarHistorico() {
        String nome = contexto.getNomeComprador();
        try {
            List<ResumoCompra> compras = new ArrayList<>(contexto.getServicoCompra().consultarHistorico(nome));
            Collections.reverse(compras); // mais recente primeiro
            Map<Integer, LeitorCompras.DetalheCompra> detalhes = leitorCompras.comprasDoComprador(nome);

            listaHistorico.getChildren().clear();
            if (compras.isEmpty()) {
                listaHistorico.getChildren().add(criarHistoricoVazio());
            } else {
                for (ResumoCompra compra : compras) {
                    listaHistorico.getChildren().add(criarLinhaHistorico(compra, detalhes.get(compra.idCompra())));
                }
            }
            labelQtdCompras.setText(compras.isEmpty() ? "Nenhuma compra realizada ainda"
                    : compras.size() + (compras.size() == 1 ? " compra realizada" : " compras realizadas"));
            labelTotalGasto.setText(Formatos.dinheiro(contexto.getServicoCompra().calcularTotalGasto(nome)));
        } catch (ErroPersistenciaException e) {
            Alertas.erro(areaCentral, "Não foi possível ler o histórico", e.getMessage());
        }
    }

    private Node criarLinhaHistorico(ResumoCompra compra, LeitorCompras.DetalheCompra detalhe) {
        StackPane icone = new StackPane(Icones.criar("recibo", "icone-g"));
        icone.getStyleClass().add("circulo-icone");

        VBox titulos = new VBox(3, rotulo("Compra nº " + compra.idCompra(), "item-nome"),
                rotulo(Formatos.dataHora(compra.horarioCompra()), "texto-suave"));
        titulos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titulos, Priority.ALWAYS);

        Label pagamento = rotulo(Formatos.formaPagamento(compra.formaPagamento()), "pilula");
        Label valor = rotulo(Formatos.dinheiro(compra.valorFinal()), "item-subtotal");
        valor.setMinWidth(Region.USE_PREF_SIZE);

        HBox linha = new HBox(14, icone, titulos, pagamento, valor, Icones.criar("seta", "icone-claro"));
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(12, 14, 12, 12));
        linha.getStyleClass().add("item-lista");
        if (detalhe != null) { // clicar na compra abre os detalhes
            linha.getStyleClass().add("item-clicavel");
            linha.setCursor(Cursor.HAND);
            Tooltip.install(linha, new Tooltip("Ver detalhes da compra"));
            linha.setOnMouseClicked(e -> DetalhesCompra.mostrar(areaCentral, detalhe));
        }
        return linha;
    }

    private Node criarHistoricoVazio() {
        Button explorar = new Button("Ver produtos");
        explorar.getStyleClass().add("botao-primario");
        explorar.setOnAction(e -> mostrarPagina(Pagina.INICIO));

        VBox vazio = new VBox(10, Icones.criar("recibo", "icone-xg", "icone-claro"),
                rotulo("Você ainda não fez nenhuma compra", "subtitulo"),
                rotulo("As compras finalizadas aparecem aqui.", "texto-suave"), explorar);
        vazio.setAlignment(Pos.CENTER);
        vazio.getStyleClass().add("vazio");
        vazio.setMaxWidth(Double.MAX_VALUE);
        return vazio;
    }

    // ===================== Saída =====================

    @FXML
    private void sair() {
        if (!contexto.getCarrinho().estaVazio()
                && !Alertas.confirmar(areaCentral, "Sair da conta", "Seu carrinho será descartado. Deseja sair?",
                        "Sair", false)) {
            return;
        }
        contexto.sairDoComprador();
        contexto.getNavegador().mostrarInicio();
    }

    private Label rotulo(String texto, String classe) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add(classe);
        return rotulo;
    }
}
