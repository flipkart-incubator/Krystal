# Vajram-Lang Browser Playground Plan

## Goal

Provide a localhost web playground in which a user can author one or more `.vajram` files,
compile them to Rust/WebAssembly using a locally cloned Krystal repository, and execute the
resulting public Vajrams entirely in a browser WebAssembly sandbox.

The compiler service creates artifacts only. It must never execute user-authored Vajrams. The
browser loads the returned WebAssembly and executes only generated public dispatch APIs.

## Initial Scope And Assumptions

- The playground web application and compiler service run on `localhost`.
- The user already has a local clone of this repository checked out.
- The sandbox UI accepts an absolute Krystal repository location. The service validates that it
  is a Krystal checkout and uses its Gradle/compiler tooling as the compiler workspace.
- Editor sources are sent to the service as an explicit virtual file set. The service does not
  compile arbitrary source files discovered below the configured repository path.
- The browser HTTP capability may access any endpoint permitted by normal browser CORS and
  mixed-content rules. A future hosted deployment must add an explicit endpoint policy.
- Native compilation remains the default. WebAssembly compilation is an explicit target.

## Playground User Experience

The root UI has four sections in the left-most navigation pane:

- **Home**: explains the playground, browser execution model, repository-location requirement,
  and supported capabilities.
- **Language Spec**: renders `VAJRAM_LANGUAGE_SPEC.md` from the selected repository checkout.
- **Playground**: provides authoring, compilation, execution, diagnostics, and output.
- **Samples**: provides categorized, pre-written Vajram examples.

### Playground

The Playground section contains:

- A repository-location setting. It is required before compilation and the UI displays service
  validation failures without exposing host filesystem details beyond the selected path.
- A multi-file tabbed code editor. Users can create, rename, select, and close editable `.vajram`
  files. Compilation always submits the complete open file set.
- A target selector that initially exposes `wasm`; native remains available for compiler
  development and regression verification but is not executed by the browser playground.
- A Run button that compiles the current file set to WASM through the localhost service, loads the
  returned artifact, and invokes a selected public Vajram in the browser.
- A bottom pane below the editor for public-Vajram input, compilation state, structured output,
  execution errors, and timing. It must distinguish compiler failures from runtime failures.
- Inline editor diagnostics. Compiler diagnostics use file name, line, column, severity, and
  message so the editor can show markers and source ranges in the appropriate tab.

### Samples

The Samples section contains a categorized list whose entries show each example's flavor or
use case, including basic dependencies, asynchronous dependencies, fanout dependencies,
`readFileAsString`, and HTTP requests.

Selecting a sample opens a new read-only source-viewer tab. The viewer supports copying its
contents; users paste copied code into an editable Playground tab. Sample tabs are never
silently converted into editable user files.

## Public Vajram API

The `permit callers outsideProcess public` declaration is the source-level opt-in boundary for
callers outside the compiled Vajram graph. No browser-specific source annotation is required.

The compilation target chooses the generated adapter:

- `native`: retain the existing generated `main.rs` command-line dispatcher.
- `wasm`: generate a `wasm-bindgen` asynchronous dispatcher instead of `main.rs`. It accepts a
  public Vajram name and structured inputs, validates both, invokes the generated Vajram, awaits
  it when required, and returns a structured result or error to JavaScript.

Target selection is explicit:

```text
--target native
--target wasm
```

The WASM dispatcher exposes only public `outsideProcess` Vajrams. It must never expose internal
Vajrams, arbitrary generated Rust functions, or a general host-import mechanism.

## Browser Runtime And Capability Model

Generated Rust preserves Vajram's deferred-facet execution model: dependency work starts eagerly,
while a resolver or output waits only when it consumes that dependency.

The bundled `vajram_rt` needs target-specific implementations:

- Native builds retain the Tokio `LocalSet` implementation used by `spawn_local_shared`.
- WebAssembly builds use browser-compatible futures and `wasm_bindgen_futures::spawn_local` to
  start work promptly and expose a cloneable shared future for later facet consumption.

The WebAssembly runtime remains single-threaded, matching generated code's `Rc<T>` ownership
model. The public WASM adapter is asynchronous whenever the selected Vajram or its dependency
graph is asynchronous.

Browser-targeted code must not acquire native process access, unrestricted filesystem access, or
unreviewed host imports. Add a versioned, allowlisted browser SDK crate containing all supported
types, system Vajrams, and JavaScript bindings. WASM target validation rejects unsupported
injections, application/external Rust API contracts, public type shapes, and system capabilities
with source-positioned diagnostics.

## Standard-Library Browser Capabilities

### `readFileAsString`

`readFileAsString` remains a native asynchronous filesystem operation for the `native` target.
For the `wasm` target it lowers to an explicit browser File Picker capability:

- The declared path remains a user-visible placeholder only; it is not resolved against the
  browser or server filesystem.
- Invoking the Vajram opens the browser file-selection dialog.
- The selected file's UTF-8 text becomes the result asynchronously.
- User cancellation, unreadable content, and decoding failures are returned as structured Vajram
  errors rather than panics.

The browser SDK binding owns File Picker interaction. User source cannot call arbitrary browser
APIs directly.

### HTTP Web Client

Add an allowlisted HTTP web-client system Vajram and browser SDK request/response types.

- Define an explicit, serializable request contract including URL, method, headers, and optional
  body, and a response contract including status, headers, and body.
- Lower the native target to a non-blocking Rust HTTP client implementation.
- Lower the WASM target to non-blocking browser `fetch` bindings.
- Map transport failures, browser rejections, and non-success responses into a documented
  structured error/result contract without blocking the single-threaded WASM runtime.
- Initially rely on browser CORS and mixed-content enforcement. Preserve a single capability
  boundary so a hosted deployment can add an endpoint allowlist without changing Vajram source.

## Compiler, Build, And Delivery Pipeline

1. The browser sends the virtual `.vajram` file set, selected target, and configured repository
   location to the localhost compiler service.
2. The service validates the checkout and invokes the existing Java compiler with `--target wasm`.
3. The compiler parses, resolves, performs target capability validation, emits Rust, and returns
   structured source diagnostics on failure.
4. For successful WASM compilation, an isolated Cargo build compiles the generated crate for
   `wasm32-unknown-unknown` and generates `wasm-bindgen` JavaScript bindings.
5. The service caches successful artifacts by normalized virtual source set, compiler version,
   runtime version, browser SDK version, target, and Cargo toolchain version.
6. The browser receives the WASM binary, generated binding module, and public-Vajram metadata;
   it instantiates the module locally and invokes the generated dispatcher.
7. Execution, File Picker interaction, and HTTP requests occur only in the browser.

The current localhost scope can use local process isolation, but the service interface and build
layout must support future untrusted-code deployment. Each Cargo build therefore uses a disposable
workspace with bounded CPU, memory, wall-clock time, disk usage, process count, and dependency
access. User input never controls Cargo manifests, build scripts, crate dependencies, output
paths, or host imports.

## Implementation Work

1. Add a `CompilationTarget` model and `--target` CLI option to `RustCompilerMain`, preserving
   `native` as the default for existing callers.
2. Thread the target through code generation, runtime-prelude selection, annotation processing,
   validation, and compiler test helpers.
3. Refactor `OutsideProcessAnnotationProcessor` into shared public-Vajram discovery/validation
   plus target-specific native CLI and WASM dispatcher emitters.
4. Define the structured JavaScript/WASM dispatch request, result, error, and public-Vajram
   metadata contracts. Ensure public inputs and outputs have an explicit supported serialization
   policy.
5. Split `rust-prelude` into native and browser implementations while preserving eager deferred
   dependency execution semantics.
6. Add the versioned browser SDK crate and target validation for system Vajrams, injections,
   external contracts, and type shapes.
7. Implement native and WASM lowerings for `readFileAsString`, including error propagation and
   File Picker bindings for the browser target.
8. Define and implement the HTTP web-client system Vajram, its Rust client lowering, and its
   browser `fetch` lowering.
9. Add WASM Cargo templates and a dedicated sample fixture/module. Keep native CLI sample tests
   separate from WASM tests.
10. Add a localhost compiler-service module with structured compile responses, artifact caching,
    disposable build workspaces, resource limits, and repository-workspace validation.
11. Add a browser frontend module using the repository's existing static-web conventions where
    practical. Implement the four sections, multi-file editor, read-only sample viewer, inline
    diagnostics, compiler-service client, artifact loader, input form, and bottom output pane.
12. Package sample metadata and source independently from editable files so the UI can present
    categories and copyable read-only examples.

## Verification

- Compile fixtures for both `native` and `wasm32-unknown-unknown` targets.
- Verify native output and CLI behavior remain unchanged when no target is specified.
- Verify all and only public `outsideProcess` Vajrams are available through the WASM dispatcher.
- Verify structured public-input validation and structured runtime/compiler error output.
- Verify independent asynchronous dependencies are started eagerly and do not become serialized
  in the browser runtime.
- Verify WASM target rejection produces source-positioned diagnostics for native-only or
  unsupported capabilities.
- Verify `readFileAsString` uses native filesystem I/O for native builds and the File Picker for
  browser builds, including cancellation and decode errors.
- Verify the HTTP Vajram is non-blocking on native and WASM builds and that WASM behavior follows
  browser CORS enforcement.
- Run browser integration tests that compile through the localhost service, load a returned module,
  invoke synchronous, asynchronous, fanout, file-picker, and HTTP Vajrams, and display outputs.
- Run UI tests for section navigation, language-spec rendering, multi-file tabs, sample read-only
  viewers, copying samples, inline diagnostics, and bottom-pane output.
- After implementation, follow the repository verification sequence: publish locally with
  `upgradeVersionLocal.macOS.sh`, run `./gradlew test --rerun -PunsafeCompile=true`, then run
  `./gradlew build -PunsafeCompile=true`.
