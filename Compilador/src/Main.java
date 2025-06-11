import java.util.Queue;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AnalisadorSintatico analisadorSintatico = new AnalisadorSintatico();

        while (true) {
            System.out.println("\n------------------------------------");
            System.out.print("Digite um comando (ou 'sair'): ");
            String texto = scanner.nextLine();

            if (texto.equalsIgnoreCase("sair")) {
                System.out.println("Saindo do programa.");
                break;
            }

            if (texto.equalsIgnoreCase("tabela")) {
                analisadorSintatico.imprimirTabelaDeSimbolos();
                continue;
            }

            AnalisadorLexico analisadorLexico = new AnalisadorLexico(texto);
            analisadorLexico.analisar();
            Queue<Token> filaDeTokens = analisadorLexico.getFilaDeTokens();

            System.out.println("Fila de Tokens gerada: "
                    + filaDeTokens.stream().map(Token::getValor).collect(Collectors.joining(" ")));

            analisadorSintatico.analisar(filaDeTokens);
        }

        analisadorSintatico.imprimirTabelaDeSimbolos();
        scanner.close();
    }
}