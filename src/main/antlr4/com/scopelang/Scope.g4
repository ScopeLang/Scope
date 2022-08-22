grammar Scope;

// Parser

program: (statement ENDL)* EOF;

statement: IDENT ASSIGN INT | IDENT ASSIGN DOUBLE;

// Lexer

ASSIGN: '=';

ENDL: ';';

IDENT: [a-zA-Z_][a-zA-Z0-9_]*;

INT: NUMBER;
DOUBLE: DECIMAL_NUMBER;

fragment NUMBER: [0-9]+;
fragment DECIMAL_NUMBER: [0-9]* '.' [0-9]+;

WHITESPACE: [ \t\r\n]+ -> skip;