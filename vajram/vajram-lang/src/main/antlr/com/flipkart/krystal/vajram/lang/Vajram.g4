grammar Vajram;

@header {
package com.flipkart.krystal.vajram.lang;
}

program
    : vajram_def EOF
    ;

vajram_def: package_decl imports_decl* vajram_visibility? type ID '(' input_decl? ')' permits? '{' (dependency)* output_logic'}' ;

package_decl: annotation* PACKAGE qualifiedName ';';

imports_decl: IMPORT qualifiedName ('.' '*')? ';';

qualifiedName: ID ('.' ID)*;

dependency: annotation* type FANOUT? ID ERRABLE? EQ ID '(' (dep_input_resolver SEMI)* dep_input_resolver? ')';

logic_block: annotation* completion_time '{' stat* return_stat '}';

lambda_block: (var_use (COMMA var_use)* '->' )? annotation* completion_time '{' stat* return_stat '}';

var_use: ID ERRABLE?;

completion_time: (SOON | LATER)?;

vajram_visibility: PUBLIC | PRIVATE;

input_decl: (annotation* input_id_declaration COMMA)* (annotation* input_id_declaration)?;

annotation: '@' ID;

permits: PERMITS ID (COMMA ID)*;

input_id_declaration: type ERRABLE? ID;

dep_input_resolver: dep_input_resolver_stat | dep_input_resolver_func;

dep_input_resolver_stat: (ID COMMA)* ID EQ FANOUT? (expr COMMA)* expr;
dep_input_resolver_func: (ID COMMA)* ID EQ FANOUT? '{' stat* return_stat '}';

output_logic : logic_block;

return_stat: (RETURN (expr COMMA)* expr SEMI | (expr COMMA)* expr);

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

expr: var_use
    | STRING_LITERAL
    | INT
    | bool
    | NOT expr
    | expr PLUS expr
    | func_chain
    | expr accessor ID
    | expr accessor func_chain
    | expr '::' ID
    | lambda_block
    | SPECIAL? func_call
    | NEW SPECIAL? func_call
    ;

accessor: (SOON | ERRABLE | DOT | SOON DOT | ERRABLE DOT | SOON ERRABLE DOT | SOON ERRABLE);

func_chain: (func_call accessor)* func_call;

func_call: ID ('(' ((expr COMMA)* expr COMMA?)? ')' | logic_block );

NOT : 'not' ;
EQ : '=' ;
PLUS : '+';
COMMA : ',' ;
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
SPECIAL : '#' ;

NEW : 'new' ;
RETURN : 'return' ;
THROW: 'throw';

PUBLIC: 'public';
PRIVATE: 'private';
PERMITS: 'permits' ;
PACKAGE: 'package';
IMPORT : 'import';

FANOUT : '*';
ERRABLE: '?';
SOON : '~';
LATER : '~~';
DOT: '.';

INT : [0-9]+ ;
TRUE : 'true' ;
FALSE : 'false' ;
STRING: 'string' ;
STRING_LITERAL:     '"' (~["\\\r\n])* '"';
ID: [a-zA-Z_][a-zA-Z_0-9]* ;
WS: [ \t\n\r\f]+ -> skip ;
