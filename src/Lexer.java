// Lexer.java
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
            put("WHITESPACE", "[ \t\r\n]");
            put("MULTI_LINE_COMMENT", "%\\*");
            put("SINGLE_LINE_COMMENT", "%%");
            // Place IDENTIFIER before KEYWORD to ensure proper matching
            put("IDENTIFIER", "[a-zA-Z][a-zA-Z0-9]*|_[a-zA-Z0-9]+");
            put("KEYWORD", "int|dec|bln|char|mrWorld|mrArea");
            put("BOOLEAN", "true|false");
            put("DECIMAL", "[0-9]+\\.[0-9]+");
            put("INTEGER", "[0-9]+");
            put("OPERATOR", "[+\\-*/%^]");
            put("ASSIGNMENT", "=");
        }};
        return convertToDFA(buildCombinedNFA(tokenSpecs));
    }

    

    private NFA parseRegex(String pattern) {
        if (pattern.isEmpty()) {
            return new NFA(new State(), new State());
        }

        if (pattern.startsWith("[") && pattern.endsWith("]")) {
            // Handle character class
            String chars = pattern.substring(1, pattern.length() - 1);
            return parseCharacterClass(chars);
        }

        List<NFA> alternatives = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            
            if (c == '\\' && i + 1 < pattern.length()) {
                char nextChar = pattern.charAt(++i);
                if (nextChar == '.') {
                    if (current.length() > 0) {
                        alternatives.add(parseSimplePattern(current.toString()));
                        current.setLength(0);
                    }
                    alternatives.add(NFA.fromChar('.'));
                } else {
                    current.append(nextChar);
                }
                continue;
            }
            
            if (c == '|') {
                if (current.length() > 0) {
                    alternatives.add(parseSimplePattern(current.toString()));
                    current.setLength(0);
                }
                continue;
            }
            
            if (c == '[') {
                int closeBracket = findClosingBracket(pattern, i);
                if (closeBracket != -1) {
                    if (current.length() > 0) {
                        alternatives.add(parseSimplePattern(current.toString()));
                        current.setLength(0);
                    }
                    alternatives.add(parseCharacterClass(pattern.substring(i + 1, closeBracket)));
                    i = closeBracket;
                    continue;
                }
            }
            
            if (c == '*' || c == '+') {
                if (current.length() > 0) {
                    NFA base = parseSimplePattern(current.toString());
                    alternatives.add(c == '*' ? NFA.kleeneStar(base) : 
                                              NFA.concat(base, NFA.kleeneStar(base)));
                    current.setLength(0);
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            alternatives.add(parseSimplePattern(current.toString()));
        }
        
        if (alternatives.isEmpty()) {
            return new NFA(new State(), new State());
        }
        
        NFA result = alternatives.get(0);
        for (int i = 1; i < alternatives.size(); i++) {
            result = NFA.alternate(result, alternatives.get(i));
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

    private NFA parseCharacterClass(String chars) {
        if (chars.contains("-")) {
            char start = chars.charAt(0);
            char end = chars.charAt(2);
            return NFA.range(start, end);
        }
        
        NFA result = null;
        for (char c : chars.toCharArray()) {
            if (c == '\\') continue;  // Skip escape character
            NFA charNFA = NFA.fromChar(c);
            result = (result == null) ? charNFA : NFA.alternate(result, charNFA);
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
                
                return new Token(Token.TokenType.valueOf(lastAcceptState.tokenType), lexeme, line);
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