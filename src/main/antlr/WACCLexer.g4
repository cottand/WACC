lexer grammar WACCLexer;

// Skip comments
COMMENT: '#'~[\n]* -> skip;

// Whitespace
WS: [ \n\t]+;

// Comparison
EQ: '==';
NOT_EQ: '!=';
GRT_EQ: '>=';
LESS_EQ: '<=';
GRT: '>';
LESS: '<';
AND: '&&';
OR: '||';

// Operators
ASSIGN: '=';
PLUS: '+';
MINUS: '-';
NOT: '!';
DIV: '/';
MUL: '*';
MOD: '%';
LEN: 'len';
CHR: 'chr';
ORD: 'ord';

FST: 'fst';
SND: 'snd';
CALL: 'call';
NEWPAIR: 'newpair';

// Types
PAIR: 'pair';
INT: 'int';
BOOL: 'bool';
CHAR: 'char';
STRING: 'string';

// Keywords
BEGIN: 'begin';
END: 'end';
SKP: 'skip'; // SKIP is reserved
READ: 'read';
FREE: 'free';
RETURN: 'return';
EXIT: 'exit';
PRINT: 'print';
PRINTLN: 'println';
IF: 'if';
FI: 'fi';
THEN: 'then';
ELSE: 'else';
WHILE: 'while';
DO: 'do';
DONE: 'done';
IS: 'is';

// Symbols
SEMICOLON: ';';
COMMA: ',';

// Brackets
LBRACKET: '(';
RBRACKET:  ')';
LSQBRACKET: '[';
RSQBRACKET: ']';

BOOL_LIT: TRUE | FALSE;
CHAR_LIT: QUOTE CHARACTER QUOTE;
STRING_LIT: DQUOTE CHARACTER* DQUOTE;
PAIR_LIT: NULL;

SIGN: PLUS | MINUS;
INTEGER: DIGIT+;

TRUE: 'true';
FALSE: 'false';
NULL: 'null';
QUOTE: '\'';
DQUOTE: '"';

ID: [a-zA-Z_][a-zA-Z0-9_]*;

CHARACTER: ~[\\'"] | ESC_CHARACTER;
ESC_CHARACTER: '\\0' | '\\b' | '\\t' | '\\n' | '\\f' | '\\r' | '\\"' | '\\\'' | '\\';
DIGIT: '0'..'9';
