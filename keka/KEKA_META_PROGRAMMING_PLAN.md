# Keka meta-programming API: use cases (draft for review)

## Context
Keka needs compile-time meta-programming so developers declare intent (an entity, a table, an endpoint) and the compiler generates the boilerplate and enforces standards (HTTP, SQL). Today the only extension point is `VajramAnnotationProcessor` (keka-rust-compiler/.../plugin/): it matches only `callers` annotations on a vajram, runs after resolution and Rust emission, and writes files. It can't add vajrams or types that other Keka code uses. Custom annotation definitions (`` `type name(...) ``) with typed, constant parameters and `` `target `` now exist on branch `keka-annotation-defs` in Krystal.

This file lists the use cases the API must serve. Design comes after the list is agreed.

## A. What developers declare and what is generated (annotation users)

| # | Use case | Declared | Generated | Krystal Java analog |
|---|---|---|---|---|
| U1 | Entity / model | Annotated type with typed fields (optional, defaults, serial ids) | Rust struct, immutable builder, equality, optional handling | `@ModelRoot` -> `_Immut`, `_ImmutPojo` |
| U2 | Enum | Annotated enum with a safe `UNKNOWN` default | Rust enum and conversions | `EnumModel` |
| U3 | Serialization | Protocol flags on an entity (JSON, protobuf) | serde impls, `.proto` schema | `@SupportedModelProtocol` -> `_ImmutJson`, `_ImmutProto` |
| U4 | Field constraints | Range, regex, required on fields (e.g. rating 0-10, trust 1-10) | Validators called at construction | `@IfAbsent`, `validateModelRootMethod` |
| U5 | SQL table | Entity tagged as table: columns, primary/unique/foreign keys, defaults | DDL, typed row type, column constants | `@Table`, `@PrimaryKey`, `@ForeignKey` |
| U6 | SQL queries | Trait-style declaration: select/insert with where, order, limit, projection | IO vajrams that build SQL, bind params, map rows | `@SQL @SELECT/@INSERT @Trait` -> `*_VertxSql` |
| U7 | Append-only store | Table tagged "append-only, latest wins" (mitram) | Insert vajram plus "select latest" vajram per key | none (mitram-specific, built on U5/U6) |
| U8 | HTTP endpoint | Vajram tagged with verb, path; facets tagged path/query/body | Server routes, request mapping, response encoding | `@Path/@GET/@POST` -> `*_JakartaRestResource` |
| U9 | Process entry points | `outsideProcess` (exists), scheduled job (`autoCommitDueDrafts`) | `main.rs` dispatch, scheduler loop | `OutsideProcessAnnotationProcessor` (Keka), lattice app impl |
| U10 | Dependency injection | Provider and injected-type declarations | Singleton wiring, scopes | Guice/CDI module generators, Keka `_Injections` |
| U11 | Cross-cutting policy | Declarative flags: caching, retry, timeout, auth, tracing on a vajram | Wrapper vajram or generated decorator | none yet |
| U12 | Test doubles | Store traits | In-memory fake implementing the same trait | none yet |
| U13 | Schema docs | Endpoints, entities | OpenAPI / `.proto` files | proto schema generators |

## B. What generator authors need (the plugin API)

| # | Need | Why |
|---|---|---|
| G1 | Read declarations: entities, enums, vajrams, annotation definitions, with typed annotation arguments and source locations | Every generator starts from declarations |
| G2 | Match annotations on any site, not only `callers` | Entities, fields, inputs, facets, vajrams |
| G3 | Emit Keka source that rejoins the compilation, so user vajrams can call generated ones and get type-checked | U6, U7, U8 are called from user code |
| G4 | Emit Rust directly (files, extra crate modules, Cargo dependencies) | Entry points, serialization, glue |
| G5 | Phase or round ordering: generators whose output others read (models before queries before endpoints); deferral when an input is not generated yet | Krystal does this with MODELS/FINAL phases and `CodeGenShortCircuitException` |
| G6 | Diagnostics at the annotation's source position; errors fail the build | Standards enforcement |
| G7 | Validation of annotation arguments against the annotation definition (done for vajram-level uses) | Typed API for generator authors |
| G8 | Deterministic, formatted output; golden tests for generators | Reviewable generated code |
| G9 | Third-party registration without editing the compiler (`ServiceLoader`, as today) | Extensibility |
| G10 | Generator composition: a generator can consume another's output (U7 on U5 and U6) | Layered standards |
| G11 | Target (native vs wasm) visible only to generators that emit Rust. Generators that emit Keka source are target-neutral: the Rust backend handles target differences. | `Target` is already on `AnnotationProcessorContext`; not needed by v1 (models, constraints) |
| G12 | Authoring language: Java plugins first; Keka-defined generators are required and follow, on the same serializable contract | Decided |

## C. Mitram's immediate needs (drives priority)
U1, U2, U4 (statement, trust, rating, enums, 1-10/0-10 ranges), U5, U6, U7 (six stores in the LLD), U8 (API), U9 (scheduled auto-commit). U3 follows once the API serves HTTP. U10-U13 later.

## Decisions so far
- v1 scope: models first (U1, U2, U4; G1-G3, G6-G9).
- Generated code is Keka source, parsed and re-resolved (javac-style rounds).
- Generators: Java SPI first; the contract is designed so Keka-defined generators can follow.
- Ordering: free multi-round, not fixed phases.

## Proposed design (draft)

### 1. Pipeline
```
parse all sources -> index declarations (symbol table, annotation definitions)
  -> loop (rounds):
       run generators on declarations that are new since their last run
       generators emit Keka source (and Rust files) or defer
       parse emitted Keka, add to the model, mark as generated
     until a round emits no new Keka (cap: 10 rounds, then error)
  -> full resolution and validation of the whole program
  -> emit Rust, then run Rust-emitting generators
```
- Resolution splits in two. Declaration-level indexing (names, signatures, annotation definitions and their typed arguments) is available to generators every round. Body-level validation (types of facets, completion, errability) runs once at the end, because bodies may call vajrams that are generated later.
- Termination: a generator runs at most once per declaration identity (declaration key plus generator id). A generator may `defer()`; it is retried only if the model changed since. Deferrals unresolved at the end are errors at the triggering annotation.
- Determinism: generators run in a fixed order (by id), declarations sorted by file then position.

### 2. Generator contract (Java SPI, generalising `VajramAnnotationProcessor`)
- `KekaGenerator`: `id()`, `triggers()` (annotation definition names), `scope()` (PER_DECLARATION or AGGREGATING), `generate(GeneratorContext)`.
- `GeneratorContext`: `declarations()` (matching, with typed resolved annotation arguments, source locations, parent chain), `model()` (read-only view of everything indexed this round), `emitKeka(relativePath, source)`, `emitRust(relativePath, content)`, `defer(reason)`, `error(location, message)`, `warning(...)`, `round()`, and `target()` (kept because it exists today; only generators that call `emitRust` need it, and a Keka-emitting generator is target-neutral).
- Declaration view types: Vajram, Entity, Enum, Field, Input, Facet, Package; each carries `annotations()` (name, definition, evaluated constant arguments) and `location()`.
- Plugins ship annotation definitions as bundled `.keka` resources in a plugin package (for example `keka.model`), loaded into every compilation, so users write `` import `type model from keka.model ``.
- Registration: `ServiceLoader` as today. The built-in `outsideProcess` processor becomes the first generator on the new SPI, and the old interface stays as an adapter during migration.
- Serializable contract: the declaration view and the generator output (list of `{kind: keka|rust, path, content}`, diagnostics, defers) have a JSON form, so a Keka-defined generator can implement the same contract later.

### 3. Generated source handling
- Written to `<out>/generated-keka/<package>/<Name>.keka` with a header naming the generator and the triggering declaration. Treated as sources for all later rounds and the final compile.
- Diagnostics inside generated code report the generated file position plus the origin annotation location.
- Not committed by default; build output only.

### 4. Prerequisite AST work
- Retain full annotations (name, arguments) on every site: input, output, facet, dependency, injection, callers, package. Today only vajram-level annotations keep arguments. Extend `AnnotationValidator` to check all sites against `` `target ``.
- Add entity and enum declarations to the grammar and AST (see the open decision below), with annotations on fields.

### 5. Keka-defined generators (designed now, built later)
- A generator is a vajram marked `` `generator(for = <annotation>) `` that takes a declaration view (an entity) and returns generated outputs.
- Needs language features not yet present: entities, collections, conditionals, string building (gaps 1, 3, 5).
- Execution model: compile generator vajrams to a separate host Rust crate, run it as a subprocess, and exchange the JSON contract above. The Java and Keka paths share one contract.

## Verification (when implemented)
- Unit tests per component in `RustCompilerGoldenTest`-style: generator discovery, multi-round termination, defer-then-resolve, error mapping to origin.
- A sample generator plus sample under `keka-samples`, built with `./gradlew :keka:keka-samples:compileVajram` and cargo tests.
- Mitram builds with an entity generator once available.

## Further decisions (confirmed)
- Entities and enums are language-native: the compiler lowers them to Rust structs, builders and enums. U1 and U2 are therefore language features (they close GAP-3), and generators are layered on top (U3 serde, U4 constraints, U5 tables, U6 queries). The v1 generator is the field-constraint one (U4).
- Keka-defined generators later: compile generator vajrams to a host Rust crate, run as a subprocess, exchange the JSON contract.
- Generated Keka lives in build output only.

## Implementation steps (all in `../flipkart-incubator/Krystal/keka`, branch `keka-annotation-defs`)
1. Retain annotations everywhere in the AST. `ast/InputDecl`, `Field`, `Dependency`, `InjectionDecl`, `Callers`, `OutputBlock`, `VajramFile.package` keep `VajramAnnotation` (name plus arguments) instead of names; update `parse/AstBuilder.annotationName(...)` call sites and `resolve/AnnotationValidator` to check every site against `` `target `` (reuse its constant evaluation).
2. Models (language-native), scope below in "Models: scope and plan". Enums are out of scope: they will be modelled later as tagged unions.
3. Declaration model for generators: read-only views (`plugin/model/*`) built from the AST each round; reuse `ast/*` records and `AnnotationValidator` evaluation for typed arguments.
4. New SPI `plugin/KekaGenerator`, `GeneratorContext`; adapter so `plugin/OutsideProcessAnnotationProcessor` keeps working; discovery through `ServiceLoader` as in `plugin/AnnotationProcessorRunner`.
5. Round engine in `cli/RustCompilerMain.compile` (today parse, resolve, emit, process): parse, index, rounds, final validation, emit. Split `resolve/Resolver.validate` into declaration indexing and body validation.
6. Plugin-shipped annotation definitions loaded as bundled resources; first plugin package `keka.model` with a built-in constraint annotation (for example `` `range(min, max) ``) and its generator emitting a validator vajram.
7. Generated source writer and origin-aware diagnostics (`diag/Diagnostics`, `ast/SourceLocation`).
8. Tests in `keka-rust-compiler/src/test/.../RustCompilerGoldenTest.java` (extend it, as before), a sample in `keka-samples`, and a mitram use.

## Models: scope and plan (current task; models only)

### Concepts (from the user)
All data in Keka is a model. A model with identity (fields marked `` `idField ``) is an entity; a model without identity is a value (struct-like, inlinable, behaves like a primitive). All primitives are models. User models compose other models. Enums are not part of this task: they will be tagged unions later.

### Syntax (confirmed unless marked default)
```keka
package mitram.model;

// value model: no `idField, so compared field by field
model Rating(`range(min = 0, max = 10) int32 value;)

// entity: equality and hash use only the `idField fields
model Statement(
  `idField string id;
  string text;
  string? topicId;          // `?` is errable: value, error or nil (absent)
  int32 confidence = 5;     // constant default
  List<Rating> ratings;     // composition, including collections
)

vajram describe(string id) out `now string {
  Statement s = new Statement(id = id; text = "hi");   // default: new + named args, `;` between args
  { s.text }
}
```
- Declaration: `model Name(fields)`, fields separated by `;` (trailing `;` allowed), each `annotations* type name (= constant)?`. Field types: primitives, other models, `List<T>`, `Map<K,V>`, `T?`.
- An omitted field takes its default; an omitted `T?` field is `nil`; any other omitted field is a compile error.
- Entity-typed fields embed a copy of the entity (equality and hash still by its id fields). Default, since ids are not referable for now; references (`Ref<T>`) can be added later.
- Construction: `new Model(field = value; ...)` (default; reuses `new`).
- Imports: `import model Name from pkg;`.
- `` `idField `` is a built-in annotation with target `FIELD`; `FIELD` and `MODEL` are new `` `target `` kinds so `` `range `` and other field annotations are ordinary annotation definitions.

### Work items (Krystal, branch `keka-annotation-defs`)
1. Grammar `keka-grammar/.../Keka.g4`: `model` keyword; `top_level_file_element: vajram_def | type_def | model_def`; `model_def: annotation* MODEL ID LPAREN model_field_list? RPAREN`; `model_field: annotation* nowType ID (ASSIGN annotation_value)?` separated by `SEMI`; `new Model(arg = expr; ...)` alternative in `expr`; `import model`.
2. AST: `ast/ModelDef`, `ModelFile` (mirrors `AnnotationDefFile`), `Expr.NewModel`; `ast/ParsedSource` gains models; `parse/AstBuilder.buildSource` builds them.
3. Symbol table and validation (`resolve/SymbolTable`, new `resolve/ModelValidator`, `resolve/AnnotationValidator`): unique model names across models and vajrams; field types resolve (primitives, models, `List`, `Map`, `T?`); no direct containment cycles (through plain fields or `T?`; `List`/`Map` break a cycle); duplicate fields; defaults are constants matching the field type (reuse the constant evaluation in `AnnotationValidator`, extract it into a shared class); id fields must be hashable (not floats); `` `idField `` only on model fields; `new` checks the model exists, no unknown or duplicate fields, and required fields present; `FIELD` and `MODEL` added to `TARGETS`, annotation sites on fields and models checked.
4. Rust lowering (`codegen/RustEmitter`, `codegen/TypeMapper`, `cli/RustCompilerMain`): one `struct` per model in its source file's module (sources with only models still get a module; register in `ModuleTree`). Field names keep their Keka spelling with `#[allow(non_snake_case)]`, as `Errable` does, so member access stays a plain transliteration. Values derive `Debug, Clone, PartialEq` and add `Eq, Hash` when every field allows it. Entities implement `PartialEq`, `Eq`, `Hash` by hand over the `idField` fields. `new Model(...)` becomes a struct literal with defaults filled in. Each module imports all models with `#[allow(unused_imports)] use crate::pkg::module::{...}` (model names are globally unique). Models can be vajram inputs, outputs and facets like any type.
5. Runtime prelude (native and wasm `error.rs`, `errable.rs`): derive `PartialEq, Eq, Hash` on `VajramError` so `T?` fields can sit in models; give `VajramError` a nil kind and make `isNil()` true only for nil (it is always false today).
6. Tests: parser and validator cases in `RustCompilerGoldenTest.java` (extend it); a sample `keka-samples/.../models/` with a value model, an entity, a `T?` field and a vajram using them, plus Cargo tests for equality (entity by id, value by fields) and hashing in `keka-samples/src/test/rust/tests`; regenerate goldens (`-PupdateRustGoldens`) and review them.
7. Docs and tracking: add a Models section to `keka/KEKA_LANGUAGE_SPEC.md`; move GAP-3 to Fixed in `keka/KEKA_GAPS.md` with the commit; check mitram still compiles.

### Verification
`./gradlew :keka:keka-rust-compiler:test :keka:keka-grammar:test :keka:keka-samples:test` in Krystal; `./gradlew compileKeka` in mitram; run the new sample with `./gradlew :keka:keka-samples:runVajram -Pvajram=<sample>`.

### Risks
- Models in the bump arena are never dropped (as with `String` outputs today), so their heap fields leak until the arena goes; acceptable and consistent with the current design.
- `T?` fields make a model contain `Result`, so `Copy` is never derived for such models.
- `isNil` semantics change (see item 5) is a behavioural fix to the runtime.

## Out of scope for this design
Enums and tagged unions (later), generators and the round engine (steps 3-7 above wait for models), Keka-defined generator execution (only the contract is fixed), SQL and HTTP generators, and annotation checks on generated declarations beyond the normal pipeline.
