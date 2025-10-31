grammar simplf;

@header {
    package simplf.parser;
    import simplf.Stmt;
    import simplf.Stmt.*;

    import simplf.Expr;
    import simplf.Expr.*;
}

program returns [List<Stmt> val = new ArrayList<>();] : (declaration {$val.add($declaration.val);})* ;

declaration returns [Stmt val]: funDecl      {$val = $funDecl.val;}
                             |  varDecl      {$val = $varDecl.val;}
                             |  statement    {$val = $statement.val;};

funDecl returns [Stmt val]: 
      FUN id LEFT_PAREN RIGHT_PAREN block           {$val = new Stmt.Function($id.val, new ArrayList<>(), $block.val);}
    | FUN id LEFT_PAREN params RIGHT_PAREN block    {$val = new Stmt.Function($id.val, $params.val, $block.val);};

params returns [List<simplf.Token> val = new ArrayList<>();] :
    id1=id {$val.add($id1.val);} (COMMA idi=id {$val.add($idi.val);})*;

varDecl returns [Stmt val] :    VAR id SEMICOLON                        {$val = new Stmt.Var($id.val, null);}
                           |    VAR id EQUAL expression SEMICOLON       {$val = new Stmt.Var($id.val, $expression.val);};

statement returns [Stmt val] :  exprStatement                           {$val = $exprStatement.val;}
          |                     ifStatement                             {$val = $ifStatement.val;}
          |                     printStatement                          {$val = $printStatement.val;}
          |                     whileStatement                          {$val = $whileStatement.val;}
          |                     forStatement                            {$val = $forStatement.val;}
          |                     block                                   {$val = new Stmt.Block($block.val);};

block returns [List<Stmt> val] : LEFT_BRACE program RIGHT_BRACE          {$val = $program.val;};

ifStatement returns [Stmt val] :    IF LEFT_PAREN expression RIGHT_PAREN statement                          {$val = new Stmt.If($expression.val, $statement.val, null);}
                               |    IF LEFT_PAREN expression RIGHT_PAREN s1=statement ELSE s2=statement     {$val = new Stmt.If($expression.val, $s1.val, $s2.val);};

whileStatement returns [Stmt val] :    WHILE LEFT_PAREN expression RIGHT_PAREN statement {
    $val = new Stmt.While($expression.val, $statement.val);
};

forStatement returns [Stmt val] :       FOR LEFT_PAREN init=expression SEMICOLON cond=expression SEMICOLON update=expression RIGHT_PAREN statement {
    $val = new Stmt.For($init.val, $cond.val, $update.val, $statement.val);
};

exprStatement returns [Stmt val] : expression SEMICOLON         {$val = new Stmt.Expression($expression.val);};
printStatement returns [Stmt val] : PRINT expression SEMICOLON  {$val = new Stmt.Print($expression.val);};

arguments returns [List<Expr> val = new ArrayList<>();] :
    arg1=expression {$val.add($arg1.val);} (COMMA argi=expression {$val.add($argi.val);})*;



expression returns [Expr val] locals [simplf.Token op, List<Expr> args] : literal {$val = new Expr.Literal($literal.val);}
           |                    id { $val = new Expr.Variable($id.val); }
           |                    e1=expression LEFT_PAREN rparen { $val = new Expr.Call($e1.val, $rparen.val, new ArrayList<>()); }
           |                    e1=expression LEFT_PAREN arguments rparen { $val = new Expr.Call($e1.val, $rparen.val, $arguments.val); }
           |<assoc=right>       (minus {$op = $minus.val;} | bang {$op = $bang.val;}) expression {
                                    $val = new Expr.Unary($op, $expression.val);
                                }
           |                    e1=expression (slash {$op = $slash.val;} | star {$op = $star.val;}) e2=expression {
                                    $val = new Expr.Binary($e1.val, $op, $e2.val);
                                }
           |                    e1=expression (plus {$op = $plus.val;} | minus {$op = $minus.val;}) e2=expression {
                                    $val = new Expr.Binary($e1.val, $op, $e2.val);
                                }
           |                    e1=expression (greater {$op = $greater.val;} | greater_equal {$op = $greater_equal.val;} | less {$op = $less.val;} | less_equal {$op = $less_equal.val;}) e2=expression {
                                    $val = new Expr.Binary($e1.val, $op, $e2.val);
                                }
           |                    e1=expression (equal_equal {$op = $equal_equal.val;} | bang_equal {$op = $bang_equal.val;}) e2=expression {
                                    $val = new Expr.Binary($e1.val, $op, $e2.val);
                                }
           |                    expression and expression {$val = new Expr.Logical($e1.val, $and.val, $e2.val);}
           |                    e1=expression or e2=expression {$val = new Expr.Logical($e1.val, $or.val, $e2.val);}
           |                    id EQUAL expression {$val = new Expr.Assign($id.val, $expression.val);}
           |                    LEFT_PAREN expression RIGHT_PAREN {$val = new Expr.Grouping($expression.val);}
           |                    LAMBDA params COLON expression {$val = new Expr.Lambda($params.val, $expression.val);};

literal returns [Object val]:   STRINGLIT  {$val = $STRINGLIT.text.substring(1,$STRINGLIT.text.length()-1);}
                            |   NUMBER  {$val = (Double) Double.parseDouble($NUMBER.text);}
                            |   TRUE    {$val = true;}
                            |   FALSE   {$val = false;}
                            |   NIL     {$val = null;};


id returns [simplf.Token val]: ident=IDENTIFIER {$val = new simplf.Token(simplf.TokenType.IDENTIFIER, $ident.text, $ident.text, $ident.line, $ident.pos);};

or returns [simplf.Token val]: OR {$val = new simplf.Token(simplf.TokenType.OR, $OR.text, null, $OR.line, $OR.pos);};
and returns [simplf.Token val]: AND {$val = new simplf.Token(simplf.TokenType.AND, $AND.text, null, $AND.line, $AND.pos);};

equal returns [simplf.Token val]: EQUAL {$val = new simplf.Token(simplf.TokenType.EQUAL, $EQUAL.text, null, $EQUAL.line, $EQUAL.pos);};
bang_equal returns [simplf.Token val]: BANG_EQUAL {$val = new simplf.Token(simplf.TokenType.BANG_EQUAL, $BANG_EQUAL.text, null, $BANG_EQUAL.line, $BANG_EQUAL.pos);};
equal_equal returns [simplf.Token val]: EQUAL_EQUAL {$val = new simplf.Token(simplf.TokenType.EQUAL_EQUAL, $EQUAL_EQUAL.text, null, $EQUAL_EQUAL.line, $EQUAL_EQUAL.pos);};

greater returns [simplf.Token val]: GREATER {$val = new simplf.Token(simplf.TokenType.GREATER, $GREATER.text, null, $GREATER.line, $GREATER.pos);};
greater_equal returns [simplf.Token val]: GREATER_EQUAL {$val = new simplf.Token(simplf.TokenType.GREATER_EQUAL, $GREATER_EQUAL.text, null, $GREATER_EQUAL.line, $GREATER_EQUAL.pos);};
less returns [simplf.Token val]: LESS {$val = new simplf.Token(simplf.TokenType.LESS, $LESS.text, null, $LESS.line, $LESS.pos);};
less_equal returns [simplf.Token val]: LESS_EQUAL {$val = new simplf.Token(simplf.TokenType.LESS_EQUAL, $LESS_EQUAL.text, null, $LESS_EQUAL.line, $LESS_EQUAL.pos);};

plus returns [simplf.Token val]: PLUS {$val = new simplf.Token(simplf.TokenType.PLUS, $PLUS.text, null, $PLUS.line, $PLUS.pos);};
minus returns [simplf.Token val]: MINUS {$val = new simplf.Token(simplf.TokenType.MINUS, $MINUS.text, null, $MINUS.line, $MINUS.pos);};

slash returns [simplf.Token val]: SLASH {$val = new simplf.Token(simplf.TokenType.SLASH, $SLASH.text, null, $SLASH.line, $SLASH.pos);};
star returns [simplf.Token val]: STAR {$val = new simplf.Token(simplf.TokenType.STAR, $STAR.text, null, $STAR.line, $STAR.pos);};

bang returns [simplf.Token val]: BANG {$val = new simplf.Token(simplf.TokenType.BANG, $BANG.text, null, $BANG.line, $BANG.pos);};

rparen returns [simplf.Token val]: RIGHT_PAREN {$val = new simplf.Token(simplf.TokenType.RIGHT_PAREN, $RIGHT_PAREN.text, null, $RIGHT_PAREN.line, $RIGHT_PAREN.pos);};



LEFT_PAREN  : '(';
RIGHT_PAREN : ')';
LEFT_BRACE  : '{';
RIGHT_BRACE : '}';
COMMA       : ',';
DOT         : '.';
MINUS       : '-';
PLUS        : '+';
SEMICOLON   : ';';
COLON       : ':';
STAR        : '*';
SLASH       : '/';

BANG        : '!';
BANG_EQUAL  : '!=';
EQUAL       : '=';
EQUAL_EQUAL : '==';
GREATER     : '>';
GREATER_EQUAL   : '>=';
LESS        : '<';
LESS_EQUAL  : '<=';

AND         : 'and';
CLASS       : 'class';
ELSE        : 'else';
FALSE       : 'false';
FOR         : 'for';
FUN         : 'fun';
IF          : 'if';
NIL         : 'nil';
OR          : 'or';
PRINT       : 'print';
SUPER       : 'super';
THIS        : 'this';
TRUE        : 'true';
VAR         : 'var';
WHILE       : 'while';
LAMBDA      : 'lambda';

IDENTIFIER  : [a-zA-Z_] [a-zA-Z0-9_]*;
STRINGLIT   : '"' (~["\n\r])* '"';
NUMBER      : [0-9]+ | [0-9]+'.'[0-9]+;

COMMENT : '//' (~('\n'))* '\n' -> skip;
WHITESPACE  : [ \r\n\t]+ -> skip;

