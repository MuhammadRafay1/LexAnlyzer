import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            SymbolTable symbolTable = new SymbolTable();
            ErrorHandler errorHandler = new ErrorHandler();
            Lexer lexer = new Lexer("test.sui", symbolTable, errorHandler);
            List<Token> tokens = lexer.tokenize();

            // Print tokens
            for (Token token : tokens) {
                System.out.println(token);
            }

            // Print errors
            if (errorHandler.hasErrors()) {
                System.out.println("\nErrors:");
                errorHandler.getErrors().forEach(System.out::println);
            }

            System.out.println("Symbol Table:");
                for (String name : symbolTable.table.keySet()) {
                    SymbolTable.Entry entry = symbolTable.getEntry(name);
                    System.out.println(name + " -> Type: " + entry.type + ", Scope: " + entry.scope);
                }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}