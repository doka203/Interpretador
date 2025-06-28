public class EstadoInteracao {
    private String regraIncompleta;
    private String elementoFaltando;

    public EstadoInteracao(String regraIncompleta, String elementoFaltando) {
        this.regraIncompleta = regraIncompleta;
        this.elementoFaltando = elementoFaltando;
    }

    public String getRegraIncompleta() {
        return regraIncompleta;
    }

    public String getElementoFaltando() {
        return elementoFaltando;
    }
}