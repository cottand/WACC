parser grammar WACCParser;

options {
  tokenVocab=WACCLexer;
}

prog: BEGIN func* stat END;

func: type ID LBRACKET param_list? RBRACKET IS stat END;

stat: SKP
| type ID ASSIGN assign_rhs
| assign_lhs ASSIGN assign_rhs
| READ assign_lhs
| FREE expr
| RETURN expr
| EXIT expr
| PRINT expr
| PRINTLN expr
| IF expr THEN stat ELSE stat FI
| WHILE expr DO stat DONE
| BEGIN stat END
| stat SEMICOLON stat;

expr: INT_LIT
| BOOL_LIT
| CHAR_LIT
| STRING_LIT
| PAIR_LIT
| ID
| array_elem
| unary_op expr
| expr binary_op expr
| LBRACKET expr RBRACKET;

// Assignments
assign_lhs: ID | array_elem | pair_elem;
assign_rhs: expr
| array_lit
| NEWPAIR LBRACKET expr COMMA expr RBRACKET
| pair_elem
| CALL ID LBRACKET (arg_list)? RBRACKET;

// Param & args
param_list: param (COMMA param)*;
param: type ID;

arg_list: expr (COMMA expr)*;

// Types
type: base_type | array_type | pair_type;
base_type: INT | BOOL | CHAR | STRING;

array_type: (base_type | pair_type) LSQBRACKET RSQBRACKET | array_type LSQBRACKET RSQBRACKET;
array_elem: ID (LSQBRACKET expr RSQBRACKET)+;

pair_type: PAIR LBRACKET pair_elem_type COMMA pair_elem_type RBRACKET;
pair_elem_type: base_type | array_type | PAIR;
pair_elem: FST expr | SND expr;

array_lit: LSQBRACKET (expr (COMMA expr)*)? RSQBRACKET;

// Operators
unary_op: NOT
| MINUS
| LEN
| ORD
| CHR;

binary_op: MUL
| DIV
| MOD
| PLUS
| MINUS
| GRT
| GRT_EQ
| LESS
| LESS_EQ
| EQ
| NOT_EQ
| AND
| OR;
