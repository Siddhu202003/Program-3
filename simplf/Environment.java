package simplf; 

import java.util.HashMap;
import java.util.Map;

/**
 * Manages variable bindings and implements lexical scoping by chaining environments.
 */
class Environment {
    
    private final Map<String, Object> values = new HashMap<>();
    private final Environment enclosing;
    
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
}