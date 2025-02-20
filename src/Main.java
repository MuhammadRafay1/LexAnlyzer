import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            SymbolTable symbolTable = new SymbolTable();
            ErrorHandler errorHandler = new ErrorHandler();
            Lexer lexer = new Lexer("/home/tabish/Desktop/Compiler Construction/LexAnlyzer/test.sui", symbolTable, errorHandler);
            
            System.out.println("DFA State Count: " + lexer.dfa.states.size());
            
            List<Token> tokens = lexer.tokenize();

            tokens.forEach(System.out::println);
            errorHandler.getErrors().forEach(System.out::println);
            symbolTable.table.forEach((k,v) -> 
                System.out.println(k + " -> " + v.type + " (" + v.scope + ")"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}