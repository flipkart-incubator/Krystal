# Vajram-Lang Browser Execution Plan

## Goal

Compile `.vajram` sources to Rust/WebAssembly and execute the resulting Vajrams in a web browser.
The browser is the execution environment; compilation may initially run in an isolated service because
the existing compiler is Java and the generated Rust must be compiled by Cargo.

## Public Vajram API

`permit callers `outsideProcess public` is the opt-in boundary for callers outside the compiled
Vajram graph. It remains the single source-level declaration for both native and WebAssembly builds.
No browser-specific annotation is needed.

The compilation target determines the adapter generated for these Vajrams:

- `native`: retain the existing generated `main.rs` command-line dispatcher.
- `wasm`: generate a `wasm-bindgen` dispatcher instead of `main.rs`. It accepts a public Vajram name
  and structured inputs, validates both, invokes the generated Vajram, awaits it when required, and
  returns a structured result or error to JavaScript.

Target selection should be explicit, with `native` remaining the default. For example:

```text
--target native
--target wasm
```

## Runtime Design

The generated Rust preserves Vajram's deferred-facet execution model: dependency work starts eagerly,
while a resolver or output waits only when it consumes that dependency.

The bundled `vajram_rt` needs target-specific implementations:

- Native builds retain the Tokio `LocalSet` implementation used by `spawn_local_shared`.
- WebAssembly builds use browser-compatible futures and `wasm_bindgen_futures::spawn_local` to start
  work promptly and expose a cloneable shared future for later facet consumption.

The WebAssembly runtime stays single-threaded, matching the generated code's current `Rc<T>` ownership
model. The generated public adapter must be asynchronous whenever the selected Vajram or its dependency
graph is asynchronous.

## Browser Capabilities

Browser-targeted code must not acquire native process, filesystem, or unrestricted network access.

- Reject native-only system Vajrams, including `readFileAsString`, for the `wasm` target unless they have
  an explicit browser implementation.
- Model browser functionality as explicit host-provided capabilities, such as mocked fetch, timers, or
  user-selected file contents.
- Begin with pure, deterministic Vajrams and mocked capabilities in the playground.
- Do not permit arbitrary Rust crates, build scripts, or unreviewed host imports in user-authored code.

Application types and method calls currently remain Rust API contracts. The browser target therefore
needs a versioned, allowlisted browser SDK crate containing the supported types and functions.

## Build And Delivery Pipeline

1. The web editor sends one or more `.vajram` source files to an isolated compiler service.
2. The existing Java compiler parses, validates, and emits Rust with `--target wasm`.
3. An isolated Cargo build compiles the generated crate for `wasm32-unknown-unknown` and produces JS
   bindings.
4. The service caches successful artifacts by normalized source, compiler version, runtime version, and
   browser SDK version.
5. The browser loads the returned WebAssembly module and invokes only generated public dispatch APIs.
6. Vajram execution and browser-safe capabilities run locally in the browser.

The compilation service is untrusted-code infrastructure and must run builds in disposable sandboxes
with strict CPU, memory, wall-clock, disk, and dependency-access limits.

## Implementation Work

1. Add compilation-target configuration to `RustCompilerMain`, the code-generation context, and tests.
2. Refactor `OutsideProcessAnnotationProcessor` so its public-Vajram discovery is shared by native CLI
   and WASM adapter emitters.
3. Keep native `main.rs` generation unchanged; add the WASM dispatcher and target-specific Cargo
   manifest/template.
4. Split the bundled `rust-prelude` into native and browser async runtime implementations behind Cargo
   target configuration.
5. Add target validation for system Vajrams, injections, types, and external API contracts that cannot
   run in a browser.
6. Add a WebAssembly sample fixture and Gradle task alongside `vajram-lang-samples`.
7. Build the web playground: editor, diagnostics, structured input, result/error display, and artifact
   loading.
8. Add optional graph and execution-timing events to the browser runtime for visualization.

## Verification

- Compile generated fixtures for both native and `wasm32-unknown-unknown` targets.
- Verify every public `outsideProcess` Vajram is available through the WASM dispatcher and non-public
  Vajrams are rejected.
- Verify structured input validation and structured error output.
- Verify independent asynchronous dependencies are started eagerly and do not become serialized in the
  browser runtime.
- Verify native-only capabilities fail with source-positioned diagnostics under `--target wasm`.
- Run browser integration tests that load a compiled module and invoke a now Vajram and an asynchronous
  dependency graph.
