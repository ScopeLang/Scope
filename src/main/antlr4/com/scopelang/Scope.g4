grammar Scope;

// ================ //
// ==== Parser ==== //
// ================ //

// First node
program: code EOF;

code: (statement | codeblock)*?;
codeblock: '{' code '}';

statement: func | invoke;
typeName: VoidType;

func: FuncKeyword typeName Identifier '(' ')' codeblock;
invoke: Identifier '(' StringLiteral? ')' EndLine;

// =============== //
// ==== Lexer ==== //
// =============== //

// Keywords
FuncKeyword: 'func';

// Primitive types
VoidType: 'void';

// Literals
IntLiteral: NumberFrag;
DoubleLiteral: DecimalNumberFrag;
StringLiteral: '"' .*? '"';

// Literal fragments
fragment NumberFrag: [0-9]+;
fragment DecimalNumberFrag: [0-9]* '.' [0-9]+;

// Important
Identifier: [a-zA-Z_][a-zA-Z0-9_]*;
EndLine: ';';

// Ignore
Whitespace: [ \t\r\n]+ -> skip;
LineComment: '//' ~[\r\n]* -> skip; 