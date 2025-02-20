public class Token {
    public enum TokenType {
        KEYWORD, IDENTIFIER, INTEGER, DECIMAL, BOOLEAN, CHAR,
        OPERATOR, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, ASSIGNMENT,
        WHITESPACE, ERROR, NEWLINE
    }

    private TokenType type;
    private String value;
    private int line;

    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    public TokenType getType() { return type; }
    public String getValue() { return value; }
    public int getLine() { return line; }

    @Override
    public String toString() {
        return "Token [type=" + type + ", value=" + value + ", line=" + line + "]";
    }
}
