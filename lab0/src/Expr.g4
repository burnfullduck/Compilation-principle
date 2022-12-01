grammar Expr;

@header {
package simpleexpr;
}

// *: 0 or more
prog : stat* EOF ;

//ID 词法结构//expr,stat等语法结构
//'if': literal 字面量, 隐式的词法单元 ，放在显式的词法单元前面
stat : expr ';'
     | ID '=' expr ';'
     | 'if' expr ';'
     ;

// | : or
// (): subrule
//顺序表示优先级
expr : expr ('*'|'/') expr
     | expr ('+'|'-') expr
     | ID
     | INT
     | FLOAT
     ;

//Alt+Insert 自动生成隐式词法单元
SEMI : ';' ;
ASSIGN : '=' ;
IF : 'if' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

//ID 是字母或下划线开头
//ID : ([a-zA-Z] |'_') ([a-zA-Z0-9] |'_')*;
ID : (LETTER |'_') (LETTER| DIGIT |'_')* ;

//+: 1 or more
INT : '0'|([1-9] DIGIT*) ;
FLOAT : DIGIT '.' DIGIT+ ;

WS : [ \t\r\n]+ -> skip ;

//single line comment
//一个.可以匹配任意字符    加个?就变成非贪婪匹配
SL_COMMENT : '//' .*? '\n' -> skip ;
ML_COMMENT : '/*' .*? '*/' -> skip ;

//fragment关键词令其识别到下面定义的东西时不将其识别为token
fragment LETTER : [a-zA-Z] ;
fragment DIGIT : [0-9] ;