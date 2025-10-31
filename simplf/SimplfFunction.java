package simplf;
 
import java.util.List;
import simplf.Stmt.Function;

/**
 * Represents a user-defined function object (closure).
 * Implements SimplfCallable for execution.
 */
class SimplfFunction implements SimplfCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    /**
     * Creates a function object, capturing the environment where it was defined.
     */
    SimplfFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure; 
    }
    
    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        // For SimPL-F: Use dynamic scoping - create new environment from current interpreter environment
        // instead of lexical closure
        Environment functionEnv = new Environment(interpreter.environment);

        // Bind arguments to parameters.
        for (int i = 0; i < arity(); i++) {
            Token param = declaration.params.get(i);
            functionEnv.define(param, param.lexeme, args.get(i));
        }

        // Execute the function body and capture the last expression's value
        Object lastValue = null;
        
        Environment previous = interpreter.environment;
        try {
            interpreter.environment = functionEnv;
            
            for (int i = 0; i < declaration.body.size(); i++) {
                Stmt stmt = declaration.body.get(i);
                
                // If this is the last statement and it's an expression statement,
                // capture its value as the return value
                if (i == declaration.body.size() - 1 && stmt instanceof Stmt.Expression) {
                    Stmt.Expression exprStmt = (Stmt.Expression) stmt;
                    lastValue = interpreter.evaluate(exprStmt.expr);
                } else {
                    interpreter.execute(stmt);
                }
            }
            
            // Return the last expression's value, or null if no expression
            return lastValue;
            
        } catch (ReturnControlFlow returnValue) {
            // Catches the value thrown by a return statement.
            return returnValue.value;
        } finally {
            // Always restore the environment
            interpreter.environment = previous;
        }
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}

/**
 * Custom exception used to unwind the stack for function return values. 
 * Place this directly in simplf/SimplfFunction.java to avoid creating a new file.
 */
class ReturnControlFlow extends RuntimeException {
    final Object value;
    
    ReturnControlFlow(Object value) {
        // Suppress stack trace for clean control flow.
        super(null, null, false, false);
        this.value = value;
    }
}