parser grammar WACCParser;

options {
  tokenVocab=WACCLexer;
}

prog: WS* BEGIN WS+ func* WS* stat WS* END WS* EOF;

func: type WS+ ID WS* LBRACKET WS* param_list? WS* RBRACKET WS* IS WS* stat WS* END WS*;

stat: SKP #Skip
| type WS+ ID WS* ASSIGN WS* assign_rhs #Declare
| assign_lhs WS* ASSIGN WS* assign_rhs #Assign
| READ WS+ assign_lhs #ReadStat
| FREE WS+ expr #FreeStat
| RETURN WS+ expr #ReturnStat
| EXIT WS+ expr #ExitStat
| PRINT WS* expr #PrintStat
| PRINTLN WS+ expr #PrintlnStat
| IF WS* expr WS* THEN WS* stat WS* ELSE WS* stat WS* FI #IfElse
| WHILE WS* expr WS* DO WS* stat WS* DONE #WhileDo
| BEGIN WS* stat WS* END #NewScope
| <assoc=right> stat WS* SEMICOLON WS* stat #SemiColon
;

expr:
expr WS* MUL WS* expr #BinOp
| expr WS* DIV WS* expr #BinOp
| expr WS* MOD WS* expr #BinOp
| expr WS* PLUS WS* expr #BinOp
| expr WS* MINUS WS* expr #BinOp
| expr WS* GRT WS* expr #BinOp
| expr WS* GRT_EQ WS* expr #BinOp
| expr WS* LESS WS* expr #BinOp
| expr WS* LESS_EQ WS* expr #BinOp
| expr WS* EQ WS* expr #BinOp
| expr WS* NOT_EQ WS* expr #BinOp
| expr WS* AND WS* expr #BinOp
| expr WS* OR WS* expr #BinOp
| int_lit #IntLitExpr
| BOOL_LIT #BoolLitExpr
| CHAR_LIT #CharLitExpr
| STRING_LIT #StrLitExpr
| PAIR_LIT #PairLitExpr
| ID #IdentExpr
| array_elem #ArrayElemExpr
| unary_op WS* expr # UnOpExpr
| LBRACKET WS* expr WS* RBRACKET #NestedExpr
;

// Assignments
assign_lhs:
ID #LHSIdent
| array_elem #LHSArrayElem
| pair_elem #LHSPairElem
;
assign_rhs:
expr #RHSExpr
| array_lit #RHSArrayLit
| NEWPAIR WS* LBRACKET WS* expr WS* COMMA WS* expr WS* RBRACKET #RHSNewpair
| pair_elem #RHSPairElem
| CALL WS+ ID WS* LBRACKET WS* (arg_list)? WS* RBRACKET #RHSFuncCall
;

// Param & args
param_list: param WS* (COMMA WS* param)*;
param: type WS+ ID;

arg_list: expr WS* (COMMA WS* expr)*;

// Types
type: base_type | array_type | pair_type;
base_type:
INT #IntBaseT
| BOOL #BoolBaseT
| CHAR #CharBaseT
| STRING #StringBaseT
;

array_type:
 base_type WS* LSQBRACKET WS* RSQBRACKET #ArrayOfBaseT
| pair_type WS* LSQBRACKET WS* RSQBRACKET #ArrayOfPairs
| array_type WS* LSQBRACKET WS* RSQBRACKET #ArrayOfArrays
;
array_elem: ID WS* (LSQBRACKET WS* expr WS* RSQBRACKET)+;

pair_type: PAIR WS* LBRACKET WS* pair_elem_type WS* COMMA WS* pair_elem_type WS* RBRACKET;

pair_elem_type:
 base_type #BaseTPairElem
 | array_type  #ArrayPairElem
 | PAIR #PairPairElem
 ;
pair_elem: FST WS+ expr | SND WS+ expr;

// Literals
array_lit: LSQBRACKET WS* (expr WS* (COMMA WS* expr WS*)*)? WS* RSQBRACKET;
//int_lit: int_sign? WS* INTEGER;
//
//int_sign: PLUS | MINUS;
int_lit: (PLUS | MINUS)? WS* INTEGER;

unary_op: NOT
| MINUS
| LEN
| ORD
| CHR;
