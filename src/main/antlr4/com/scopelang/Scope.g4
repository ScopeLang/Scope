grammar Scope
	;

// ================ //
// ==== Parser ==== //
// ================ //

program // First node
	: outerStatments EOF
	;

code
	: (innerStatement | codeblock)*
	;
codeblock
	: '{' code '}'
	;

outerStatments
	: (outerStatement)*
	;

innerStatement
	: invoke
	| declare
	;
outerStatement
	: function
	;

typeName
	: VoidType
	;

// Inner statements
invoke
	: Identifier '(' expr? ')' EndLine
	;
declare
	: StringType Identifier '=' expr EndLine
	;

// Outer statements
function
	: FuncKeyword typeName Identifier '(' ')' (codeblock | EndLine)
	;

// Expressions
expr
	: literals
	| Identifier
	;
literals
	: IntLiteral
	| DoubleLiteral
	| StringLiteral
	;

// =============== //
// ==== Lexer ==== //
// =============== //

// Keywords
FuncKeyword
	: 'func'
	;

// Primitive types
VoidType
	: 'void'
	;
IntType
	: 'int'
	;
StringType
	: 'string'
	;

// Literals
IntLiteral
	: NumberFrag
	;
DoubleLiteral
	: DecimalNumberFrag
	;
StringLiteral
	: '"' .*? '"'
	;

// Literal fragments
fragment NumberFrag
	: [0-9]+
	;
fragment DecimalNumberFrag
	: [0-9]* '.' [0-9]+
	;

// Important
Identifier
	: [a-zA-Z_][a-zA-Z0-9_]*
	;
EndLine
	: ';'
	;

// Ignore
Whitespace
	: [ \t\r\n]+ -> skip
	;
LineComment
	: '//' ~[\r\n]* -> skip
	;
