import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class AnalisadorSintatico {

    private static final List<String> VALORES_FORMATO = List.of("PDF", "DOCX", "TXT", "HTML", "PPTX");
    private static final List<String> VALORES_OBJETO = List.of("documento", "arquivo", "imagem", "vídeo");

    private List<Map<String, Object>> tabelaDeSimbolos = new ArrayList<>();
    private List<Token> tokens;
    private int pos;
    private EstadoInteracao estadoInteracao = null;

    public void analisar(Queue<Token> filaTokens) {
        this.tokens = new ArrayList<>(filaTokens);
        this.pos = 0;

        if (this.tokens.isEmpty())
            return;
        if (this.estadoInteracao != null) {
            parseEProcessaResposta();
            return;
        }

        ParseResult resultado = parseInteracao();
        if (resultado.isSucesso()) {
            if (resultado.isCompleto()) {
                adicionarSimbolo(resultado.getRegraNome(), resultado.getValores());
                System.out.println("Pergunta '" + resultado.getRegraNome() + "' reconhecida com sucesso.");
            } else {
                this.estadoInteracao = new EstadoInteracao(resultado.getRegraNome(), resultado.getElementoFaltando());
                System.out.println("Qual " + resultado.getElementoFaltando() + " você deseja?");
            }
        } else {
            System.out.println("Erro Sintático: Não entendi.");
        }
    }

    private void parseEProcessaResposta() {
        ParseResult resultadoResposta = parseResposta();
        if (resultadoResposta.isSucesso()) {
            System.out.println("Resposta recebida e processada.");
            Map<String, Object> valoresCompletos = new HashMap<>();
            valoresCompletos.put(estadoInteracao.getElementoFaltando(),
                    resultadoResposta.getValores().values().iterator().next());
            adicionarSimbolo(estadoInteracao.getRegraIncompleta(), valoresCompletos);
            this.estadoInteracao = null;
        } else {
            System.out.println("Erro: Resposta cancelada. Pergunte novamente.");
            this.estadoInteracao = null;
        }
    }

    private ParseResult parseInteracao() {
        return parsePergunta();
    }

    // Roteador de perguntas.
    private ParseResult parsePergunta() {
        int savePos = pos;
        ParseResult resultado;

        resultado = parsePerguntaQualFormato();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parsePerguntaMostrePorTamanho();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parsePerguntaPorAutor();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parsePerguntaExisteTitulo();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parsePerguntaMostreCriadosEmData();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parsePerguntaFiltrePorObjeto();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        return ParseResult.falha();
    }

    // Roteador de respostas.
    private ParseResult parseResposta() {
        int savePos = pos;
        ParseResult resultado;

        resultado = parseRespostaOFormatoE();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parseRespostaOTamanhoE();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parseRespostaAutorE();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parseRespostaOTituloE();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parseRespostaFoiCriadoEm();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        resultado = parseRespostaOObjetoE();
        if (resultado.isSucesso())
            return resultado;
        pos = savePos;

        return ParseResult.falha();
    }

    // --- MÉTODOS DE PARSING ---

    // <pergunta> ::= Qual documento está no formato <formato> ?
    private ParseResult parsePerguntaQualFormato() {
        if (!matchSimilar(2, "qual", "documento", "formato"))
            return ParseResult.falha();

        Token formatoToken = consume(TokenType.IDENTIFIER);

        if (!matchSimilar(2, "?")) {
            if (formatoToken != null)
                pos--;
            return ParseResult.falha();
        }

        if (formatoToken == null) {
            return ParseResult.sucessoIncompleto("pergunta_qual_formato", "formato", new HashMap<>());
        } else {
            boolean valorValido = VALORES_FORMATO.stream()
                    .anyMatch(v -> v.equalsIgnoreCase(formatoToken.getValor()));

            if (valorValido) {
                return ParseResult.sucessoCompleto("pergunta_qual_formato", Map.of("formato", formatoToken.getValor()));
            } else {
                System.out.println("Valor '" + formatoToken.getValor() + "' não é um formato válido.");
                return ParseResult.sucessoIncompleto("pergunta_qual_formato", "formato", new HashMap<>());
            }
        }
    }

    // <pergunta> ::= Mostre documentos com tamanho maior que <tamanho> ?
    private ParseResult parsePerguntaMostrePorTamanho() {
        if (!matchSimilar(2, "mostre", "documentos", "tamanho", "maior", "que")) {
            return ParseResult.falha();
        }

        int posAntesDoTamanho = pos;

        Token numeroToken = consume(TokenType.NUMBER);
        Token unidadeToken = null;
        if (numeroToken != null) {
            unidadeToken = consume(TokenType.IDENTIFIER);
        }

        if (!matchSimilar(2, "?")) {
            pos = posAntesDoTamanho;
            return ParseResult.falha();
        }

        if (numeroToken == null || unidadeToken == null) {
            return ParseResult.sucessoIncompleto("pergunta_mostre_por_tamanho", "tamanho", new HashMap<>());
        }

        String unidadeValor = unidadeToken.getValor();
        boolean unidadeValida = unidadeValor.equalsIgnoreCase("KB") || unidadeValor.equalsIgnoreCase("MB")
                || unidadeValor.equalsIgnoreCase("GB");

        if (unidadeValida) {
            String unidadePadronizada = unidadeValor.toUpperCase();
            Map<String, Object> tamanho = new LinkedHashMap<>();
            tamanho.put("numero", numeroToken.getValor());
            tamanho.put("unidade", unidadePadronizada);
            return ParseResult.sucessoCompleto("pergunta_mostre_por_tamanho", Map.of("tamanho", tamanho));
        } else {
            System.out.println("Valor '" + unidadeValor + "' não é uma unidade de tamanho válida (use KB, MB, ou GB).");
            return ParseResult.sucessoIncompleto("pergunta_mostre_por_tamanho", "tamanho", new HashMap<>());
        }
    }

    // <pergunta> ::= Mostre documentos do autor <autor> ?
    private ParseResult parsePerguntaPorAutor() {
        if (!matchSimilar(2, "mostre", "documentos", "autor"))
            return ParseResult.falha();
        String autor = parseTextoLivre("?");
        if (autor == null) {
            return ParseResult.sucessoIncompleto("pergunta_por_autor", "autor", new HashMap<>());
        }
        if (!matchSimilar(2, "?"))
            return ParseResult.falha();
        return ParseResult.sucessoCompleto("pergunta_por_autor", Map.of("autor", autor));
    }

    // <pergunta> ::= Existe documento com o titulo <titulo> ?
    private ParseResult parsePerguntaExisteTitulo() {
        if (!matchSimilar(2, "existe", "documento", "titulo"))
            return ParseResult.falha();
        String titulo = parseTextoLivre("?");
        if (titulo == null)
            return ParseResult.sucessoIncompleto("pergunta_existe_titulo", "titulo", new HashMap<>());
        if (!matchSimilar(2, "?"))
            return ParseResult.falha();
        return ParseResult.sucessoCompleto("pergunta_existe_titulo", Map.of("titulo", titulo));
    }

    // <pergunta> ::= Mostre documentos criados em <data> ?
    private ParseResult parsePerguntaMostreCriadosEmData() {
        if (!matchSimilar(2, "mostre", "documentos", "criados"))
            return ParseResult.falha();
        String data = parseData();
        if (!matchSimilar(2, "?"))
            return ParseResult.falha();
        if (data == null)
            return ParseResult.sucessoIncompleto("pergunta_mostre_criados_em_data", "data", new HashMap<>());
        return ParseResult.sucessoCompleto("pergunta_mostre_criados_em_data", Map.of("data", data));
    }

    // <pergunta> ::= Filtre objetos por <objeto> ?
    private ParseResult parsePerguntaFiltrePorObjeto() {
        if (!matchSimilar(2, "filtre", "objetos"))
            return ParseResult.falha();
        Token objetoToken = consume(TokenType.IDENTIFIER);
        if (!matchSimilar(2, "?")) {
            if (objetoToken != null)
                pos--;
            return ParseResult.falha();
        }
        if (objetoToken == null) {
            return ParseResult.sucessoIncompleto("pergunta_filtre_por_objeto", "objeto", new HashMap<>());
        }
        if (VALORES_OBJETO.stream().anyMatch(v -> v.equalsIgnoreCase(objetoToken.getValor()))) {
            return ParseResult.sucessoCompleto("pergunta_filtre_por_objeto", Map.of("objeto", objetoToken.getValor()));
        }
        System.out.println("Valor '" + objetoToken.getValor() + "' não é um objeto válido.");
        return ParseResult.sucessoIncompleto("pergunta_filtre_por_objeto", "objeto", new HashMap<>());
    }

    // <resposta> ::= O formato é <formato> .
    private ParseResult parseRespostaOFormatoE() {
        if (!matchSimilar(2, "formato"))
            return ParseResult.falha();
        Token formatoToken = consume(TokenType.IDENTIFIER);
        if (formatoToken == null || !matchSimilar(2, "."))
            return ParseResult.falha();
        if (VALORES_FORMATO.stream().anyMatch(v -> v.equalsIgnoreCase(formatoToken.getValor()))) {
            return ParseResult.sucessoCompleto("resposta_formato_e", Map.of("formato", formatoToken.getValor()));
        }
        pos--; // Devolve o token se o valor for inválido, para causar falha geral.
        return ParseResult.falha();
    }

    // <resposta> ::= O tamanho é <tamanho> .
    private ParseResult parseRespostaOTamanhoE() {
        if (!matchSimilar(2, "tamanho"))
            return ParseResult.falha();
        Map<String, Object> tamanho = parseTamanho();
        if (tamanho == null || !matchSimilar(2, "."))
            return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_tamanho_e", Map.of("tamanho", tamanho));
    }

    // <resposta> ::= O autor é <autor> .
    private ParseResult parseRespostaAutorE() {
        if (!matchSimilar(2, "autor")) {
            return ParseResult.falha();
        }
        String autor = parseTextoLivre(".");
        if (autor == null || !matchSimilar(2, ".")) {
            return ParseResult.falha();
        }
        return ParseResult.sucessoCompleto("resposta_autor_e", Map.of("autor", autor));
    }

    // <resposta> ::= O titulo é <titulo> .
    private ParseResult parseRespostaOTituloE() {
        if (!matchSimilar(2, "titulo"))
            return ParseResult.falha();
        String titulo = parseTextoLivre(".");
        if (titulo == null || !matchSimilar(2, "."))
            return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_titulo_e", Map.of("titulo", titulo));
    }

    // <resposta> ::= Foi criado em <data> .
    private ParseResult parseRespostaFoiCriadoEm() {
        if (!matchSimilar(2, "criado"))
            return ParseResult.falha();
        String data = parseData();
        if (data == null || !matchSimilar(2, "."))
            return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_criado_em", Map.of("data", data));
    }

    // <resposta> ::= O objeto é <objeto> .
    private ParseResult parseRespostaOObjetoE() {
        if (!matchSimilar(2, "objeto"))
            return ParseResult.falha();
        Token objetoToken = consume(TokenType.IDENTIFIER);
        if (objetoToken == null || !matchSimilar(2, "."))
            return ParseResult.falha();
        if (VALORES_OBJETO.stream().anyMatch(v -> v.equalsIgnoreCase(objetoToken.getValor()))) {
            return ParseResult.sucessoCompleto("resposta_objeto_e", Map.of("objeto", objetoToken.getValor()));
        }
        pos--;
        return ParseResult.falha();
    }

    // --- MÉTODOS AUXILIARES ---
    private Map<String, Object> parseTamanho() {
        Token numero = consume(TokenType.NUMBER);
        if (numero == null)
            return null;
        Token unidade = consume(TokenType.IDENTIFIER);
        if (unidade != null && (unidade.getValor().equalsIgnoreCase("KB") || unidade.getValor().equalsIgnoreCase("MB")
                || unidade.getValor().equalsIgnoreCase("GB"))) {
            String unidadePadronizada = unidade.getValor().toUpperCase();
            Map<String, Object> tamanhoMap = new LinkedHashMap<>();
            tamanhoMap.put("numero", numero.getValor());
            tamanhoMap.put("unidade", unidadePadronizada);
            return tamanhoMap;
        }
        pos--;
        return null;
    }

    private String parseData() {
        int savePos = pos;
        if (pos + 4 < tokens.size() &&
                tokens.get(pos).getTipo() == TokenType.NUMBER &&
                tokens.get(pos + 1).getValor().equals("/") &&
                tokens.get(pos + 2).getTipo() == TokenType.NUMBER &&
                tokens.get(pos + 3).getValor().equals("/") &&
                tokens.get(pos + 4).getTipo() == TokenType.NUMBER) {
            String data = tokens.get(pos).getValor() + "/" + tokens.get(pos + 2).getValor() + "/"
                    + tokens.get(pos + 4).getValor();
            pos += 5;
            return data;
        }
        pos = savePos;
        return null;
    }

    private String parseTextoLivre(String delimiter) {
        StringBuilder sb = new StringBuilder();
        while (pos < tokens.size() && !tokens.get(pos).getValor().equals(delimiter)) {
            sb.append(tokens.get(pos).getValor()).append(" ");
            pos++;
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    // --- MÉTODOS DE CONTROLE ---
    private Token consume(TokenType type) {
        if (pos < tokens.size() && tokens.get(pos).getTipo() == type) {
            return tokens.get(pos++);
        }
        return null;
    }

    private boolean matchSimilar(int distanciaMaxima, String... values) {
        int initialPos = pos;
        for (String expectedValue : values) {
            if (pos >= tokens.size()) {
                pos = initialPos;
                return false;
            }

            String actualValue = tokens.get(pos).getValor();
            int distancia = AnalisadorLexico.similaridadeStrings(expectedValue.toLowerCase(),
                    actualValue.toLowerCase());

            if (distancia > distanciaMaxima) {
                pos = initialPos;
                return false;
            }
            pos++;
        }
        return true;
    }

    private void adicionarSimbolo(String regra, Map<String, Object> valores) {
        Map<String, Object> simbolo = new HashMap<>();
        simbolo.put("regra_gramatical", regra);
        simbolo.put("valores_extraidos", valores);
        tabelaDeSimbolos.add(simbolo);
    }

    public void imprimirTabelaDeSimbolos() {
        System.out.println("\n----------------------------------------");
        System.out.println("      Tabela de Símbolos Sintáticos       ");
        System.out.println("----------------------------------------");

        if (tabelaDeSimbolos.isEmpty()) {
            System.out.println("(vazia)");
        } else {
            // Ordena a tabela de símbolos por ordem alfabética do valores
            tabelaDeSimbolos.sort(
                    Comparator
                            .comparing((Map<String, Object> m) -> m.get("regra_gramatical").toString(),
                                    String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(m -> m.get("valores_extraidos").toString(), String.CASE_INSENSITIVE_ORDER));
            int i = 1;
            for (Map<String, Object> simbolo : tabelaDeSimbolos) {
                System.out.println("Símbolo " + (i++) + ": " + simbolo);
            }
        }
        System.out.println("----------------------------------------");

    }

    public List<Map<String, Object>> getTabelaDeSimbolos() {
        return this.tabelaDeSimbolos;
    }
}