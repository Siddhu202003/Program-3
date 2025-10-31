package simplf; 

import java.util.List;
import java.util.ArrayList;

import simplf.Expr.Lambda;
import simplf.Stmt.For;
import simplf.Stmt.Function;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {
    public Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        // ... (existing constructor logic)
    }

    public void interpret(List<Stmt> stmts) {
        try {
            for (Stmt stmt : stmts) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Simplf.runtimeError(error);
        }
    }

    // --- Core Execution Helpers ---

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private Object execute(Stmt stmt) {
        return stmt.accept(this);
    }
    
    /**
     * Executes a list of statements within a specified environment (for blocks/functions).
     */
    void executeBlock(List<Stmt> statements, Environment env) {
        Environment previous = this.environment;
        try {
            this.environment = env;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    // **********************************************************************
    // Part 1: Variables and Assignments
    // **********************************************************************

    @Override
    public Object visitExprStmt(Stmt.Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }
    
    @Override
    public Object visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        // Define variable in the current lexical scope.
        environment.define(stmt.name, stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitBlockStmt(Stmt.Block stmt) {
        // Create a new scope for the block, chaining to the current one.
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visitVarExpr(Expr.Variable expr) {
        // Lookup variable value, automatically walking up the scope chain.
        return environment.get(expr.name);
    }
    
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        // Find and update the variable's value up the scope chain.
        environment.assign(expr.name, value);
        // Assignment expressions return the assigned value.
        return value;
    }

    // **********************************************************************
    // Part 2: Control Flow
    // **********************************************************************
    
    @Override
    public Object visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.cond))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.cond))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitForStmt(For stmt) {
        // For loops are desugared and should not be visited here.
        throw new UnsupportedOperationException("For loops are not interpreted.");
    }
    
    // **********************************************************************
    // Part 3: Functions
    // **********************************************************************

    @Override
    public Object visitFunctionStmt(Stmt.Function stmt) {
        // Create a function object (closure), capturing the current lexical environment.
        SimplfFunction function = new SimplfFunction(stmt, environment);
        
        // Define the function name in the current environment.
        environment.define(stmt.name, stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Object visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new ReturnControlFlow(value);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        
        if (!(callee instanceof SimplfCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions.");
        }
        
        SimplfCallable function = (SimplfCallable) callee;
        
        // Evaluate arguments before the call (eager evaluation).
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.args) {
            arguments.add(evaluate(argument));
        }

        // Check arity
        if (arguments.size() != function.arity()) {
           throw new RuntimeError(expr.paren, "Expected " + function.arity() + 
                                   " arguments but got " + arguments.size() + ".");
        }
        
        // Perform the call.
        return function.call(this, arguments);
    }
    
    @Override
    public Object visitLambda(Lambda expr) {
        // Lambda creates an anonymous function. We wrap its body in a pseudo-Function Stmt.
        Function anonymousDeclaration = new Stmt.Function(
            new Token(TokenType.FUN, "lambda", null, 0, 0), 
            expr.params, 
            List.of(new Stmt.Expression(expr.body)) 
        );
        return new SimplfFunction(anonymousDeclaration, environment);
    }
    
    // --- Existing visitors and helpers (no change required) ---

    @Override
    public Object visitPrintStmt(Stmt.Print stmt) {
        Object val = evaluate(stmt.expr);
        System.out.println(stringify(val));
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.op.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.op.type) {
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                throw new RuntimeError(expr.op, "Addition operation not supported for operands.");
            case MINUS:
                checkNumbers(expr.op, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumbers(expr.op, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumbers(expr.op, left, right);
                if (right instanceof Double && (double) right == 0.0) {
                    throw new RuntimeError(expr.op, "Cannot divide by zero.");
                }
                return (double) left / (double) right;
            case GREATER:
                checkNumbers(expr.op, left, right);
                return (boolean)((double) left > (double) right);
            case GREATER_EQUAL:
                checkNumbers(expr.op, left, right);
                return (boolean)((double) left >= (double) right);
            case LESS:
                checkNumbers(expr.op, left, right);
                return (boolean)((double) left < (double) right);
            case LESS_EQUAL:
                checkNumbers(expr.op, left, right);
                return (boolean)((double) left <= (double) right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case COMMA:
                return right;
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitUnary(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.op.type) {
            case MINUS:
                checkNumber(expr.op, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitLiteral(Expr.Literal expr) {
        return expr.val;
    }

    @Override
    public Object visitGrouping(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    
    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        if (isTruthy(evaluate(expr.cond))) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null)
            return b == null;
        return a.equals(b);
    }

    private void checkNumber(Token op, Object object) {
        if (object instanceof Double)
            return;
        throw new RuntimeError(op, "Operand must be a number");
    }

    private void checkNumbers(Token op, Object a, Object b) {
        if (a instanceof Double && b instanceof Double)
            return;
        throw new RuntimeError(op, "Operand must be numbers");
    }

    private String stringify(Object object) {
        if (object == null)
            return "nil";
        if (object instanceof Double) {
            String num = object.toString();
            if (num.endsWith(".0")) {
                num = num.substring(0, num.length() - 2);
            }
            return num;
        }
        return object.toString();
    }
}