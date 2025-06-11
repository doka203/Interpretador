import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class AnalisadorSintatico {

    private List<Map<String, Object>> tabelaDeSimbolos = new ArrayList<>();
    private List<Token> tokens;
    private int pos;
    private EstadoInteracao estadoInteracao = null;

    public void analisar(Queue<Token> filaTokens) {
        this.tokens = new ArrayList<>(filaTokens);
        this.pos = 0;
        if (this.tokens.isEmpty()) return;

        if (this.estadoInteracao != null) {
            parseEProcessaResposta();
            return;
        }
        
        ParseResult resultado = parseInteracao();
        if (resultado.isSucesso()) {
            if (resultado.isCompleto()) {
                adicionarSimbolo(resultado.getRegraNome(), resultado.getValores());
                System.out.println("✅ Comando reconhecido com sucesso.");
            } else {
                this.estadoInteracao = new EstadoInteracao(resultado.getRegraNome(), resultado.getElementoFaltando());
                System.out.println("🤔 Entendi a estrutura do seu comando, mas falta uma informação.");
                System.out.println("❓ Qual " + resultado.getElementoFaltando() + " você deseja?");
            }
        } else {
            System.out.println("❌ Erro Sintático: O comando não corresponde a nenhuma regra da gramática.");
        }
    }
    
    private void parseEProcessaResposta() {
        ParseResult resultadoResposta = parseResposta();
        if (resultadoResposta.isSucesso()) {
            System.out.println("✅ Resposta recebida e processada.");
            Map<String, Object> valoresCompletos = new HashMap<>();
            valoresCompletos.put(estadoInteracao.getElementoFaltando(), resultadoResposta.getValores().values().iterator().next());
            adicionarSimbolo(estadoInteracao.getRegraIncompleta(), valoresCompletos);
            this.estadoInteracao = null;
        } else {
            System.out.println("❌ Erro: Não entendi a sua resposta. Ação cancelada.");
            this.estadoInteracao = null;
        }
    }

    private ParseResult parseInteracao() {
        ParseResult res = parsePergunta();
        return res.isSucesso() ? res : parseResposta();
    }
    
    private ParseResult parsePergunta() {
        int savePos = pos;
        ParseResult resultado;
        resultado = parsePerguntaQualFormato(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parsePerguntaMostreComposto(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parsePerguntaExisteTitulo(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parsePerguntaQuandoFoiCriado(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parsePerguntaMostreFiltro(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parsePerguntaMostrePalavras(); if (resultado.isSucesso()) return resultado; pos = savePos;
        return ParseResult.falha();
    }

    private ParseResult parseResposta() {
        int savePos = pos;
        ParseResult resultado;
        resultado = parseRespostaOFormatoE(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parseRespostaOTituloE(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parseRespostaFoiCriadoEm(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parseRespostaOTamanhoE(); if (resultado.isSucesso()) return resultado; pos = savePos;
        resultado = parseRespostaTenhoUm(); if (resultado.isSucesso()) return resultado; pos = savePos;
        return ParseResult.falha();
    }
    
    // --- MÉTODOS DE PARSING PARA CADA REGRA ---
    private ParseResult parsePerguntaQualFormato() {
        if (!match("qual", "documento", "formato")) return ParseResult.falha();
        Token formato = consume(TokenType.IDENTIFIER);
        if (!match("?")) return ParseResult.falha();
        if (formato == null) return ParseResult.sucessoIncompleto("pergunta_qual_formato", "formato", new HashMap<>());
        return ParseResult.sucessoCompleto("pergunta_qual_formato", Map.of("formato", formato.getValor()));
    }
    
    private ParseResult parsePerguntaMostreComposto() {
        if (!match("mostre", "documentos", "formato")) return ParseResult.falha();
        Token formatoToken = null;
        if (pos < tokens.size() && !tokens.get(pos).getValor().equalsIgnoreCase("tamanho")) {
            formatoToken = consume(TokenType.IDENTIFIER);
        }
        if (!match("tamanho", "maior", "que")) return ParseResult.falha();
        Map<String, Object> tamanhoMap = parseTamanho();
        if (!match("?")) return ParseResult.falha();

        boolean formatoPresente = formatoToken != null;
        boolean tamanhoPresente = tamanhoMap != null;
        Map<String, Object> valores = new HashMap<>();
        if (formatoPresente) valores.put("formato", formatoToken.getValor());
        if (tamanhoPresente) valores.put("tamanho", tamanhoMap);

        if (formatoPresente && tamanhoPresente) {
            return ParseResult.sucessoCompleto("pergunta_mostre_composto", valores);
        } else if (formatoPresente) {
            return ParseResult.sucessoIncompleto("pergunta_mostre_composto", "tamanho", valores);
        } else if (tamanhoPresente) {
            return ParseResult.sucessoIncompleto("pergunta_mostre_composto", "formato", valores);
        } else {
            return ParseResult.sucessoIncompleto("pergunta_mostre_composto", "formato", valores);
        }
    }
    
    private ParseResult parsePerguntaExisteTitulo() {
        if (!match("existe", "documento", "titulo")) return ParseResult.falha();
        String titulo = parseTextoLivre("?");
        if (!match("?")) return ParseResult.falha();
        if (titulo == null) return ParseResult.sucessoIncompleto("pergunta_existe_titulo", "titulo", new HashMap<>());
        return ParseResult.sucessoCompleto("pergunta_existe_titulo", Map.of("titulo", titulo));
    }
    
    private ParseResult parsePerguntaQuandoFoiCriado() {
        if (!match("quando", "criado")) return ParseResult.falha();
        Token objeto = consume(TokenType.IDENTIFIER);
        if (objeto == null || !match("titulo")) return ParseResult.falha();
        String titulo = parseTextoLivre("?");
        if (!match("?")) return ParseResult.falha();
        if (titulo == null) {
            return ParseResult.sucessoIncompleto("pergunta_quando_criado", "titulo", Map.of("objeto", objeto.getValor()));
        }
        return ParseResult.sucessoCompleto("pergunta_quando_criado", Map.of("objeto", objeto.getValor(), "titulo", titulo));
    }

    private ParseResult parsePerguntaMostreFiltro() {
        if (!match("mostre")) return ParseResult.falha();
        Token objeto = consume(TokenType.IDENTIFIER);
        if (objeto == null) return ParseResult.falha();
        ParseResult resultadoFiltro = parseFiltro();
        if (!resultadoFiltro.isSucesso()) return ParseResult.falha();
        if (!match("?")) return ParseResult.falha();

        if (resultadoFiltro.isCompleto()) {
            Map<String, Object> valores = new HashMap<>();
            valores.put("objeto", objeto.getValor());
            valores.put("filtro", resultadoFiltro.getValores());
            return ParseResult.sucessoCompleto("pergunta_mostre_filtro", valores);
        } else {
            return resultadoFiltro;
        }
    }
    
    private ParseResult parsePerguntaMostrePalavras() {
        if (!match("mostre")) return ParseResult.falha();
        Token objeto = consume(TokenType.IDENTIFIER);
        if (objeto == null || !match("palavras")) return ParseResult.falha();
        String listaPalavras = parseTextoLivre("?");
        if (listaPalavras == null || !match("?")) return ParseResult.falha();
        return ParseResult.sucessoCompleto("pergunta_mostre_palavras", Map.of("objeto", objeto.getValor(), "lista_palavras", listaPalavras));
    }
    
    private ParseResult parseRespostaOFormatoE() {
        if (!match("formato")) return ParseResult.falha();
        Token formato = consume(TokenType.IDENTIFIER);
        if (formato == null || !match(".")) return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_formato_e", Map.of("formato", formato.getValor()));
    }
    
    private ParseResult parseRespostaOTituloE() {
        if (!match("titulo")) return ParseResult.falha();
        String titulo = parseTextoLivre(".");
        if (titulo == null || !match(".")) return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_titulo_e", Map.of("titulo", titulo));
    }

    private ParseResult parseRespostaFoiCriadoEm() {
        if (!match("criado")) return ParseResult.falha();
        String data = parseData();
        if (data == null || !match(".")) return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_criado_em", Map.of("data", data));
    }

    private ParseResult parseRespostaOTamanhoE() {
        if (!match("tamanho")) return ParseResult.falha();
        Map<String, Object> tamanho = parseTamanho();
        if (tamanho == null || !match(".")) return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_tamanho_e", Map.of("tamanho", tamanho));
    }

    private ParseResult parseRespostaTenhoUm() {
        if (!match("tenho")) return ParseResult.falha();
        Token objeto = consume(TokenType.IDENTIFIER);
        if (objeto == null || !match("tamanho")) return ParseResult.falha();
        Map<String, Object> tamanho = parseTamanho();
        if (tamanho == null || !match(".")) return ParseResult.falha();
        return ParseResult.sucessoCompleto("resposta_tenho_um", Map.of("objeto", objeto.getValor(), "tamanho", tamanho));
    }

    // --- MÉTODOS AUXILIARES COMPLETOS ---

    private ParseResult parseFiltro() {
        int savePos = pos;
        if (match("tamanho")) {
            String operador = parseOperador();
            if (operador == null) return ParseResult.sucessoIncompleto("pergunta_mostre_filtro", "operador", new HashMap<>());
            Map<String, Object> tamanho = parseTamanho();
            if (tamanho == null) return ParseResult.sucessoIncompleto("pergunta_mostre_filtro", "tamanho", Map.of("operador", operador));
            return ParseResult.sucessoCompleto("filtro_tamanho", Map.of("tipo", "tamanho", "operador", operador, "valor", tamanho));
        }
        pos = savePos;
        if (match("data")) {
            String data = parseData();
            if (data == null) return ParseResult.sucessoIncompleto("pergunta_mostre_filtro", "data", new HashMap<>());
            return ParseResult.sucessoCompleto("filtro_data", Map.of("tipo", "data", "valor", data));
        }
        pos = savePos;
        return ParseResult.falha();
    }
    
    private String parseOperador() {
        if (pos + 1 < tokens.size()) {
            String op = tokens.get(pos).getValor() + " " + tokens.get(pos + 1).getValor();
            if (op.equalsIgnoreCase("maior que") || op.equalsIgnoreCase("menor que") ||
                op.equalsIgnoreCase("igual a") || op.equalsIgnoreCase("diferente de")) {
                pos += 2;
                return op;
            }
        }
        return null;
    }

    private Map<String, Object> parseTamanho() {
        Token numero = consume(TokenType.NUMBER);
        if (numero == null) return null;
        Token unidade = consume(TokenType.IDENTIFIER);
        if (unidade != null && (unidade.getValor().equalsIgnoreCase("MB") || unidade.getValor().equalsIgnoreCase("GB"))) {
            return Map.of("numero", numero.getValor(), "unidade", unidade.getValor());
        }
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
            
            String data = tokens.get(pos).getValor() + "/" + tokens.get(pos + 2).getValor() + "/" + tokens.get(pos + 4).getValor();
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
    
    private Token consume(TokenType type) {
        if (pos < tokens.size() && tokens.get(pos).getTipo() == type) {
            return tokens.get(pos++);
        }
        return null;
    }

    private boolean match(String... values) {
        int initialPos = pos;
        for (String value : values) {
            if (pos >= tokens.size() || !tokens.get(pos).getValor().equalsIgnoreCase(value)) {
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
        System.out.println("\n--- Tabela de Símbolos Sintáticos ---");
        if (tabelaDeSimbolos.isEmpty()) {
            System.out.println("(vazia)");
        } else {
            int i = 1;
            for (Map<String, Object> simbolo : tabelaDeSimbolos) {
                System.out.println("Símbolo " + (i++) + ": " + simbolo);
            }
        }
    }
}