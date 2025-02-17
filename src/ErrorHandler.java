import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private List<String> errors = new ArrayList<>();

    public void logError(int line, String message) {
        errors.add("Error at line " + line + ": " + message);
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}