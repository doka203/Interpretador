import java.util.Queue;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AnalisadorSintatico analisadorSintatico = new AnalisadorSintatico();

        while (true) {
            System.out.println("----------------------------------------");
            System.out.println("  0 - Sair");
            System.out.println("  1 - Exibir tabela de símbolos");
            System.out.println("  Digite uma pergunta");
            System.out.println("----------------------------------------");
            System.out.print("> ");

            String texto = scanner.nextLine();

            if (texto.equals("0") || texto.equalsIgnoreCase("sair")) {
                System.out.println("\nSaiu do Compilador.");
                break;
            } else if (texto.equals("1") || texto.equalsIgnoreCase("tabela")) {
                analisadorSintatico.imprimirTabelaDeSimbolos();
                continue;
            } else {
                AnalisadorLexico analisadorLexico = new AnalisadorLexico(texto);
                analisadorLexico.analisar();
                Queue<Token> filaDeTokens = analisadorLexico.getFilaDeTokens();

                System.out.println("\n--- Fila de Tokens ---");
                for (Token token : filaDeTokens) {
                    System.out.print("[" + token.getValor() + "] ");
                }
                System.out.println();
                System.out.println("--- Análise Sintática ---");
                analisadorSintatico.analisar(filaDeTokens);
                continue;
            }
        }

        analisadorSintatico.imprimirTabelaDeSimbolos();
        scanner.close();
    }
}