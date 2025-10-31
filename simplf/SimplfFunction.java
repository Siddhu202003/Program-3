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
        // 1. Create a new call environment, linked to the function's closure.
        Environment functionEnv = new Environment(closure);

        // 2. Bind arguments to parameters.
        for (int i = 0; i < arity(); i++) {
            Token param = declaration.params.get(i);
            functionEnv.define(param, param.lexeme, args.get(i));
        }

        // 3. Execute the function body.
        try {
            interpreter.executeBlock(declaration.body, functionEnv);
        } catch (ReturnControlFlow returnValue) {
            // Catches the value thrown by a return statement.
            return returnValue.value;
        }
        
        // Functions reaching the end implicitly return nil (null).
        return null;
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