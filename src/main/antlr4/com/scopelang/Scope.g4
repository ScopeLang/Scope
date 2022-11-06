grammar Scope;

// ================ //
// ==== Parser ==== //
// ================ //

program // First node
	: topStatements outerStatments EOF
	;

code
	: (innerStatement | codeblock)*
	;
codeblock
	: '{' code '}'
	;

topStatements
	: (topStatement)*
	;
outerStatments
	: (outerStatement)*
	;

innerStatement
	: invoke EndLine
	| declare EndLine
	| assign EndLine
	| opAssign EndLine
	| return EndLine
	| breakpoint EndLine
	| if
	| while
	| for
	| assembly
	;
outerStatement
	: function
	;
topStatement
	: ImportKeyword StringLiteral EndLine
	| namespace
	| using
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
	: fullIdent '(' arguments ')'
	;
declare
	: typeName Identifier '=' expr
	;
assign
	: Identifier '=' expr
	;
opAssign
	: Identifier '^=' expr
	| Identifier '*=' expr
	| Identifier '/=' expr
	| Identifier '+=' expr
	| Identifier '-=' expr
	| Identifier '%=' expr
	;
return
	: ReturnKeyword expr
	;
breakpoint
	: BreakpointKeyword
	;
if
	: IfKeyword '(' expr ')' codeblock else?
	;
else
	: ElseKeyword (if | codeblock)
	;
while
	: WhileKeyword '(' expr ')' codeblock
	;
for
	: ForKeyword '(' typeName Identifier ':' expr '..' expr (StepKeyword expr)? ')' codeblock
	;
assembly
	: AssemblyBlock
	;

// Outer statements
function
	: FuncKeyword typeName Identifier '(' parameters ')' (codeblock | EndLine)
	;
namespace
	: NamespaceKeyword fullIdent EndLine
	;
using
	: UsingKeyword fullIdent EndLine
	;

// Expressions
expr
	: expr '[' expr ']'
	| '-' expr
	| expr '->' typeName
	| expr '^' expr
	| expr ('*' | '/' | '%') expr
	| expr ('+' | '-') expr
	| expr ('==' | '!=' | '>' | '<') expr
	| '(' expr ')'
	| atom
	;
atom
	: literals
	| fullIdent '(' arguments ')'
	| typeName '[' expr ']'
	| Identifier
	;
literals
	: IntegerLiteral
	| DecimalLiteral
	| StringLiteral
	| BooleanLiteral
	;

fullIdent
	: Identifier ('::' Identifier)*
	;
primitiveType
	: VoidType
	| StrType
	| IntType
	| BoolType
	| DecType
	;
typeName
	: primitiveType ('[' ']')?
	;

// =============== //
// ==== Lexer ==== //
// =============== //

// Assembly block
AssemblyBlock: 'assembly' [ \t\r\n]* '{' .*? '}';

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
Cast: '->';
Where: ':';
Range: '..';
Resolve: '::';

// Keywords
FuncKeyword: 'func';
ImportKeyword: 'import';
ReturnKeyword: 'ret';
BreakpointKeyword: 'breakpoint';
IfKeyword: 'if';
ElseKeyword: 'else';
WhileKeyword: 'while';
ForKeyword: 'for';
StepKeyword: 'step';
NamespaceKeyword: 'namespace';
UsingKeyword: 'using';

// Primitive types
VoidType: 'void';
IntType: 'int';
StrType: 'str';
BoolType: 'bool';
DecType: 'dec';

// Literals
IntegerLiteral
	: '-'? [0-9']+
	;
DecimalLiteral
	: '-'? [0-9']* '.' [0-9]+
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
BlockComment
	: '/*' .*? '*/' -> skip
	;
DocComment
	: '/%' .*? '%/' -> skip
	;