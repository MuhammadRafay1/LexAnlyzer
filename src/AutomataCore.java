import java.util.*;

// ###########################
// #   Automata Core Classes #
// ###########################

class State {
    private static int nextId = 0;
    public final int id;
    public final Map<Character, Set<State>> transitions = new HashMap<>();
    public String tokenType;

    public State() {
        this.id = nextId++;
    }

    public void addTransition(char symbol, State state) {
        transitions.computeIfAbsent(symbol, k -> new HashSet<>()).add(state);
    }
}

class NFA {
    public State start;
    public State accept;

    public NFA(State start, State accept) {
        this.start = start;
        this.accept = accept;
    }

    public static NFA fromChar(char c) {
        State start = new State();
        State accept = new State();
        start.addTransition(c, accept);
        return new NFA(start, accept);
    }

    public static NFA fromString(String str) {
        if (str.isEmpty()) {
            State state = new State();
            return new NFA(state, state);
        }
        NFA nfa = fromChar(str.charAt(0));
        for (int i = 1; i < str.length(); i++) {
            NFA next = fromChar(str.charAt(i));
            nfa = concat(nfa, next);
        }
        return nfa;
    }

    public static NFA concat(NFA first, NFA second) {
        first.accept.addTransition('\0', second.start);
        return new NFA(first.start, second.accept);
    }

    public static NFA alternate(NFA a, NFA b) {
        State start = new State();
        State accept = new State();
        
        start.addTransition('\0', a.start);
        start.addTransition('\0', b.start);
        
        a.accept.addTransition('\0', accept);
        b.accept.addTransition('\0', accept);
        
        return new NFA(start, accept);
    }

    public static NFA kleeneStar(NFA nfa) {
        State start = new State();
        State accept = new State();
        
        start.addTransition('\0', accept);
        start.addTransition('\0', nfa.start);
        nfa.accept.addTransition('\0', accept);
        nfa.accept.addTransition('\0', nfa.start);
        
        return new NFA(start, accept);
    }

    public static NFA range(char from, char to) {
        NFA result = null;
        for (char c = from; c <= to; c++) {
            NFA charNFA = fromChar(c);
            result = (result == null) ? charNFA : alternate(result, charNFA);
        }
        return result;
    }
}


class DFAState {
    public final Set<State> nfaStates;
    public final Map<Character, DFAState> transitions = new HashMap<>();
    public String tokenType;

    public DFAState(Set<State> nfaStates) {
        this.nfaStates = nfaStates;
        this.tokenType = determineTokenType();
    }

    private String determineTokenType() {
        for(State s : nfaStates) {
            if(s.tokenType != null) return s.tokenType;
        }
        return null;
    }
}

class DFA {
    public DFAState startState;
    public List<DFAState> states = new ArrayList<>();
}