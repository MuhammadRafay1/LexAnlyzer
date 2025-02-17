import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public Map<String, Entry> table = new HashMap<>();

    public static class Entry {
        String name;
        String type;
        String scope;

        public Entry(String name, String type, String scope) {
            this.name = name;
            this.type = type;
            this.scope = scope;
        }
    }

    public void addEntry(String name, String type, String scope) {
        if (!table.containsKey(name)) {
            table.put(name, new Entry(name, type, scope));
        }
    }

    public Entry getEntry(String name) {
        return table.get(name);
    }
}