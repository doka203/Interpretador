public class Token {
    private int posicao;
    private String valor;
    private TokenType tipo;

    public Token(int position, String value, TokenType type) {
        this.posicao = position;
        this.valor = value;
        this.tipo = type;
    }

    public int getPosicao() {
        return posicao;
    }

    public String getValor() {
        return valor;
    }

    public TokenType getTipo() {
        return tipo;
    }
}