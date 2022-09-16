grammar Scope;

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
	| return
	;
outerStatement
	: function
	;

typeName
	: VoidType
	| StringType
	| IntType
	;

parameter
	: typeName Identifier
	;
parameters
	: (parameter (',' parameter)*)?
	;

arguments
	: (expr (',' expr)*)?
	;

// Inner statements
invoke
	: Identifier '(' arguments ')' EndLine
	;
declare
	: typeName Identifier '=' expr EndLine
	;
return
	: ReturnKeyword expr EndLine
	;

// Outer statements
function
	: FuncKeyword typeName Identifier '(' parameters ')' (codeblock | EndLine)
	;

// Expressions
expr
	: expr '^' expr
	| expr ('*' | '/' | '%') expr
	| expr ('+' | '-') expr
	| '(' expr ')'
	| atom
	;
atom
	: literals
	| Identifier '(' arguments ')'
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

// Symbols
LeftParen: '(';
RightParen: ')';
LeftBracket: '[';
RightBracket: ']';
LeftBrace: '{';
RightBrace: '}';
Assign: '=';
Pow: '^';
Mul: '*';
Div: '/';
Add: '+';
Sub: '-';
Mod: '%';

// Keywords
FuncKeyword: 'func';
ImportKeyword: 'import';
ReturnKeyword: 'ret';

// Primitive types
VoidType: 'void';
IntType: 'int';
StringType: 'string';

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
