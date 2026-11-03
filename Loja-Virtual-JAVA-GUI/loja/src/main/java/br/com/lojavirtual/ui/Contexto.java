package br.com.lojavirtual.ui;

import br.com.lojavirtual.model.Carrinho;
import br.com.lojavirtual.service.ServicoCompra;
import br.com.lojavirtual.service.ServicoProdutos;

// O que as telas compartilham: os serviços, o navegador entre telas e a sessão do comprador
// (nome e carrinho). Cada controller recebe o Contexto pelo construtor.
public class Contexto {
    private final ServicoProdutos servicoProdutos;
    private final ServicoCompra servicoCompra;
    private Navegador navegador;

    private String nomeComprador;
    private Carrinho carrinho = new Carrinho();

    public Contexto(ServicoProdutos servicoProdutos, ServicoCompra servicoCompra) {
        this.servicoProdutos = servicoProdutos;
        this.servicoCompra = servicoCompra;
    }

    public ServicoProdutos getServicoProdutos() {
        return servicoProdutos;
    }

    public ServicoCompra getServicoCompra() {
        return servicoCompra;
    }

    public Navegador getNavegador() {
        return navegador;
    }

    public void setNavegador(Navegador navegador) {
        this.navegador = navegador;
    }

    public String getNomeComprador() {
        return nomeComprador;
    }

    public Carrinho getCarrinho() {
        return carrinho;
    }

    // Começa uma sessão de comprador com carrinho novo
    public void entrarComoComprador(String nome) {
        this.nomeComprador = nome;
        this.carrinho = new Carrinho();
    }

    // Encerra a sessão: o carrinho (que só existe em memória) é descartado
    public void sairDoComprador() {
        this.nomeComprador = null;
        this.carrinho = new Carrinho();
    }
}
