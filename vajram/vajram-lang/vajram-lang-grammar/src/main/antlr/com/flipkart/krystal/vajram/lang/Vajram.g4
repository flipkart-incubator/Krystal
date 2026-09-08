grammar Vajram;

@header {
package com.flipkart.krystal.vajram.lang;
}

program
    : vajram_file
    ;

vajram_file: package_decl imports_decl* vajram_def+ ;

vajram_def : VAJRAM ID inputs_decl output_decl injection_decl? permissions? '{' (computed_facet)* output_block'}' ;

package_decl: annotation* PACKAGE qualifiedName SEMI;

imports_decl: IMPORT VAJRAM ID FROM qualifiedName ('.' '*')? SEMI;

qualifiedName: ID ('.' ID)*;

computed_facet : dependency | field;

field : annotation* type FANOUT? ID EQ expr SEMI;

dependency: annotation* type FANOUT? ID EQ dependency_invocation ;

dependency_invocation : FANOUT? ID '(' (dep_input_resolver SEMI)* dep_input_resolver? ')' (ERRABLE func_call)? annotated_logic_block* SEMI ;

annotated_delegatable_logic_block: annotation* completion_time logic_block;

annotated_logic_block: annotation*  logic_block;

logic_block: '{' statement* yield_statement? '}';

lambda_block: (var_use (COMMA var_use)* '->' )? annotation* completion_time '{' statement* yield_statement '}';

var_use: ID ERRABLE?;

completion_time: (SOON | LATER)?;

visibility: PUBLIC | PRIVATE;

inputs_decl: IN ? '(' inputs_list? ')';

inputs_list : ( grouper? annotation* input_id_declaration COMMA)* ( grouper? annotation* input_id_declaration) ;

injection_decl: 'inject' '(' injections_list?')';

output_decl: OUT (errableType);

injections_list : ( annotation* injection_id_declaration COMMA)* ( annotation* injection_id_declaration) ;

grouper: SPECIAL ID;

annotation: '`' ID param_list?;

permissions: PERMIT callers?;

callers: CALLERS (annotation* ID (COMMA annotation* ID)* | (annotation* PUBLIC));

input_id_declaration: errableType ID;

injection_id_declaration: errableType ID;

errableType : type ERRABLE? ;

dep_input_resolver: dep_input_resolver_stat | dep_input_resolver_func;

dep_input_resolver_stat: (ID COMMA)* ID EQ FANOUT? (expr COMMA)* expr;
dep_input_resolver_func: (ID COMMA)* ID EQ FANOUT? '{' statement* yield_statement '}';

yield_statement: (YIELD (expr COMMA)* expr SEMI | (expr COMMA)* expr);

output_block: OUT? annotated_delegatable_logic_block | OUT dependency_invocation;

type:
    | non_param_type ('<' ((type COMMA)* type COMMA?)? '>')? ERRABLE? SOON?;

non_param_type: ID
              | STRING
              | VOID
              | grouper
              ;

bool: TRUE | FALSE;
statement: assign_stat|throw_stat;

throw_stat: THROW expr SEMI;
assign_stat: input_id_declaration EQ expr SEMI;

expr: var_use
    | STRING_LITERAL
    | INT_LITERAL
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
    | array_expr
    ;

array_expr: '[' (expr COMMA)* expr? ']';

accessor : (SOON | ERRABLE | DOT | SOON DOT | ERRABLE DOT | SOON ERRABLE DOT | SOON ERRABLE);

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

VAJRAM : 'vajram' ;
NEW : 'new' ;
IN: 'in';
OUT: 'out';
THROW: 'throw';
YIELD: 'yield';
FROM: 'from';

PUBLIC: 'public';
PRIVATE: 'private';
PERMIT: 'permit';
CALLERS: 'callers' ;
PACKAGE: 'package';
IMPORT: 'import';

FANOUT : '*';
ERRABLE: '?';
SOON : '~';
LATER : '~~';
DOT: '.';

INT_LITERAL : [0-9]+ ;
TRUE : 'true' ;
FALSE : 'false' ;
STRING: 'string' ;
VOID: 'void' ;
STRING_LITERAL:     '"' (~["\\\r\n])* '"';
ID: [a-zA-Z_][a-zA-Z_0-9]* ;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
WS: [ \t\n\r\f]+ -> skip ;
