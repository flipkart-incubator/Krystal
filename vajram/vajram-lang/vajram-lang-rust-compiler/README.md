# Vajram-Lang Rust Compiler

This module compiles `.vajram` sources into Rust modules. It is intentionally a structural compiler: it validates Vajram references and caller access, and translates Vajram-specific operators, while application method calls and application types remain Rust API contracts.

## Completion And Blocking

Vajram code is non-blocking. A normal output block is a **now** Vajram and becomes a synchronous `pub fn` unless an asynchronous dependency requires an async entry point. An output block marked `~` is a **soon** Vajram and becomes `pub async fn`; one marked `~~` is a **later** Vajram and also becomes `pub async fn`.

Completion markers do not restrict invocation: every Vajram may invoke now, soon, or later Vajrams. The compiler infers an async Rust entry point for a caller that invokes an async dependency, even when its own output block is unmarked.

## Deferred Facet Graph

Dependency invocation is eager but dependency consumption is deferred. When a dependency facet is declared, generated code starts its invocation without awaiting it. A field, resolver, or output block awaits that facet only when its expression consumes the facet value.

The compiler must lower computed facets as a dependency graph, not as a linear sequence of blocking statements. Every facet task captures and awaits only the source facets it uses. This lets independent work progress concurrently: if `c1` consumes `d1` and `c2` consumes `d2`, both `d1` and `d2` start immediately, and either `c1` or `c2` may complete first. Source declaration order must not cause `c2` to wait for `d1` merely because `c1` appears first.

Generated crates using deferred facets execute tasks inside a Tokio `LocalSet`; `Rc` values are deliberately single-threaded and cannot be sent to a multi-thread worker.

## Ownership And Resolver Lifetimes

Values crossing a generated Vajram boundary use `Rc<T>`:

- Input fields and injection fields are `Rc<T>`.
- A Vajram returns `Rc<T>` or `Result<Rc<T>, VajramError>` for an errable output.
- Passing an existing Vajram value to a dependency emits `Rc::clone(&value)`. This increments a reference count, not a deep copy or ownership transfer.
- A resolver expression which computes a new value is wrapped in `Rc::new(...)` inside the dependency call.

Async dependency continuations own only `Rc` handles. Resolver-local values are created inside the `async move` continuation and their handles are dropped when that continuation completes. The underlying allocation is reclaimed after the last parent or dependent handle is dropped.

`Rc` safe APIs prevent use-after-free, double-free, and data races. They do not collect reference cycles; generated application types must use `Weak<T>` for cyclic back-references. Mutable shared application state should be avoided; `Rc<RefCell<T>>` is memory-safe but can panic on an invalid runtime borrow.

## Other Translation Rules

- `T?` maps to `Result<T, VajramError>` internally and `Result<Rc<T>, VajramError>` at a Vajram boundary.
- `string`, `int`, and `void` map to `String`, `i64`, and `()`.
- `new Foo(args)` maps to `Foo::new(args)`.
- `nil` maps to the bundled `vajram_rt::nil()` helper.
- `?` errable method syntax is served by the bundled `Errable` trait.
- Method chains and lambda bodies are structurally transliterated; unsupported Java-library idioms are intentionally left for Rust type checking to diagnose.

The compiler copies `vajram_rt` into the generated crate. Add Tokio with the `rt` feature when generated code contains async Vajrams.

## Annotation Processors

Compiler extensions implement `VajramAnnotationProcessor` and register through Java's `ServiceLoader`. A processor declares the `callers` annotations it supports and whether it runs once for each matching Vajram (`ISOLATED`) or once for every matching Vajram (`AGGREGATING`). Processors receive matching and complete-compilation AST views, diagnostics, symbol resolution, and a confined output writer.

The bundled aggregating `outsideProcess` processor is selected by:

```vajram
vajram hello() out string permit callers `outsideProcess public {
  { "Hello" }
}
```

It writes `main.rs`, which accepts a Vajram name as its first argument, dispatches to a matching public annotated Vajram, and prints the output. External-process dispatch accepts positional `string` and `int` inputs after the Vajram name. It blocks at the process boundary with a current-thread runtime while awaiting a soon or later Vajram; Vajram execution itself remains non-blocking.

## System Vajrams

`readFileAsString(path = filePath)` is a built-in soon Vajram. It is available without a source definition, reads the supplied path asynchronously with Tokio, decodes UTF-8, and returns a `string`. File I/O failures terminate the non-errable invocation with an error. Generated Cargo crates using it require Tokio's `fs` feature.

`concatStrings(strings = values, separator = separator)` is a built-in now Vajram imported from `lang.Strings`. It joins an ordered array of strings into one `string`, inserting `separator` between adjacent values.
