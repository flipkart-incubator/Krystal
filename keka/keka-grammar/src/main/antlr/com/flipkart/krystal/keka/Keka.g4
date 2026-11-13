grammar Keka;

@header {
package com.flipkart.krystal.keka;
}

program
    : keka_file
    ;

keka_file: package_decl imports_decl* top_level_file_element+ ;

top_level_file_element: vajram_def | type_def | model_def;

vajram_def : annotation* VAJRAM ID inputs_decl output_decl injection_decl? permissions? '{' (computed_facet)* output_block'}' ;

type_def : annotation_def;

// A model: all data is a model. A model with `idField fields is an entity (equality and hash use only
// those fields); otherwise it is a value. Fields are separated by `;` (a trailing `;` is allowed), may
// carry annotations and a constant default, and may be errable (`T?`: a value, an error or nil).
model_def: annotation* MODEL ID LPAREN model_field_list? RPAREN;

model_field_list: model_field (SEMI model_field)* SEMI?;

model_field: annotation* nowType ID (ASSIGN annotation_value)?;

// A custom annotation definition, e.g. `` `target(kinds = [VAJRAM]) `type table(string name, bool indexed = false) ``.
// Its parameters are compile-time constants (see annotation_value); a trailing comma is allowed and
// an annotation with no parameters is a marker annotation, and its parentheses are optional.
annotation_def: annotation* BACKTICK TYPE ID (LPAREN annotation_def_field_list? RPAREN)?;

annotation_def_field_list : (annotation_field COMMA)* annotation_field COMMA? ;

package_decl: annotation* PACKAGE qualifiedName SEMI;

imports_decl: IMPORT (VAJRAM | MODEL | TYPE | BACKTICK TYPE) ID FROM qualifiedName SEMI;

qualifiedName: ID ('.' ID)*;

computed_facet : dependency_declaration | field;

field : annotation* type FANOUT? ID ASSIGN expr SEMI;

dependency_declaration: annotation* type FANOUT? ID ASSIGN dependency_invocation ;

dependency_invocation : FANOUT? ID '(' (dep_input_resolver SEMI)* dep_input_resolver? ')' (ERRABLE func_call)? annotated_logic_block* SEMI ;

annotated_delegatable_logic_block: annotation* completion_time logic_block;

annotated_logic_block: annotation*  logic_block;

logic_block: '{' statement* yield_statement? '}';

lambda_block: (var_use (COMMA var_use)* '->' )? annotation* completion_time '{' statement* yield_statement '}';

var_use: ID ERRABLE?;

completion_time: (SOON | LATER)?;

visibility: PUBLIC | PRIVATE;

inputs_decl: IN ? '(' inputs_list? ')';

inputs_list : (input_declaration COMMA)* input_declaration ;

annotation_field: annotation* annotation_field_type ID (ASSIGN annotation_value)?;

// A batched input is marked with the `batched annotation, not a #grouper.
input_declaration: annotation* input_type ID;

injection_decl: 'inject' '(' injections_list?')';

output_decl: OUT annotation* type;

injections_list : (injection_declaration COMMA)* injection_declaration ;

injection_declaration: annotation* injection_type ID;

grouper: HASH ID;

annotation: BACKTICK ID annotation_param_list?;

annotation_param_list : '(' ((annotation_arg COMMA)* annotation_arg COMMA?)? ')' ;

annotation_arg: ID ASSIGN annotation_value;

// Annotation arguments and parameter defaults must be compile-time constants, since compiler plugins
// read them before anything runs: literals, arrays, `+`/`*` over constants, and named constants
// (enum constants, type references). No variables, calls or `new`. `*` binds tighter than `+`.
annotation_value
    : annotation_value MULTIPLY annotation_value
    | annotation_value PLUS annotation_value
    | annotation_value_atom
    ;

annotation_value_atom
    : STRING_LITERAL
    | NUM_LITERAL
    | bool
    | annotation_array
    | qualifiedName
    | LPAREN annotation_value RPAREN
    ;

annotation_array: '[' (annotation_value (COMMA annotation_value)* COMMA?)? ']';

permissions: PERMIT callers?;

callers: CALLERS (annotation* ID (COMMA annotation* ID)* | (annotation* PUBLIC));

nowType
    : non_param_type ('<' ((nowType COMMA)* nowType COMMA?)? '>')? ERRABLE?
    | '[' nowType ']'
    ;

type
    : non_param_type ('<' ((type COMMA)* type COMMA?)? '>')? completion_suffix?
    | '[' type ']'
    ;

input_type: nowType;

injection_type: nowType;

// Annotation parameters are constants: a primitive or string, or a list of those. (Enums and entities
// will be added here once the language can declare them.)
annotation_field_type: annotation_scalar_type | '[' annotation_field_type ']';

annotation_scalar_type: ID | STRING;

completion_suffix : ERRABLE | SOON | LATER ;

// `ID grouper` is a type generated for another vajram, e.g. `getFriends#req`.
non_param_type: ID grouper?
              | STRING
              | VOID
              | grouper
              ;

dep_input_resolver: dep_input_resolver_stat | dep_input_resolver_func;

dep_input_resolver_stat: (ID COMMA)* ID ASSIGN FANOUT? (expr COMMA)* expr;
dep_input_resolver_func: (ID COMMA)* ID ASSIGN FANOUT? '{' statement* yield_statement '}';

yield_statement: (YIELD (expr COMMA)* expr SEMI | (expr COMMA)* expr);

output_block: OUT? annotated_delegatable_logic_block | OUT dependency_invocation;

bool: TRUE | FALSE;
statement: assign_stat|throw_stat;

throw_stat: THROW expr SEMI;
assign_stat: (local_declaration ASSIGN)? expr SEMI;

// A local variable may carry a completion marker (e.g. `Map<K, V>~ x`), unlike an input.
local_declaration: type ID;

expr: var_use
    | STRING_LITERAL
    | NUM_LITERAL
    | bool
    | NOT expr
    | expr PLUS expr
    | expr MULTIPLY expr
    | expr IS_EQ expr
    | func_chain
    | expr accessor func_chain
    | expr accessor ID
    | expr '::' ID
    | HASH? func_call_in_output_logic
    | NEW HASH? func_call_in_output_logic
    | new_model
    | grouper
    | array_expr
    ;

// Creates a model by naming its fields, e.g. `new Money(amountMinor = 5; currency = usd)`.
new_model: NEW ID LPAREN model_arg (SEMI model_arg)* SEMI? RPAREN;

model_arg: ID ASSIGN expr;

array_expr: '[' (expr COMMA)* expr? ']';

accessor : (SOON | ERRABLE | DOT | SOON DOT | ERRABLE DOT | SOON ERRABLE DOT | SOON ERRABLE);

func_chain: (func_call_in_output_logic accessor)* func_call_in_output_logic;

func_call_in_output_logic: ID ( param_list | annotated_delegatable_logic_block );

func_call: ID (param_list | annotated_logic_block );

param_list : '(' ((expr COMMA)* expr COMMA?)? ')' ;

NOT : 'not' ;
ASSIGN : '=' ;
PLUS : '+';
MULTIPLY : '*';
COMMA : ',' ;
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
HASH : '#' ;

IS_EQ : 'eq' ;
VAJRAM : 'vajram' ;
TYPE : 'type' ;
MODEL : 'model' ;
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

FANOUT : '...';
ERRABLE: '?';
SOON : '~';
LATER : '~~';
DOT: '.';
BACKTICK: '`';

NUM_LITERAL : [0-9]+ ;
TRUE : 'true' ;
FALSE : 'false' ;
STRING: 'string' ;
VOID: 'void' ;
STRING_LITERAL:     '"' (~["\\\r\n])* '"';
ID: [_] | [a-zA-Z][a-zA-Z_0-9]* ;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
WS: [ \t\n\r\f]+ -> skip ;
