grammar Vajram;

@header {
package com.flipkart.krystal.vajram.lang;
}

program
    : vajram_def EOF
    ;

vajram_def: package_decl imports_decl* vajram_visibility? type ERRABLE? ID '(' input_decl? ')' permits? '{' (dependency)* annotated_delegatable_logic_block?'}' ;

package_decl: annotation* PACKAGE qualifiedName ';';

imports_decl: IMPORT qualifiedName ('.' '*')? ';';

qualifiedName: ID ('.' ID)*;

dependency: annotation* type FANOUT? ID EQ FANOUT? ID '(' (dep_input_resolver SEMI)* dep_input_resolver? ')' (ERRABLE func_call)? annotated_logic_block* SEMI ;

annotated_delegatable_logic_block: annotation* completion_time logic_block;

annotated_logic_block: annotation*  logic_block;

logic_block: '{' statement* return_statement? '}';

lambda_block: (var_use (COMMA var_use)* '->' )? annotation* completion_time '{' statement* return_statement '}';

var_use: ID ERRABLE?;

completion_time: (SOON | LATER)?;

vajram_visibility: PUBLIC | PRIVATE;

input_decl: ( grouper? annotation* input_id_declaration COMMA)* ( grouper? annotation* input_id_declaration)?;

grouper: SPECIAL ID;

annotation: '@' ID param_list?;

permits: PERMITS ID (COMMA ID)*;

input_id_declaration: type ERRABLE? ID;

dep_input_resolver: dep_input_resolver_stat | dep_input_resolver_func;

dep_input_resolver_stat: (ID COMMA)* ID EQ FANOUT? (expr COMMA)* expr;
dep_input_resolver_func: (ID COMMA)* ID EQ FANOUT? '{' statement* return_statement '}';

return_statement: (RETURN (expr COMMA)* expr SEMI | (expr COMMA)* expr);

type:
    | non_param_type ('<' ((type COMMA)* type COMMA?)? '>')? ERRABLE? SOON?;

non_param_type: ID
              | INT
              | bool
              | STRING
              | grouper
              ;

bool: TRUE | FALSE;
statement: assign_stat|throw_stat;

throw_stat: THROW expr SEMI;
assign_stat: input_id_declaration EQ expr SEMI;

expr: var_use
    | STRING_LITERAL
    | INT
    | bool
    | NOT expr
    | expr PLUS expr
    | expr IS_EQ expr
    | func_chain
    | expr accessor ID
    | expr accessor func_chain
    | expr '::' ID
    | SPECIAL? func_call_in_output_logic
    | NEW SPECIAL? func_call_in_output_logic
    | grouper
    ;

accessor: (SOON | ERRABLE | DOT | SOON DOT | ERRABLE DOT | SOON ERRABLE DOT | SOON ERRABLE);

func_chain: (func_call_in_output_logic accessor)* func_call_in_output_logic;

func_call_in_output_logic: ID ( param_list | annotated_delegatable_logic_block );

func_call: ID (param_list | annotated_logic_block );

param_list : '(' ((expr COMMA)* expr COMMA?)? ')' ;

NOT : 'not' ;
EQ : '=' ;
IS_EQ : '==' ;
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
IMPORT: 'import';

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