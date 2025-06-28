import java.util.Map;

public class ParseResult {
    private final boolean sucesso;
    private final boolean completo;
    private final String regraNome;
    private final Map<String, Object> valores;
    private final String elementoFaltando;

    private ParseResult(boolean sucesso, boolean completo, String regraNome, Map<String, Object> valores,
            String elementoFaltando) {
        this.sucesso = sucesso;
        this.completo = completo;
        this.regraNome = regraNome;
        this.valores = valores;
        this.elementoFaltando = elementoFaltando;
    }

    public static ParseResult falha() {
        return new ParseResult(false, false, null, null, null);
    }

    public static ParseResult sucessoCompleto(String regraNome, Map<String, Object> valores) {
        return new ParseResult(true, true, regraNome, valores, null);
    }

    public static ParseResult sucessoIncompleto(String regraNome, String elementoFaltando,
            Map<String, Object> valoresParciais) {
        return new ParseResult(true, false, regraNome, valoresParciais, elementoFaltando);
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public boolean isCompleto() {
        return completo;
    }

    public String getRegraNome() {
        return regraNome;
    }

    public Map<String, Object> getValores() {
        return valores;
    }

    public String getElementoFaltando() {
        return elementoFaltando;
    }
}