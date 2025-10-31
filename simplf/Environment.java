package simplf; 
 
import java.util.HashMap;
import java.util.Map;

/**
 * Manages variable bindings and implements lexical scoping by chaining environments.
 */
class Environment {
    
    final Map<String, Object> values = new HashMap<>();
    final Environment enclosing;
    
    /**
     * Creates the global environment (no enclosing scope).
     */
    Environment() {
        this.enclosing = null;
    }

    /**
     * Creates a new nested environment, chaining to the enclosing scope.
     */
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Defines a new variable in the current scope (Part 1).
     */
    void define(Token varToken, String name, Object value) {
        values.put(name, value);
    }
    
    /**
     * Looks up the value of a variable, walking up the lexical scope chain (Part 1, 3).
     */
    Object get(Token name) {
        String varName = name.lexeme;
        if (values.containsKey(varName)) {
            return values.get(varName);
        }
        
        // Search in the enclosing environment (lexical scoping)
        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + varName + "'.");
    }

    /**
     * Finds and updates an existing variable's value, walking up the scope chain (Part 1, 3).
     */
    void assign(Token name, Object value) {
        String varName = name.lexeme;
        if (values.containsKey(varName)) {
            values.put(varName, value);
            return;
        }

        // Search and assign in the enclosing environment
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Assignment to undefined variable '" + varName + "'.");
    }

    // -------- Added helpers for hybrid lexical+dynamic resolution --------

    private Object getFromChain(Token name) {
        String varName = name.lexeme;
        for (Environment env = this; env != null; env = env.enclosing) {
            if (env.values.containsKey(varName)) return env.values.get(varName);
        }
        throw new RuntimeError(name, "Undefined variable '" + varName + "'.");
    }

    private static Object getFromChain(Token name, Environment start) {
        String varName = name.lexeme;
        for (Environment env = start; env != null; env = env.enclosing) {
            if (env.values.containsKey(varName)) return env.values.get(varName);
        }
        return null; // not found
    }

    private boolean assignInChain(Token name, Object value) {
        String varName = name.lexeme;
        for (Environment env = this; env != null; env = env.enclosing) {
            if (env.values.containsKey(varName)) { env.values.put(varName, value); return true; }
        }
        return false;
    }

    private static boolean assignInChain(Token name, Object value, Environment start) {
        String varName = name.lexeme;
        for (Environment env = start; env != null; env = env.enclosing) {
            if (env.values.containsKey(varName)) { env.values.put(varName, value); return true; }
        }
        return false;
    }

    Object getOrFallback(Token name, Environment fallbackRoot) {
        try {
            return getFromChain(name);
        } catch (RuntimeError e) {
            if (fallbackRoot != null) {
                Object v = getFromChain(name, fallbackRoot);
                if (v != null) return v;
            }
            throw e;
        }
    }

    void assignOrFallback(Token name, Object value, Environment fallbackRoot) {
        if (assignInChain(name, value)) return;
        if (fallbackRoot != null && assignInChain(name, value, fallbackRoot)) return;
        throw new RuntimeError(name, "Assignment to undefined variable '" + name.lexeme + "'.");
    }
}