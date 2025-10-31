package simplf;

import java.util.ArrayList;
import java.util.List;

import simplf.Expr.Assign;
import simplf.Expr.Binary;
import simplf.Expr.Call;
import simplf.Expr.Conditional;
import simplf.Expr.Grouping;
import simplf.Expr.Lambda;
import simplf.Expr.Literal;
import simplf.Expr.Unary;
import simplf.Expr.Variable;
import simplf.Stmt.Block;
import simplf.Stmt.Expression;
import simplf.Stmt.For;
import simplf.Stmt.Function;
import simplf.Stmt.If;
import simplf.Stmt.Print;
import simplf.Stmt.Var;
import simplf.Stmt.While;

public class Desugar implements Expr.Visitor<Expr>, Stmt.Visitor<Stmt> {

    public Desugar() {
        
    }

    public List<Stmt> desugar(List<Stmt> stmts) {
        ArrayList<Stmt> ret = new ArrayList<>();
        for (Stmt stmt : stmts) {
            ret.add(stmt.accept(this));
        }
        return ret;
    }
    
    // --- Pass-through and recursive visitors (Part 1, 3) ---

    @Override
    public Stmt visitPrintStmt(Print stmt) {
        return new Print(stmt.expr.accept(this));
    }

    @Override
    public Stmt visitExprStmt(Expression stmt) {
        return new Stmt.Expression(stmt.expr.accept(this));
    }

    @Override
    public Stmt visitVarStmt(Var stmt) {
        return new Var(stmt.name, stmt.initializer != null ? stmt.initializer.accept(this) : null);
    }

    @Override
    public Stmt visitBlockStmt(Block stmt) {
        ArrayList<Stmt> new_statements = new ArrayList<>();
        for(Stmt old_state : stmt.statements) {
            new_statements.add(old_state.accept(this));
        }
        return new Block(new_statements);
    }

    @Override
    public Stmt visitIfStmt(If stmt) {
        Stmt new_else;
        if (stmt.elseBranch == null) {
            new_else = null;
        } else {
            new_else = stmt.elseBranch.accept(this);
        }

        return new If(stmt.cond.accept(this),
            stmt.thenBranch.accept(this),
            new_else);
    }

    @Override
    public Stmt visitWhileStmt(While stmt) {
        return new While(stmt.cond.accept(this),
            stmt.body.accept(this));
    }

    /**
     * Implements the desugaring for the ForStmt (Part 2):
     * for (init; cond; incr) body => { init; while (cond) { body; incr; } }
     */
    @Override
    public Stmt visitForStmt(For stmt) {
        // 1. Prepare the loop body: { body; incr; }
        List<Stmt> loopBodyStatements = new ArrayList<>();
        
        // Add body, recursively desugared
        loopBodyStatements.add(stmt.body.accept(this));
        
        if (stmt.incr != null) {
            // The increment is an expression, wrapped in an Expression statement
            loopBodyStatements.add(new Stmt.Expression(stmt.incr.accept(this)));
        }
        
        Stmt innerWhileBlock = new Stmt.Block(loopBodyStatements);

        // 2. Determine condition (defaulting to true for endless loop if null)
        Expr condition = stmt.cond;
        if (condition == null) {
            condition = new Expr.Literal(true);
        } else {
            condition = condition.accept(this);
        }

        Stmt whileLoop = new Stmt.While(condition, innerWhileBlock);

        // 3. Create the outer block: { init; whileLoop; }
        List<Stmt> outerBlockStatements = new ArrayList<>();
        
        if (stmt.init != null) {
            // The initializer is an expression, wrapped in an Expression statement 
            outerBlockStatements.add(new Stmt.Expression(stmt.init.accept(this)));
        }
        
        outerBlockStatements.add(whileLoop);

        // The outer BlockStmt defines the local scope for the initializer.
        return new Stmt.Block(outerBlockStatements);
    }

    @Override
    public Stmt visitFunctionStmt(Function stmt) {
        ArrayList<Stmt> new_body = new ArrayList<>();
        for (Stmt old_statement : stmt.body) {
            new_body.add(old_statement.accept(this));
        }

        return new Function(stmt.name, stmt.params, new_body);
    }
    
    // --- Expression visitors (no change in logic) ---

    @Override
    public Expr visitBinary(Binary expr) {
        return new Binary(expr.left.accept(this), expr.op, expr.right.accept(this));
    }

    @Override
    public Expr visitUnary(Unary expr) {
        return new Unary(expr.op, expr.right.accept(this));
    }

    @Override
    public Expr visitLiteral(Literal expr) {
        return expr;
    }

    @Override
    public Expr visitGrouping(Grouping expr) {
        return new Grouping(expr.expression.accept(this));
    }

    @Override
    public Expr visitVarExpr(Variable expr) {
        return expr;
    }

    @Override
    public Expr visitAssignExpr(Assign expr) {
        return new Assign(expr.name, expr.value.accept(this));
    }

    @Override
    public Expr visitLogicalExpr(Logical expr) {
        return new Logical(expr.left.accept(this), expr.op, expr.right.accept(this));
    }

    @Override
    public Expr visitConditionalExpr(Conditional expr) {
        return new Conditional(expr.cond.accept(this), 
            expr.thenBranch.accept(this),
            expr.elseBranch.accept(this));
    }

    @Override
    public Expr visitCallExpr(Call expr) {
        ArrayList<Expr> new_args = new ArrayList<>();
        for (Expr arg : expr.args) {
            new_args.add(arg.accept(this));
        }

        return new Call(expr.callee.accept(this), expr.paren, new_args);
    }

    @Override
    public Expr visitLambda(Lambda expr) {
        Expr new_body = expr.body.accept(this);
        return new Lambda(expr.params, new_body);
    }
}