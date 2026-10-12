---
title: "Vajram-Lang Rust Compiler"
---

# Vajram-Lang Rust Compiler

This module compiles `.vajram` sources into Rust modules, producing one `.rs` file per source file. It is intentionally a structural compiler: it validates Vajram references and caller access, and translates Vajram-specific operators, while application method calls and application types remain Rust API contracts.

## Completion And Blocking

Vajram code is non-blocking. A normal output block is a **now** Vajram and becomes a synchronous `pub fn` unless an asynchronous dependency requires an async entry point. An output block marked `~` is a **soon** Vajram and becomes `pub async fn`; one marked `~~` is a **later** Vajram and also becomes `pub async fn`.

Completion markers do not restrict invocation: every Vajram may invoke now, soon, or later Vajrams. The compiler infers an async Rust entry point for a caller that invokes an async dependency, even when its own output block is unmarked.

## Deferred Facet Graph

Dependency invocation is eager but dependency consumption is deferred. When a dependency facet is declared, generated code starts its invocation without awaiting it. A field, resolver, or output block awaits that facet only when its expression consumes the facet value.

The compiler must lower computed facets as a dependency graph, not as a linear sequence of blocking statements. Every facet task captures and awaits only the source facets it uses. This lets independent work progress concurrently: if `c1` consumes `d1` and `c2` consumes `d2`, both `d1` and `d2` start immediately, and either `c1` or `c2` may complete first. Source declaration order must not cause `c2` to wait for `d1` merely because `c1` appears first.

Independent computed facets are lowered into unspawned `async { ... }` blocks, one per facet, grouped by dependency level (a topological sort over facet-to-facet references). Each level's facets are resolved together with `futures::join!`/`futures::try_join!` before the next level's blocks run. This keeps facets that don't depend on each other from blocking on one another purely due to declaration order, without ever detaching a task from the current stack frame — the current thread is never blocked, but nothing is spawned either, so borrowed (`&'a T`) facet values can flow into the joined futures. WASM and native targets share this same `join!`-based lowering.

## Ownership And Resolver Lifetimes

Each Vajram call is given a caller-supplied bump arena (`&'a vajram_rt::Arena`, backed by the `bumpalo` crate) instead of reference counting:

- Input fields and injection fields are `&'a T`, borrowed from the caller's arena.
- A Vajram returns `&'a T` or `Result<&'a T, VajramError>` for an errable output — the returned value lives in the caller's arena and outlives the call.
- Passing an existing Vajram value to a dependency passes the borrow through unchanged; no cloning or reference-count bump occurs.
- A resolver expression which computes a new value is allocated with `arena.alloc(...)` inside the dependency call, unless the facet is marked `` `local ``.
- A facet annotated `` `local `` is a plain owned Rust value (no arena allocation, no `&'a` wrapper): it is scoped to that Vajram's execution and freed by ordinary Rust `drop` at the end of the call. Passing a `` `local `` value into a nested Vajram call allocates a throwaway scratch `Arena::new()` for that nested call so the callee still receives an `&'a T`.

Because nothing survives a Vajram's own execution except what's written into the arena the caller passed in, memory allocated within a Vajram (other than its output) is reclaimed once that arena — or the throwaway scratch arena backing a `` `local `` value — goes out of scope. Dependency-injection singletons (`AppContext`, `Injector`, `Provider`) are the one deliberate exception: they are still `Rc`-based internally and threaded by plain reference (`&AppContext<I>`), since they are process-lifetime, not call-lifetime, state.

## Other Translation Rules

- `T?` maps to `Result<T, VajramError>` internally and `Result<&'a T, VajramError>` at a Vajram boundary.
- `string`, `int`, and `void` map to `String`, `i64`, and `()`.
- `new Foo(args)` maps to `Foo::new(args)`.
- `nil` maps to the bundled `vajram_rt::nil()` helper.
- `?` errable method syntax is served by the bundled `Errable` trait.
- Method chains and lambda bodies are structurally transliterated; unsupported Java-library idioms are intentionally left for Rust type checking to diagnose.

The compiler copies `vajram_rt` into the generated crate. Every generated crate needs the `bumpalo` crate, since `vajram_rt::Arena` is a `bumpalo::Bump` alias used by every Vajram call regardless of sync/async. Native crates with async Vajrams need Tokio with the `rt` feature. WASM crates with async Vajrams need `futures`, `wasm-bindgen`, and `wasm-bindgen-futures`; Tokio is not used by the WASM prelude.

## Annotation Processors

Compiler extensions implement `VajramAnnotationProcessor` and register through Java's `ServiceLoader`. A processor declares the `callers` annotations it supports and whether it runs once for each matching Vajram (`ISOLATED`) or once for every matching Vajram (`AGGREGATING`). Processors receive matching and complete-compilation AST views, diagnostics, symbol resolution, and a confined output writer.

The bundled aggregating `outsideProcess` processor is selected by:

```vajram
vajram hello() out string permit callers `outsideProcess public {
  { "Hello" }
}
```

It writes `main.rs`, which accepts a Vajram name as its first argument, dispatches to a matching public annotated Vajram, and prints the output. External-process dispatch accepts `string` and `int` inputs as named flags after the Vajram name, such as `hello --name Alice --count 3`. It blocks at the process boundary with a current-thread runtime while awaiting a soon or later Vajram; Vajram execution itself remains non-blocking.

## System Vajrams

`readFileAsString(path = filePath)` is a built-in soon Vajram for the native target. It is available without a source definition, reads the supplied path asynchronously with Tokio, decodes UTF-8, and returns a `string`. File I/O failures terminate the non-errable invocation with an error. Generated Cargo crates using it require Tokio's `fs` feature. WASM compilation rejects it with a source-positioned diagnostic until the browser File Picker SDK capability is bundled; it never lowers to Tokio filesystem I/O on WASM.

`concatStrings(strings = values, separator = separator)` is a built-in now Vajram imported from `lang.Strings`. It joins an ordered array of strings into one `string`, inserting `separator` between adjacent values.

`callHttp(url = requestUrl)` is a built-in soon Vajram imported from `lang.net`, supported on both targets. On the native target it issues a non-blocking HTTP GET using `reqwest`; generated Cargo crates need the `reqwest` crate (default features, which pull in Tokio). On the wasm target it lowers to the bundled `vajram_rt::fetch_text` helper, which drives the browser's `fetch` API through `wasm-bindgen`/`web-sys` instead of Tokio; generated wasm crates need the `web-sys` crate with the `Request`, `RequestInit`, `RequestMode`, `Response`, and `Window` features enabled. Either way it returns the response body decoded as a `string`, and request or decoding failures terminate the non-errable invocation with an error.

## Vajram Injection
1. For every vajram which has injections defined, a struct called <Vajram>_Injections is generated where each injectable value has one corresponding field in the struct - the field type is `Provider<T>` where T is the type of the injection facet.
2. All rust functions compiled from vajrams accept a Context object. This Context object has an "Injector" instance which can retrieved by calling `injector()` method on the Context object.
3. The first time that a vajram is invoked, a single instance of the <Vajram>_Injections struct is created - the new function of the struct accepts the context.
4. The constructor of the struct invokes the `getProvider(injectionKey, context)` method passing an injection key as parameter.
5. The injection key is of the struct type `InjectionKey` which is a system type. It contains a type and a list of selector annotations. The `getProvider` method returns a `Provider` that returns the value of the instance. The `<Vajram>_Injections` instance is a singleton and stores this provider for the lifetime of the application and is used by the vajram for every invocation
6. The Injector exposes a `getProvider(injectionKey, context)` method which returns a re-usable threadsafe `Provider<T>` instance on which vajrams call `get()` to get the injected instance. The Injector works in this way:
   1. It has mappings from injector keys to a provider which invokes the `provider vajram corresponding to that injector key declared by the developer. This map is populated by code generated by the compiler which has discovered all providers at compile time.
   2. When the `getProvider` method is called, the injector creates a new `Provider` that decorates the developer written provider - it creates a new instance of the appropriate scope from the Context and looks up a map if an instance already exists for that scope instance. If it does, it returns that, else it calls the developer written provider
7. The vajram accesses the injection values by calling get() on the providers in the `_Injections` struct
