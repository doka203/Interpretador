import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalisadorLexico {
    private String textoEntrada;
    private int posicaoAtual;
    private final Set<String> palavrasIgnoradas;
    private final Queue<Token> filaDeTokens = new LinkedList<>();

    public AnalisadorLexico(String textoEntrada) {
        this.textoEntrada = textoEntrada;
        this.posicaoAtual = 0;
        this.palavrasIgnoradas = new HashSet<>(Arrays.asList(new Stopwords().stopwords));
    }

    public List<Token> analisar() {
        List<Token> tabelaDeSimbolosLexicos = new ArrayList<>();
        while (posicaoAtual < textoEntrada.length()) {
            char caractereAtual = textoEntrada.charAt(posicaoAtual);
            if (Character.isWhitespace(caractereAtual)) {
                posicaoAtual++;
                continue;
            }
            Token token = proximoToken();
            if (token != null) {
                if (!palavrasIgnoradas.contains(token.getValor().toLowerCase())) {
                    filaDeTokens.add(token);
                    if (token.getTipo() == TokenType.IDENTIFIER) {
                        // Lógica para popular a tabela de símbolos do léxico (opcional)
                        if (tabelaDeSimbolosLexicos.stream()
                                .noneMatch(t -> t.getValor().equalsIgnoreCase(token.getValor()))) {
                            tabelaDeSimbolosLexicos.add(token);
                        }
                    }
                }
            }
        }
        return tabelaDeSimbolosLexicos;
    }

    private Token proximoToken() {
        if (posicaoAtual >= textoEntrada.length())
            return null;

        class PadraoToken {
            final Pattern padrao;
            final TokenType tipo;

            PadraoToken(String r, TokenType t) {
                this.padrao = Pattern.compile("^" + r, Pattern.CASE_INSENSITIVE);
                this.tipo = t;
            }
        }

        PadraoToken[] padroes = new PadraoToken[] {
                new PadraoToken("[a-zA-ZáÁàÀâÂãÃéÉêÊíÍóÓôÔõÕúÚüÜçÇ_][a-zA-Z0-9áÁàÀâÂãÃéÉêÊíÍóÓôÔõÕúÚüÜçÇ_]*",
                        TokenType.IDENTIFIER),
                new PadraoToken("\\d+", TokenType.NUMBER),
                new PadraoToken("[+\\-/*=<>!]", TokenType.OPERATOR),
                new PadraoToken("[.,;(){}\\?]", TokenType.SYMBOL),
        };

        String restante = textoEntrada.substring(posicaoAtual);
        for (PadraoToken p : padroes) {
            Matcher m = p.padrao.matcher(restante);
            if (m.find()) {
                String valor = m.group();
                posicaoAtual += valor.length();
                return new Token(posicaoAtual, valor, p.tipo);
            }
        }

        String caractereInvalido = restante.substring(0, 1);
        posicaoAtual++;
        return new Token(posicaoAtual, caractereInvalido, TokenType.INVALID);
    }

    public Queue<Token> getFilaDeTokens() {
        return filaDeTokens;
    }
}