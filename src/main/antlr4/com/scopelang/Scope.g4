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
	| assign
	| return
	| breakpoint
	| if
	| while
	;
outerStatement
	: function
	;

typeName
	: VoidType
	| StringType
	| IntType
	| BoolType
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
assign
	: Identifier '=' expr EndLine
	;
return
	: ReturnKeyword expr EndLine
	;
breakpoint
	: BreakpointKeyword EndLine
	;
if
	: IfKeyword '(' expr ')' codeblock else?
	;
else
	: ElseKeyword codeblock
	;
while
	: WhileKeyword '(' expr ')' codeblock
	;

// Outer statements
function
	: FuncKeyword typeName Identifier '(' parameters ')' (codeblock | EndLine)
	;

// Expressions
expr
	: expr '[' expr ']'
	| expr '^' expr
	| expr ('*' | '/' | '%') expr
	| expr ('+' | '-') expr
	| expr ('==' | '!=' | '>' | '<') expr
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
	| BooleanLiteral
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
Equals: '==';
NotEquals: '!=';
GreaterThan: '>';
LessThan: '<';

// Keywords
FuncKeyword: 'func';
ImportKeyword: 'import';
ReturnKeyword: 'ret';
BreakpointKeyword: 'breakpoint';
IfKeyword: 'if';
ElseKeyword: 'else';
WhileKeyword: 'while';
ForKeyword: 'for';

// Primitive types
VoidType: 'void';
IntType: 'int';
StringType: 'str';
BoolType: 'bool';

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
BooleanLiteral
	: 'true'
	| 'false'
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
