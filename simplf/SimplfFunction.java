package simplf;
 
import java.util.List;

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

    // Hybrid environment wrapper: lexical (primary) with dynamic fallback to caller
    private static class HybridEnv extends Environment {
        private final Environment primary;
        private final Environment fallback;

        HybridEnv(Environment primary, Environment fallback) {
            // Do not chain via super; we only delegate.
            super((Environment) null);
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        Object get(Token name) {
            return primary.getOrFallback(name, fallback);
        }

        @Override
        void assign(Token name, Object value) {
            primary.assignOrFallback(name, value, fallback);
        }

        @Override
        void define(Token tok, String name, Object value) {
            primary.define(tok, name, value);
        }
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        // Lexical closure for locals
        Environment functionFrame = new Environment(closure);
        // Dynamic fallback via caller env
        Environment callerEnv = interpreter.environment;
        Environment hybrid = new HybridEnv(functionFrame, callerEnv);

        // Bind arguments to parameters in the function frame (lexical locals)
        for (int i = 0; i < arity(); i++) {
            Token param = declaration.params.get(i);
            functionFrame.define(param, param.lexeme, args.get(i));
        }

        // Execute the function body and capture the last expression's value implicitly
        Object lastValue = null;
        Environment previous = interpreter.environment;
        try {
            interpreter.environment = hybrid;
            for (int i = 0; i < declaration.body.size(); i++) {
                Stmt stmt = declaration.body.get(i);
                if (i == declaration.body.size() - 1 && stmt instanceof Stmt.Expression) {
                    Stmt.Expression exprStmt = (Stmt.Expression) stmt;
                    lastValue = interpreter.evaluate(exprStmt.expr);
                } else {
                    interpreter.execute(stmt);
                }
            }
            return lastValue;
        } finally {
            interpreter.environment = previous;
        }
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}