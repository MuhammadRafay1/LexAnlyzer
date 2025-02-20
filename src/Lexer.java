import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Lexer {
    public final DFA dfa;
    private final SymbolTable symbolTable;
    private final ErrorHandler errorHandler;
    private String input;
    private int pos;
    private int line;
    private boolean inMultiLineComment = false;

    public Lexer(String filePath, SymbolTable symbolTable, ErrorHandler errorHandler) throws IOException {
        this.symbolTable = symbolTable;
        this.errorHandler = errorHandler;
        this.input = new String(Files.readAllBytes(Paths.get(filePath)));
        this.dfa = buildDFA();
        this.pos = 0;
        this.line = 1;
    }

   
    private DFA buildDFA() {
        Map<String, String> tokenSpecs = new LinkedHashMap<>() {{
            // Order matters - more specific patterns first
            put("WHITESPACE", "[ \t\r\n]+");
            put("MULTI_LINE_COMMENT", "%\\*");
            put("SINGLE_LINE_COMMENT", "%%");
            // Match operators before numbers to avoid conflicts
            put("OPERATOR", "[\\^\\+\\-\\*/%]");  // Escape ^ and put it first
            put("ASSIGNMENT", "=");
            // Match decimals before integers
            put("DECIMAL", "[0-9]+[.][0-9]+");
            put("INTEGER", "[0-9]+");
            put("CHAR", "'[^']'");  // Single character enclosed in single quotes
            put("BOOLEAN", "true|false");
            put("IDENTIFIER", "[a-zA-Z][a-zA-Z0-9]*|_[a-zA-Z0-9]+");
            put("KEYWORD", "int|dec|bln|char|mrWorld|mrArea");
        }};
        return convertToDFA(buildCombinedNFA(tokenSpecs));
    }

    private NFA parseRegex(String pattern) {
        if (pattern.isEmpty()) {
            return new NFA(new State(), new State());
        }

        if (pattern.endsWith("+")) {
            String basePart = pattern.substring(0, pattern.length() - 1);
            NFA baseNFA = parseRegex(basePart);
            return NFA.concat(baseNFA, NFA.kleeneStar(baseNFA));
        }

        if (pattern.startsWith("[") && pattern.endsWith("]")) {
            String chars = pattern.substring(1, pattern.length() - 1);
            return parseCharacterClass(chars);
        }

        String[] alternatives = pattern.split("\\|");
        if (alternatives.length > 1) {
            List<NFA> nfas = new ArrayList<>();
            for (String alt : alternatives) {
                nfas.add(parseSimplePattern(alt));
            }
            NFA result = nfas.get(0);
            for (int i = 1; i < nfas.size(); i++) {
                result = NFA.alternate(result, nfas.get(i));
            }
            return result;
        }

        return parseSimplePattern(pattern);
    }

    private NFA parseCharacterClass(String chars) {
        List<NFA> nfas = new ArrayList<>();
        
        for (int i = 0; i < chars.length(); i++) {
            if (chars.charAt(i) == '\\' && i + 1 < chars.length()) {
                nfas.add(NFA.fromChar(chars.charAt(++i)));
                continue;
            }
            
            if (i + 2 < chars.length() && chars.charAt(i + 1) == '-') {
                nfas.add(NFA.range(chars.charAt(i), chars.charAt(i + 2)));
                i += 2;
                continue;
            }
            
            nfas.add(NFA.fromChar(chars.charAt(i)));
        }
        
        NFA result = nfas.get(0);
        for (int i = 1; i < nfas.size(); i++) {
            result = NFA.alternate(result, nfas.get(i));
        }
        return result;
    }
    private NFA parseSimplePattern(String pattern) {
        NFA result = null;
        for (int i = 0; i < pattern.length(); i++) {
            NFA charNFA = NFA.fromChar(pattern.charAt(i));
            result = (result == null) ? charNFA : NFA.concat(result, charNFA);
        }
        return result;
    }


    private NFA buildCombinedNFA(Map<String, String> tokenSpecs) {
        List<NFA> nfas = new ArrayList<>();
        for (Map.Entry<String, String> entry : tokenSpecs.entrySet()) {
            NFA nfa = parseRegex(entry.getValue());
            nfa.accept.tokenType = entry.getKey();
            nfas.add(nfa);
        }
        
        // Combine all NFAs with alternation
        NFA combined = nfas.get(0);
        for (int i = 1; i < nfas.size(); i++) {
            combined = NFA.alternate(combined, nfas.get(i));
        }
        return combined;
    }

    private DFA convertToDFA(NFA nfa) {
        DFA dfa = new DFA();
        Set<State> initial = epsilonClosure(Collections.singleton(nfa.start));
        dfa.startState = new DFAState(initial);
        dfa.states.add(dfa.startState);
        
        Queue<DFAState> queue = new LinkedList<>();
        queue.add(dfa.startState);
        
        while (!queue.isEmpty()) {
            DFAState current = queue.poll();
            
            // Consider all possible input characters
            for (char c = 0; c < 128; c++) {
                Set<State> moved = move(current.nfaStates, c);
                if (moved.isEmpty()) continue;
                
                Set<State> closure = epsilonClosure(moved);
                DFAState existing = findExistingState(dfa.states, closure);
                
                if (existing == null) {
                    existing = new DFAState(closure);
                    dfa.states.add(existing);
                    queue.add(existing);
                }
                
                current.transitions.put(c, existing);
            }
        }
        return dfa;
    }

    // Main tokenization method
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            if (inMultiLineComment) {
                skipMultiLineComment();
                continue;
            }
            
            Token token = nextToken();
            if (token != null) {
                tokens.add(token);
                
                // Update symbol table
                switch (token.getType()) {
                    case IDENTIFIER:
                        symbolTable.addEntry(token.getValue(), "identifier", "local");
                        break;
                    case INTEGER:
                        symbolTable.addEntry(token.getValue(), "integer", "local");
                        break;
                    case DECIMAL:
                        symbolTable.addEntry(token.getValue(), "decimal", "local");
                        break;
                    case CHAR:
                        symbolTable.addEntry(token.getValue(), "char", "local");
                        break;
                    case BOOLEAN:
                        symbolTable.addEntry(token.getValue(), "boolean", "local");
                        break;
                    default:
                        break;
                }
            }
        }
        return tokens;
    }

    private int findClosingBracket(String pattern, int start) {
        int count = 1;
        for (int i = start + 1; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '[') count++;
            if (pattern.charAt(i) == ']') count--;
            if (count == 0) return i;
        }
        return -1;
    }

    // Tokenization logic, including assignments and decimal numbers
    private Token nextToken() {
        while (pos < input.length()) {
            // Skip whitespace
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                if (input.charAt(pos) == '\n') line++;
                pos++;
            }
            
            if (pos >= input.length()) break;
            
            // Handle multi-line comments
            if (pos + 1 < input.length() && input.substring(pos, pos + 2).equals("%*")) {
                inMultiLineComment = true;
                pos += 2;
                skipMultiLineComment();
                continue;
            }

            int start = pos;
            DFAState current = dfa.startState;
            DFAState lastAcceptState = null;
            int lastAcceptPos = -1;
            
            while (pos < input.length()) {
                char c = input.charAt(pos);
                DFAState next = current.transitions.get(c);
                
                if (next == null) break;
                
                current = next;
                pos++;
                
                if (current.tokenType != null) {
                    lastAcceptState = current;
                    lastAcceptPos = pos;
                }
            }
            
            if (lastAcceptState != null) {
                String lexeme = input.substring(start, lastAcceptPos);
                pos = lastAcceptPos;

                // Skip whitespace tokens
                if (lastAcceptState.tokenType.equals("WHITESPACE")) continue;

                // Check for keywords
                if (lastAcceptState.tokenType.equals("IDENTIFIER") && isKeyword(lexeme)) {
                    return new Token(Token.TokenType.KEYWORD, lexeme, line);
                }

                Token.TokenType type = Token.TokenType.valueOf(lastAcceptState.tokenType);

                // Update symbol table
                if (type == Token.TokenType.INTEGER) {
                    symbolTable.addEntry(lexeme, "integer", "local");
                } else if (type == Token.TokenType.DECIMAL) {
                    symbolTable.addEntry(lexeme, "decimal", "local");
                } else if (type == Token.TokenType.IDENTIFIER) {
                    symbolTable.addEntry(lexeme, "identifier", "local");
                }

                return new Token(type, lexeme, line);
            }
            
            // Handle unrecognized characters
            errorHandler.logError(line, "Invalid character: '" + input.charAt(pos) + "'");
            pos++;
        }
        
        return null;
    }
    private boolean isKeyword(String lexeme) {
        return lexeme.matches("int|dec|bln|char|mrWorld|mrArea");
    }

    private Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        states.forEach(stack::push);

        while (!stack.isEmpty()) {
            State current = stack.pop();
            Set<State> epsilonTransitions = current.transitions.getOrDefault((char) 0, Collections.emptySet());
            
            for (State next : epsilonTransitions) {
                if (closure.add(next)) {
                    stack.push(next);
                }
            }
        }
        return closure;
    }

    private Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        for (State state : states) {
            Set<State> transitions = state.transitions.getOrDefault(symbol, Collections.emptySet());
            result.addAll(transitions);
        }
        return result;
    }

    private DFAState findExistingState(List<DFAState> states, Set<State> nfaStates) {
        for (DFAState state : states) {
            if (state.nfaStates.equals(nfaStates)) {
                return state;
            }
        }
        return null;
    }

    private void skipMultiLineComment() {
        while (pos < input.length() - 1) {
            if (input.charAt(pos) == '*' && input.charAt(pos + 1) == '%') {
                pos += 2;
                inMultiLineComment = false;
                return;
            }
            if (input.charAt(pos) == '\n') {
                line++;
            }
            pos++;
        }
    }
}
