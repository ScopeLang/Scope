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
	| break EndLine
	| continue EndLine
	| assembly
	;
outerStatement
	: function
	| const
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
	: Identifier ('[' expr ']')* '=' expr
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
break
	: BreakKeyword+ ContinueKeyword?
	;
continue
	: ContinueKeyword
	;
assembly
	: AssemblyBlock
	;

// Outer statements
function
	: FuncKeyword typeName Identifier '(' parameters ')' (codeblock | EndLine)
	;
const
	: ConstKeyword typeName Identifier '=' literals EndLine
	;

// Top statements
namespace
	: NamespaceKeyword fullIdent EndLine
	;
using
	: UsingKeyword fullIdent EndLine
	;

// Expressions
expr
	: expr '.' Identifier
	| expr '[' expr ']'
	| ('!' | '-') expr
	| expr '->' typeName
	| expr '^' expr
	| expr ('*' | '/' | '%') expr
	| expr ('+' | '-') expr
	| expr ('==' | '!=' | '>' | '<' | '>=' | '<=' | IsKeyword) expr
	| expr ('&' | '|') expr
	| '(' expr ')'
	| arrayInit
	| objectInit
	| atom
	;
atom
	: literals
	| fullIdent '(' arguments ')'
	| fullIdent
	;
literals
	: IntegerLiteral
	| DecimalLiteral
	| StringLiteral
	| BooleanLiteral
	;
arrayInit
	: typeName '{' arguments '}'
	;
objectInit
	: typeName '(' arguments ')'
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
	: typeName '[' ']'
	| primitiveType
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
Not: '!';
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
GreaterThanEqual: '>=';
LessThanEqual: '<=';
And: '&';
Or: '|';
Cast: '->';
Where: ':';
Range: '..';
Resolve: '::';
Access: '.';

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
BreakKeyword: 'break';
ContinueKeyword: 'continue';
IsKeyword: 'is';
ConstKeyword: 'const';

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
	| '-'? 'infinity'
	| 'nan'
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