parser grammar WACCParser;

options {
  tokenVocab=WACCLexer;
}

prog: WS* BEGIN WS+ func* WS* stat WS* END WS* EOF;

func: type WS+ ID WS* LBRACKET WS* param_list? WS* RBRACKET WS* IS WS* stat WS* END WS*;

stat: SKP
| type WS+ ID WS* ASSIGN WS* assign_rhs
| assign_lhs WS* ASSIGN WS* assign_rhs
| READ WS+ assign_lhs
| FREE WS+ expr
| RETURN WS+ expr
| EXIT WS+ expr
| PRINT WS* expr
| PRINTLN WS+ expr
| IF WS* expr WS* THEN WS* stat WS* ELSE WS* stat WS* FI
| WHILE WS* expr WS* DO WS* stat WS* DONE
| BEGIN WS* stat WS* END
| <assoc=right> stat WS* SEMICOLON WS* stat;

expr:
expr WS* MUL WS* expr
| expr WS* DIV WS* expr
| expr WS* MOD WS* expr
| expr WS* PLUS WS* expr
| expr WS* MINUS WS* expr
| expr WS* GRT WS* expr
| expr WS* GRT_EQ WS* expr
| expr WS* LESS WS* expr
| expr WS* LESS_EQ WS* expr
| expr WS* EQ WS* expr
| expr WS* NOT_EQ WS* expr
| expr WS* AND WS* expr
| expr WS* OR WS* expr
| int_lit
| BOOL_LIT
| CHAR_LIT
| STRING_LIT
| PAIR_LIT
| ID
| array_elem
| unary_op WS* expr
| LBRACKET WS* expr WS* RBRACKET;

// Assignments
assign_lhs: ID | array_elem | pair_elem;
assign_rhs: expr
| array_lit
| NEWPAIR WS* LBRACKET WS* expr WS* COMMA WS* expr WS* RBRACKET
| pair_elem
| CALL WS+ ID WS* LBRACKET WS* (arg_list)? WS* RBRACKET;

// Param & args
param_list: param WS* (COMMA WS* param)*;
param: type WS+ ID;

arg_list: expr WS* (COMMA WS* expr)*;

// Types
type: base_type | array_type | pair_type;
base_type: INT | BOOL | CHAR | STRING;

array_type: (base_type | pair_type) WS* LSQBRACKET WS* RSQBRACKET | array_type WS* LSQBRACKET WS* RSQBRACKET;
array_elem: ID WS* (LSQBRACKET WS* expr WS* RSQBRACKET)+;

pair_type: PAIR WS* LBRACKET WS* pair_elem_type WS* COMMA WS* pair_elem_type WS* RBRACKET;

pair_elem_type: base_type | array_type | PAIR;
pair_elem: FST WS+ expr | SND WS+ expr;

// Literals
array_lit: LSQBRACKET WS* (expr WS* (COMMA WS* expr WS*)*)? WS* RSQBRACKET;
//int_lit: int_sign? WS* INTEGER;
//
//int_sign: PLUS | MINUS;
int_lit: PLUS? WS* INTEGER;

unary_op: NOT
| MINUS
| LEN
| ORD
| CHR;
