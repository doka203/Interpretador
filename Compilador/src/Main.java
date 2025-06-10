import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nEscolha o modo de entrada:");
            System.out.println("1 - Entrada padrão");
            System.out.println("2 - Digitar entrada");
            System.out.println("0 - Sair");

            System.out.print("Digite a opção (0/1/2): ");
            int opcao = scanner.nextInt();
            scanner.nextLine();
            System.out.println();

            if (opcao == 0) {
                System.out.println("Saindo do analisador léxico.");
                break;
            }

            String texto = "";
            if (opcao == 1) {
                texto = "O rato roeu a roupa do Rei de Roma. Três pratos de trigo para três tigres tristes.";
                        System.out.println(texto);
                        System.out.println();
            } else if (opcao == 2) {
                System.out.println("Digite a entrada: ");
                texto = scanner.nextLine();
                System.out.println();
            } else {
                System.out.println("Opção inválida.");
                continue;
            }

            AnalisadorLexico analisador = new AnalisadorLexico(texto);
            List<Token> tabelaDeSimbolos = analisador.analisar();
            Queue<Token> filaDeTokens = analisador.getFilaDeTokens();

            tabelaDeSimbolos.sort(Comparator.comparing(Token::getValor, String.CASE_INSENSITIVE_ORDER));

            System.out.println("\n--- Tabela de Símbolos ---");
            for (Token token : tabelaDeSimbolos) {
                System.out.print(token.getValor() + " ");
            }

            System.out.println("\n\n--- Fila de Tokens ---");
            for (Token token : filaDeTokens) {
                System.out.print(token.getValor() + " ");
            }

            System.out.println();
        }

        scanner.close();
    }
}