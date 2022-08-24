grammar Scope;

// ================ //
// ==== Parser ==== //
// ================ //

program: (statement ENDL)* EOF;

statement: invoke;

invoke: IDENT '(' STRING ')';

// =============== //
// ==== Lexer ==== //
// =============== //

ENDL: ';';

IDENT: [a-zA-Z_][a-zA-Z0-9_]*;

// Literals
INT: NUMBER;
DOUBLE: DECIMAL_NUMBER;
STRING: '"' .*? '"';

// Literal fragments
fragment NUMBER: [0-9]+;
fragment DECIMAL_NUMBER: [0-9]* '.' [0-9]+;

// Ignore
WHITESPACE: [ \t\r\n]+ -> skip;