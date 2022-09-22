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
	| opAssign
	| return
	| breakpoint
	| if
	| while
	;
outerStatement
	: function
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
opAssign
	: Identifier '^=' expr EndLine
	| Identifier '*=' expr EndLine
	| Identifier '/=' expr EndLine
	| Identifier '+=' expr EndLine
	| Identifier '-=' expr EndLine
	| Identifier '%=' expr EndLine
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
	| '-' expr
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
	: IntegerLiteral
	| DecimalLiteral
	| StringLiteral
	| BooleanLiteral
	;

typeName
	: VoidType
	| StrType
	| IntType
	| BoolType
	| DecType
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
PowAssign: '^=';
MulAssign: '*=';
DivAssign: '/=';
AddAssign: '+=';
SubAssign: '-=';
ModAssign: '%=';
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
StrType: 'str';
BoolType: 'bool';
DecType: 'dec';

// Literals
IntegerLiteral
	: '-'? [0-9]+
	;
DecimalLiteral
	: '-'? [0-9]* '.' [0-9]+
	;
StringLiteral
	: '"' .*? '"'
	;
BooleanLiteral
	: 'true'
	| 'false'
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
