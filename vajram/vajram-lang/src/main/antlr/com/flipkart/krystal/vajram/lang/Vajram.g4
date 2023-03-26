grammar Vajram;

@header {
package com.flipkart.krystal.vajram.lang;
}

program
    : vajram_def EOF
    ;

vajram_def : vajram_visibility? type ID '(' input_decl? ')' permits? '{' (dependency | async_dep)* vajram_logic'}' ;

async_dep: type ID EQ block;

block: '{' stat* return_stat '}';

vajram_visibility: PUBLIC | PRIVATE;

input_decl: ((input_qualifier)* input_id_declaration COMMA)? input_qualifier* input_id_declaration;

input_qualifier: APP | CLIENT | MOD;

permits: PERMITS ID (COMMA ID)*;

input_id_declaration: type OPTIONAL? ID;

dependency: type FANOUT? ID (OPTIONAL)? EQ ID '(' (dep_input_resolver SEMI)* dep_input_resolver? ')';

dep_input_resolver: dep_input_resolver_stat | dep_input_resolver_func;

dep_input_resolver_stat: (ID COMMA)* ID EQ FANOUT? (expr COMMA)* expr;
dep_input_resolver_func: (ID COMMA)* ID EQ FANOUT? '{' stat* return_stat '}';

vajram_logic : block;

return_stat: RETURN (expr COMMA)* expr SEMI;


type:
    | non_param_type ('<' ((type COMMA)* type COMMA?)? '>')?;

non_param_type: ID
              | INT
              | bool
              | STRING
              ;

bool: TRUE | FALSE;
stat: assign_stat|throw_stat;

throw_stat: THROW expr SEMI;
assign_stat: input_id_declaration EQ expr SEMI;

expr: ID (OPTIONAL)?
    | STRING_LITERAL
    | INT
    | bool
    | NOT expr
    | expr AND expr
    | expr OR expr
    | expr PLUS expr
    | func_chain
    | expr (OPTIONAL | DOT ) ID
    | expr (OPTIONAL | DOT) func_chain
    | expr '::' ID
    | block
    ;

func_chain: (func_call (OPTIONAL | DOT))* func_call;

func_call: ID '(' ((expr COMMA)* expr COMMA?)? ')';

AND : 'and' ;
OR : 'or' ;
NOT : 'not' ;
EQ : '=' ;
PLUS : '+';
COMMA : ',' ;
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;

DEP    : 'dep' ;
OUT    : 'out' ;
RETURN : 'return' ;
THROW: 'throw';

PUBLIC: 'public';
PRIVATE: 'private';
PERMITS: 'permits' ;
APP    : 'app' ;
CLIENT : 'client' ;
MOD : 'mod' ;

FANOUT : '*';
OPTIONAL: '?';
FUTURE : '~';
DOT: '.';

INT : [0-9]+ ;
TRUE : 'true' ;
FALSE : 'false' ;
STRING: 'string' ;
STRING_LITERAL:     '"' (~["\\\r\n])* '"';
ID: [a-zA-Z_][a-zA-Z_0-9]* ;
WS: [ \t\n\r\f]+ -> skip ;

