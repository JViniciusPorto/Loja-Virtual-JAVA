package excecoes.excecoes;

public class EstoqueInsuficienteException extends Exception {
    private final String nomeProduto;
    private final int disponivel;
    private final int solicitado;

    public EstoqueInsuficienteException(String nomeProduto, int disponivel, int solicitado) {
        super("Estoque insuficiente para \"" + nomeProduto + "\": disponível "
                + disponivel + ", solicitado " + solicitado + ".");
        this.nomeProduto = nomeProduto;
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    public String getNomeProduto() { return nomeProduto; }
    public int getDisponivel()     { return disponivel; }
    public int getSolicitado()     { return solicitado; }
}