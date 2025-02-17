import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private BufferedReader reader;
    private int currentLine = 1;
    private ErrorHandler errorHandler;
    private SymbolTable symbolTable;

    // Reserved keywords
    private static final String[] KEYWORDS = {
        "int", "dec", "bln", "char", "mrWorld", "mrArea" , "true", "false"
    };

    

    public Lexer(String filePath, SymbolTable symbolTable, ErrorHandler errorHandler) throws IOException {
        this.reader = new BufferedReader(new FileReader(filePath));
        this.errorHandler = errorHandler;
        this.symbolTable = symbolTable;
    }

    public List<Token> tokenize() throws IOException {
        List<Token> tokens = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            tokens.addAll(processLine(line));
            currentLine++;
        }
        reader.close();
        return tokens;
    }

    private List<Token> processLine(String line) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        while (pos < line.length()) {
            char ch = line.charAt(pos);
            if (Character.isWhitespace(ch)) {
                pos++; // Skip whitespace
            } else if (isCommentStart(line, pos)) {
                pos = processComment(line, pos, tokens);
            } else if (Character.isLetter(ch)) {
                pos = processIdentifierOrKeyword(line, pos, tokens);
            } else if (Character.isDigit(ch) || ch == '.') {
                pos = processNumber(line, pos, tokens);
            } else if (isOperator(ch)) {
                tokens.add(new Token(Token.TokenType.OPERATOR, String.valueOf(ch), currentLine));
                pos++;
            } else if (ch == '\'') {
                pos = processCharacter(line, pos, tokens);
            } else if (ch == '=') {
                pos = processAssignment(line, pos, tokens);
            } 
            else {
                errorHandler.logError(currentLine, "Illegal character: " + ch);
                pos++;
            }
        }
        return tokens;
    }

    private int processIdentifierOrKeyword(String line, int pos, List<Token> tokens) {
        int start = pos;
        while (pos < line.length() && (Character.isLetterOrDigit(line.charAt(pos)) || line.charAt(pos) == '_')) {
            pos++;
        }
        String word = line.substring(start, pos);
        
        if (isKeyword(word) && word != "mrWorld") {
            tokens.add(new Token(Token.TokenType.KEYWORD, word, currentLine));
            if (symbolTable.getEntry(word) == null) { 
                symbolTable.addEntry(word, "Reserved", "local");
            }
        } else if (word == "mrWorld") {
            tokens.add(new Token(Token.TokenType.KEYWORD, word, currentLine));
            if (symbolTable.getEntry(word) == null) { 
                symbolTable.addEntry(word, "Reserved", "Global");
            }
        } else {
            if (symbolTable.getEntry(word) == null) { 
                symbolTable.addEntry(word, "Identifier", "local");
            }
            tokens.add(new Token(Token.TokenType.IDENTIFIER, word, currentLine));
        }
        
        return pos;
    }

    private int processAssignment(String line, int pos, List<Token> tokens) {
        if (pos + 1 < line.length() && line.charAt(pos + 1) == '=') {
            tokens.add(new Token(Token.TokenType.OPERATOR, "==", currentLine));
            return pos + 2; // Move past '=='
        } else {
            tokens.add(new Token(Token.TokenType.ASSIGNMENT, "=", currentLine));
            return pos + 1; // Move past '='
        }
    }    

    private boolean isCommentStart(String line, int pos) {
        return line.startsWith("%%", pos) || line.startsWith("%*", pos);
    }

    private int processComment(String line, int pos, List<Token> tokens) {
        if (line.startsWith("%%", pos)) {
            // Single-line comment
            String comment = line.substring(pos);
            tokens.add(new Token(Token.TokenType.SINGLE_LINE_COMMENT, comment, currentLine));
            return line.length(); // Move to the end of the line
        } else if (line.startsWith("%*", pos)) {
            int endPos = line.indexOf("*%", pos + 2);
            if (endPos == -1) {
                errorHandler.logError(currentLine, "Unterminated multi-line comment");
                return line.length();
            }
            String comment = line.substring(pos, endPos + 2);
            tokens.add(new Token(Token.TokenType.MULTI_LINE_COMMENT, comment, currentLine));
            return endPos + 2;
        }
        return pos;
    }

    private boolean isKeyword(String word) {
        for (String keyword : KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private int processNumber(String line, int pos, List<Token> tokens) {
        int start = pos;
        boolean hasDecimal = false;
        while (pos < line.length() && (Character.isDigit(line.charAt(pos)) || line.charAt(pos) == '.')) {
            if (line.charAt(pos) == '.') {
                if (hasDecimal) {
                    errorHandler.logError(currentLine, "Invalid number format: multiple decimal points");
                    break;
                }
                hasDecimal = true;
            }
            pos++;
        }
        String number = line.substring(start, pos);
        if (hasDecimal) {
            tokens.add(new Token(Token.TokenType.DECIMAL, number, currentLine));
        } else {
            tokens.add(new Token(Token.TokenType.INTEGER, number, currentLine));
        }
        return pos;
    }

    private int processCharacter(String line, int pos, List<Token> tokens) {
        if (pos + 2 >= line.length() || line.charAt(pos + 2) != '\'') {
            errorHandler.logError(currentLine, "Invalid character literal");
            return line.length();
        }
        String character = line.substring(pos, pos + 3);
        tokens.add(new Token(Token.TokenType.CHARACTER, character, currentLine));
        return pos + 3;
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%' || ch == '^';
    }
}
