# Vajram-Lang Specification

## Status And Scope

This document specifies Vajram-Lang V1 semantics. It defines the observable behavior a conforming parser, validator, interpreter, or compiler must preserve. It deliberately does not prescribe a target language, runtime, memory-management mechanism, or code-generation strategy.

A **Vajram** is a named unit of computation with declared inputs, optional injections, dependencies, and one output. A source file contains one or more Vajrams that share its package declaration and imports. A compilation compiles every `.vajram` file below one source root as one program.

## Syntax

The normative syntax is defined by [Vajram.g4](vajram-lang-grammar/src/main/antlr/com/flipkart/krystal/vajram/lang/Vajram.g4). This specification defines the meaning of constructs accepted by that grammar; it does not duplicate the grammar.

In particular, the grammar defines packages, declarations, types, expressions, annotations, output blocks, dependencies, fanout, error accessors, completion markers, and logic blocks. An import binds a local Vajram name to a source module, for example `import vajram readFileAsString from lang.FileSystem;`.

## Names, Packages, And Callers

A package identifies the logical module containing its Vajram. Package segments are preserved by the language and are available to a backend when organizing emitted artifacts.

V1 resolves dependencies by Vajram name over the complete compilation root. Every dependency invocation must name exactly one Vajram in that root. Duplicate Vajram names are errors, including names declared in distinct packages. Imports document the intended dependency surface but do not alter V1 name resolution.

The `permit callers` declaration controls cross-Vajram access. `permit callers public` allows every Vajram to invoke the declaration. `permit callers a, b` allows only Vajrams named `a` or `b`. Without a `permit callers` declaration, V1 permits every Vajram to invoke it. A caller violation is an error.

Annotations attach to each caller entry. For example, `permit callers `outsideProcess public` annotates the public caller, while `permit callers `audit callerA, `internal callerB` separately annotates each named caller. An `outsideProcess` annotation on `public` declares that a public Vajram is eligible for an external-process entrypoint. Annotation processors define the behavior of annotations; unsupported annotations must be preserved or diagnosed.

## Types And Values

`string`, `int`, and `void` are built-in value types. Other identifiers name application-defined types. Generic type arguments are structural.

`T?` is an **errable value**. It represents either a `T` or a `VajramError`; it is not an optional value. `nil` is the canonical nil-error value.

An errable value provides this conceptual interface:

| Operation | Meaning |
| --- | --- |
| `valuePresent()` | A value exists. |
| `valueAbsent()` | The inverse of `valuePresent()`. |
| `errorPresent()` | An error exists. |
| `errorAbsent()` | The inverse of `errorPresent()`. |
| `isNil()` | Both the value and error are absent. |
| `value()` | Returns the value when present. |
| `error()` | Returns the error when present. |
| `default(fallback)` | Returns the value when present, otherwise `fallback`. |

The `?` suffix on a variable preserves its errable surface. The `?` in an accessor selects this error-aware method surface. A compiler must not silently convert an errable value into a successful value.

`T~` is a type-level soon marker accepted by the grammar. Completion of a Vajram itself is determined by its output block, not by its declared output type.

## Completion And Non-Blocking Execution

No Vajram-Lang operation may block its executing context. Completion is declared on an inline output block:

| Output form | Completion | Meaning |
| --- | --- | --- |
| `{ ... }` | now | Has no explicit asynchronous delegation. |
| `~ { ... }` | soon | Produces its output asynchronously. |
| `~~ { ... }` | later | Produces its output asynchronously at a later lifecycle. |

A delegating output inherits the completion of the Vajram it delegates to. Completion markers do not restrict which Vajrams may invoke one another: a now, soon, or later Vajram may invoke a Vajram with any completion. A backend must schedule asynchronous invocations without blocking the caller; it may lift a caller to an asynchronous execution context when required by its dependency graph.

An invocation of an asynchronous dependency must be scheduled without blocking the caller's execution context. Fanout invocations schedule all element invocations concurrently and collect their results. A successful fanout preserves input iteration order. An errable fanout propagates an element error according to the backend's documented aggregation rule.

## System Vajrams

System Vajrams are supplied by the language environment and do not require a source definition. `readFileAsString(path = filePath)` is a soon System Vajram that reads the complete file at `path`, decodes it as UTF-8, and returns a `string`. A file read or UTF-8 decoding failure is an invocation failure. `concatStrings(strings = values, separator = separator)` is a now System Vajram that joins the ordered `string` values in `values` into one `string`, inserting `separator` between adjacent values.

## Fanout Dependencies

A fanout dependency invokes one Vajram once for every element of an input sequence. Fanout is declared with `*` on the dependency, invocation, or resolver. The resolver marked `=*` supplies the per-invocation input value.

```vajram
string* fileContent =* headFile(numChars = 100, filePath =* files);
```

This declaration invokes `headFile` once for every element in `files`. Each invocation is scheduled concurrently; the caller must not wait for one element invocation to finish before scheduling the next. The fanout result preserves the source iteration order, so `fileContent[i]` corresponds to `files[i]` when every invocation succeeds.

Completion markers do not restrict fanout: a fanout may invoke a now, soon, or later Vajram. Each asynchronous element invocation must still be scheduled without blocking the caller.

For an errable fanout, the aggregate succeeds only when every element invocation succeeds. If an element fails, the aggregate returns that failure according to the backend's documented concurrent-aggregation behavior; it must not silently omit failed elements. Resolver-local values are scoped to their individual element invocation and are released when that invocation completes.

## Dependencies, Resolvers, And Lifetime

A dependency declaration binds an invocation result to a Vajram-local name:

```vajram
UserInfo userInfo = getUserInfo(userId = userId);
```

Each resolver maps one or more callee input names to expressions or to a logic block. A resolver marked `=*` fans one expression sequence into multiple invocations. A dependency or invocation marked `*` is also a fanout declaration.

The caller owns every value it computes. Passing an existing value to a dependency shares that value; it does not transfer or deep-copy ownership. A resolver logic block owns all names it declares. Those names exist only while resolving and invoking that dependency.

For an asynchronous dependency, resolver evaluation and invocation form one asynchronous continuation. Resolver-local values must be released when that continuation completes. An implementation must not retain resolver-local values in the parent Vajram lifecycle after the dependency result has been obtained.

## Statements, Outputs, And Errors

An assignment declares a new local binding. Its scope begins after its declaration and ends with the enclosing logic block or resolver continuation.

`throw expression;` terminates the current errable execution with an error derived from that expression. A `throw` in a non-errable context is invalid unless the backend has an explicitly documented error representation for that context.

An output logic block evaluates statements in source order and returns its final yield expression. Multiple yield expressions form an ordered tuple-like aggregate. A logic block with no yield returns `void`.

An output may delegate directly to a dependency invocation. An invocation may include an errable fallback (`?method(...)`) and annotated trailing blocks. Annotations without a standardized semantic definition must be preserved or diagnosed; an implementation must not invent behavior for them.

## Expressions

Vajram-Lang expressions include identifiers, literals, boolean negation, addition, equality, function calls, method chains, member access, method references, groupers, special calls, constructors, and logic-block lambdas.

The language specifies expression structure, errability, completion markers, and ownership/lifetime behavior. It does not define the semantics of application-defined methods, constructors, types, or libraries. A backend may validate those through its target type system, but it must not claim unsupported application-library behavior is part of Vajram-Lang.

The implicit lambda parameter `_` names the value supplied to a logic-block lambda. Explicit `name ->` lambda syntax is not part of the V1 function-call syntax.

## Diagnostics

A conforming implementation must report diagnostics with severity, source file, line, column, and message. Parsing errors, duplicate Vajram names, unresolved invocations, caller-access violations, and unsupported semantic constructs are errors.

An implementation may continue processing independent files after finding an error, but it must not report successful compilation when any error is present.

## Non-Goals Of V1

- Semantic translation of application libraries.
- Inference of application method signatures or constructor arity.
- Implicit cross-thread execution.
- Semantics for arbitrary invocation annotations beyond preserving or diagnosing unsupported annotations.
