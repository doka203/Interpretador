import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
        List<Token> tabelaDeSimbolos = new ArrayList<>();

        while (posicaoAtual < textoEntrada.length()) {
            char caractereAtual = textoEntrada.charAt(posicaoAtual);

            if (Character.isWhitespace(caractereAtual)) {
                posicaoAtual++;
                continue;
            }

            Token token = proximoToken();
            if (token != null) {
                String valor = token.getValor().toLowerCase();

                if (!palavrasIgnoradas.contains(valor)) {
                    filaDeTokens.add(token);

                    if (token.getTipo() == TokenType.IDENTIFIER) {
                        boolean palavraDuplicada = false;

                        for (Token simbolo : tabelaDeSimbolos) {
                            int distancia = similaridadeStrings(
                                    simbolo.getValor().toLowerCase(), valor);
                            if (distancia <= 2) {
                                System.out.printf("Palavra possivelmente com erro (distância %d): %s ↔ %s%n",
                                        distancia, simbolo.getValor(), valor);
                                palavraDuplicada = true;
                                break;
                            }
                        }

                        if (!palavraDuplicada) {
                            tabelaDeSimbolos.add(token);
                        }
                    }
                }
            } else {
                char caractereInvalido = textoEntrada.charAt(posicaoAtual);
                throw new RuntimeException("Erro léxico: caractere inválido '" + caractereInvalido
                        + "' (código Unicode: U+" + String.format("%04X", (int) caractereInvalido) + ")");
            }
        }

        return tabelaDeSimbolos;
    }

    private Token proximoToken() {
        if (posicaoAtual >= textoEntrada.length()) {
            return null;
        }

        class PadraoToken {
            final Pattern padrao;
            final TokenType tipo;

            PadraoToken(String regex, TokenType tipo) {
                this.padrao = Pattern.compile("^" + regex);
                this.tipo = tipo;
            }
        }

        PadraoToken[] padroes = new PadraoToken[] {
                new PadraoToken("[a-zA-ZáÁàÀâÂãÃéÉêÊíÍóÓôÔõÕúÚüÜçÇ_][a-zA-Z0-9áÁàÀâÂãÃéÉêÊíÍóÓôÔõÕúÚüÜçÇ_]*",
                        TokenType.IDENTIFIER),
                new PadraoToken("\\d+", TokenType.NUMBER),
                new PadraoToken("[+\\-/*=<>!]", TokenType.OPERATOR),
                new PadraoToken("[.,;(){}]", TokenType.SYMBOL),
        };

        String restante = textoEntrada.substring(posicaoAtual);

        for (PadraoToken padraoToken : padroes) {
            Matcher matcher = padraoToken.padrao.matcher(restante);

            if (matcher.find()) {
                String valor = matcher.group();
                posicaoAtual += valor.length();

                return new Token(posicaoAtual, valor, padraoToken.tipo);
            }
        }

        String caractereInvalido = textoEntrada.substring(posicaoAtual, posicaoAtual + 1);
        posicaoAtual += 1;
        return new Token(posicaoAtual, caractereInvalido, TokenType.INVALID);
    }

    public static int similaridadeStrings(String string1, String string2) {
        int[][] matriz = new int[string1.length() + 1][string2.length() + 1];

        for (int i = 0; i <= string1.length(); i++) {
            for (int j = 0; j <= string2.length(); j++) {
                if (i == 0) {
                    matriz[i][j] = j;
                } else if (j == 0) {
                    matriz[i][j] = i;
                } else if (string1.charAt(i - 1) == string2.charAt(j - 1)) {
                    matriz[i][j] = matriz[i - 1][j - 1];
                } else {
                    matriz[i][j] = 1 + Math.min(
                            matriz[i - 1][j - 1],
                            Math.min(matriz[i - 1][j], matriz[i][j - 1]));
                }
            }
        }

        return matriz[string1.length()][string2.length()];
    }

    public Queue<Token> getFilaDeTokens() {
        return filaDeTokens;
    }
}
