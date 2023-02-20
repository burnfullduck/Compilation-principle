parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;
}

program
   : compUnit
   ;
compUnit
   : (funcDef | decl)+ EOF
   ;

decl
    : constDecl
    | varDecl
    ;

constDecl
    : 'const' bType constDef (',' constDef)* ';'
    ;

bType
    : 'int'
    ;

constDef
    : IDENT ('[' constExp ']')* '=' constInitVal
    ;

constInitVal
    : constExp
    | '{' (constInitVal (','constInitVal)* )? '}'
    ;

varDecl
    : bType varDef (',' varDef)* ';'
    ;

varDef
    : IDENT  ( '[' constExp ']' )*
    | IDENT ( '[' constExp ']' )* '=' initVal
    ;

initVal
    :exp
    | '{' ( initVal ( ',' initVal )* )? '}'
    ;

funcDef
    :funcType IDENT '(' (funcFParams)? ')' block
    ;

funcType
    :'void'
    |'int'
    ;

funcFParams
    : funcFParam ( ',' funcFParam )*
    ;

funcFParam
    :bType IDENT ('[' ']' ( '[' exp ']' )* )?
    ;

block
    :'{' ( blockItem )* '}'
    ;

blockItem
    : decl
    | stmt
    ;

stmt
    :lVal '=' exp ';'                               #asignStmt
    | exp? ';'                                      #expStmt
    | block                                         #blockStmt
    | 'if' '(' cond ')' stmt ( 'else' stmt )?       #ifelseStmt
    | 'while' '(' cond ')' stmt                     #whileStmt
    | 'break' ';'                                   #breakStmt
    | 'continue' ';'                                #continueStmt
    | 'return' exp? ';'                             #returnStmt
    ;

exp
   : L_PAREN exp R_PAREN #expParenthesis
   | lVal #lvalExp
   | number #numberExp
   | IDENT L_PAREN funcRParams? R_PAREN #callFuncExp
   | unaryOp exp #unaryOpExp
   | exp (MUL | DIV | MOD) exp #mulExp
   | exp (PLUS | MINUS) exp #plusExp
   ;

cond
   : exp #expCond
   | cond (LT | GT | LE | GE) cond #ltCond
   | cond (EQ | NEQ) cond #eqCond
   | cond AND cond #andCond
   | cond OR cond #orCond
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   :DECIMAL_CONST
   |OCTAL_CONST
   |HEXADECIMAL_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   ;

constExp
   : exp
   ;
