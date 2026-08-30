# Vajram-Lang Specification

## Vajram Language File

* A vajram language file has the extension ".vajram"
* The file starts with an optional [edition](#edition) number(
  `edition ...`)
  which pins down the default semantics of the code written in the file.
* This is followed by optionally declaring the namespace that the file belongs to.
* This is followed by optional [import](#import) declarations
  where other vajrams, operators, and entities etc. are imported to be used in the files.
* This is followed by one or more [vajram definitions](#vajram)

## Language Constructs

### Edition

TODO

### Import

### Vajram

A vajram is the basic unit of work in vajram-lang. The closest analog t a traditional programming construct is a function. But to be more precise, a vajram provides a function-like interface to its callers - it accepts inputs and produces an output. But underneath, it is a collection of functions that may execute independently to orchestrate the computation of the final response. A vajram definition has the following parts

* The
  `vajram` keyword.
* The name of the vajram. This is used (along with the namespace) to refer to this vajram elsewhere.
  The name of the vajram can be preceded by zero or more [annotations](#annotations)
* [Vajram inputs](#vajram-inputs): One or more inputs that the vajram accepts. These have an
  optional
  `in` keyword followed by braces in which a comma seperated list of inputs are defined.
  Each input has a data type and name. Each input declaration can be preceded by zero or
  more [annotations](#annotations).
* [Vajram output](#vajram-output): every vajram defines the type of its output using the
  `out`
  keyword followed by
  the type of the output. Vajrams which don't return anything use the
  `void` keyword as the type.
  The type can be [annotated](#annotations).
* [Vajram injections](#vajram-injections): A vajram may declare injections - values that it wants
  the runtime to provide every time the vajram is executed.
* [Vajram Permissions](#vajram-permissions): A vajram may declare permissions using the
  `permit`
  keyword followed by different types of permissions. There are two main types of permissions -
  `callers` and
  `impls`.
  `callers` is followed either by
  `public` which means any other vajram can
  call it or a list of vajrams which are allowed to call it.
  `impls` permission is only applicable
  to [vajram traits](#vajram-trait) - it either declares
  `public` - anyone can implement it, or a
  list of vajrams which are allowed to implement it.
* Vajram control block: The vajram control block is enclosed in curly braces
  `{}`. A vajram control
  block contains a list of zero or more dependency facets followed by a single output block.
* Vajram dependency: A vajram dependency declares that a vajram depends on another vajram's output and assigns that output to a
  `dependency facet` - a variable which can be used elsewhere in the vajram.
* Output block: The output block in enclosed in a set of
  `{}` prefixed with the keyword
  `out` and can have arbitrary logic which computes the output of the vajram.

### Annotations

Annotations in vajrams are written using the
`` ` `` character followed by the name of the
annotation provided they have been imported in the file.

### Vajram inputs

### Vajram injections

### Vajram permissions

### The dependency Facet

A vajram dependency declares that a vajram depends on another vajram's output and assigns that output to a
`dependency facet` - a variable which can be used elsewhere in the vajram. Each dependency facet is a typed, named variable whose value
is bound to a vajram invocation. The invoked vajram is called a dependency of the current vajram.
A vajram dependency is declared like this:
`
FacetType facetName = 
  dependencyVajram(input1 = {}, input2, input3 = {});
`.
This declaration specifies that
`dependencyVajram` needs to be invoked and the resulting value
needs to be made available in the facet
`facetName` for consumption elsewhere in the vajram. The
inputs of the dependency in computed in the code blocks called "input resolvers" or just "
resolvers". An input resolver can compute the value of one or more inputs. The resolvers can have
arbitrary code which access any other facets defined in the vajram which have been defined before
the current facet - This prevents cyclic facet consumption. Resolvers cannot invoke other vajrams
from inside the block.

### Vajram trait

### Vajram output

The output block in enclosed in a set of `{}` with the prefixed with the keyword
`out` and can have arbitrary logic which computes the output of the vajram. It can refer to any facet of the vajram including its inputs, injections and dependencies. The output block is the only code in a vajram which is allowed to exit the current execution thread - only in a non-blocking way (remember - no code in vajram lang can ever block). This act of exiting the current thread and returning a placeholder value is called "delegation" (This is analogous to the "CompletableFuture" in Java, and "Promise" in javascript). If the delegated computation is designed to finish within the current application lifetime, it is called a "soon" output depicted by a single
`~` - this is called "sync delegation". If the delegated computation is designed to finish at a much later time and the computation could resume from them in a different applcation, it is called a "later" output depicated by two
`~~` - this is called "async delegation". The output logic cannot invoke another vajram from inside the code block.

### Output Logic Delegation mode

The [output logic](#vajram-output) of a vajram can have exactly one of three delegation modes - NONE, SYNC, ASYNC.
`out {}` implies NONE i.e the logic returns a now value,
`out ~{}` implies SYNC i.e. the logic returns a soon value,
`out ~~{}` implies ASYNC i.e. the logic returns a later value.

### Vajram Call Graph Delegation Mode

The vajram itself can have one of three delegation modes. This is declared by the vajram in this way:
`` `callGraphDelegationMode("NONE") `` which means this vajram and all of its immediate and transitive dependencies have [output logic delegation mode](#output-logic-delegation-mode) "NONE".
`` `callGraphDelegationMode("SYNC") `` which means this vajram and all of its immediate and transitive dependencies either have [output logic delegation mode](#output-logic-delegation-mode) as "NONE" or "SYNC".
`` `callGraphDelegationMode("ASYNC") `` which means this vajram and all of its immediate and transitive dependencies either have [output logic delegation mode](#output-logic-delegation-mode) as "NONE", "SYNC" or "ASYNC".

This implies that a vajram with callGraphDelegationMode "NONE" cannot directly or transitively depend on a vajram with callGraphDelegationMode "SYNC" or "ASYNC". Similarly and vajram with callGraphDelegationMode "SYNC" cannot directly or transitively depend on a vajram with callGraphDelegationMode "ASYNC".

If the vajram does not specify this annotation, then the value is auto-computed from the vajram's definition and its dependency graph.


