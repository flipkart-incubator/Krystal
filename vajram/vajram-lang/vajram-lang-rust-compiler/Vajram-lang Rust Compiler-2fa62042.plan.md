<!-- 2fa62042-d571-4b04-8f0d-245dec3deea5 -->
---
todos:
  - id: "module-setup"
    content: "Create vajram-lang-rust-compiler module (build.gradle, settings.gradle entry), add -visitor to vajram-lang-grammar's generateGrammarSource"
     status: completed
  - id: "ast"
    content: "Define AST records + AstBuilder visitor over ANTLR parse tree"
     status: completed
  - id: "resolver"
    content: "Build cross-file SymbolTable/Resolver for packages, imports, dependency references"
     status: completed
  - id: "type-op-mapping"
    content: "Implement type mapping and operator desugaring (?, ~, ~~, *, nil, new, lambdas)"
     status: completed
  - id: "prelude"
    content: "Author vajram_rt Rust prelude (Errable trait, VajramError) as bundled resource"
     status: completed
  - id: "emitter"
    content: "Implement RustEmitter (RustWriter) producing per-Vajram module + mod/lib tree"
     status: completed
  - id: "cli"
    content: "Implement RustCompilerMain CLI driver with diagnostics + best-effort rustfmt invocation"
     status: completed
  - id: "tests"
    content: "Add golden-file tests from existing .vajram fixtures + optional rustc syntax smoke test"
    status: pending
isProject: false
---

# Vajram-lang → Rust Compiler

## Context found in repo
- `vajram/vajram-lang/vajram-lang-grammar` already has the ANTLR grammar ([Vajram.g4](vajram/vajram-lang/vajram-lang-grammar/src/main/antlr/com/flipkart/krystal/vajram/lang/Vajram.g4)) and generates `VajramLexer`/`VajramParser`/`VajramBaseListener` (no visitor yet — plugin default doesn't pass `-visitor`).
- `settings.gradle` already declares `vajram:vajram-lang:vajram-lang-compiler` but that directory has **no source/build.gradle** — it's an empty placeholder (presumably meant for a Java-target compiler). We will NOT touch it; we add a sibling module `vajram-lang-rust-compiler`.
- Root [build.gradle](build.gradle) applies common conventions to every leaf project (java-library, Java 17 toolchain, lombok, guava, slf4j, jspecify, checkerframework, google-java-format via `com.flipkart.java.code.standard`). New module inherits these automatically once added to `settings.gradle` and given a `build.gradle`.
- Sample `.vajram` files in `vajram-lang-grammar/src/test/resources/*.vajram` (e.g. `helloWorld`, `sayHelloToFriends`, `getFriendsOfUser`, `errabilityDemo`) are the concrete semantics reference and will be reused as compiler test fixtures.

## Decisions confirmed with user
- Concurrency target: generate real async Rust using **tokio + futures** (`join_all`/`try_join_all` for fanout, `async`/`.await` for `~`, `Result<T, E>` for `?`).
- Expression translation strategy: **structural transliteration, not semantic transpilation**. Rust already supports fluent `a.b().c()` method-chain syntax like Java, so most call chains carry over near verbatim as Rust method calls. vajram-lang-specific sugar (`?x`, `~x`, `*x`, lambda blocks) desugars into calls against a small hand-written Rust trait prelude crate (e.g. `Errable<T>`, `SoonExt`, fanout helpers) that the generated code depends on and that the user (or later, the compiler for a fixed known set of built-ins) implements for their own types. The compiler does not attempt to understand arbitrary Java stdlib semantics — it only understands vajram-lang grammar constructs.

## 1. Module setup
- Create `vajram/vajram-lang/vajram-lang-rust-compiler/` with a `build.gradle`:
  - `apply plugin: 'java-library'` (implicit via root config once added to settings.gradle).
  - `implementation project(':vajram:vajram-lang:vajram-lang-grammar')` — reuse the existing lexer/parser, don't duplicate grammar.
  - No new external deps needed beyond what's already globally provided (guava, slf4j, lombok, jspecify) — avoid adding a Rust-codegen library (e.g. no "javapoet for Rust" exists/needed); Rust source is plain text so a lightweight internal `RustWriter` (StringBuilder-based, indentation-aware) is enough (YAGNI — don't pull in a templating engine).
- Add `include 'vajram:vajram-lang:vajram-lang-rust-compiler'` to [settings.gradle](settings.gradle) right after the existing `vajram-lang-compiler` line.
- Edit `vajram-lang-grammar/build.gradle` to add `generateGrammarSource { arguments += ['-visitor'] }` so ANTLR emits `VajramBaseVisitor<T>` — needed because building an AST via the visitor pattern is far cleaner than walking `VajramBaseListener` callbacks with manual state stacks. This is the only change to the sibling module.

## 2. Compilation pipeline
```mermaid
flowchart LR
Src[".vajram source files"] --> Lex["ANTLR VajramLexer/Parser"]
Lex --> ParseTree["ANTLR ParseTree"]
ParseTree --> AstBuilder["AstBuilder (extends VajramBaseVisitor)"]
AstBuilder --> Ast["Vajram-lang AST (Java records)"]
Ast --> Resolver["SymbolTable / cross-file Resolver"]
Resolver --> RustEmitter["RustEmitter"]
RustEmitter --> RustSrc[".rs files"]
RustSrc --> Fmt["rustfmt (best-effort, optional)"]
```
- **Parse**: reuse `VajramLexer`/`VajramParser` exactly as `VajramParserTest` does today, but route syntax errors into a structured `Diagnostic` list instead of `BaseErrorListener` collecting strings (mirrors existing test pattern, but with line/col carried as data, not string).
- **AST**: one Java `record` per grammar rule that matters for codegen (`VajramDef`, `InputDecl`, `DependencyDecl`, `DependencyInvocation`, `Resolver`, `OutputBlock`, `Expr` sealed-interface hierarchy for `VarUse`/`MethodCall`/`Literal`/`Accessor(SOON|ERRABLE|DOT|combos)`/`FuncChain`/`Grouper`, etc.). Records chosen over Lombok/AutoValue for AST nodes: they're immutable data with structural equality, no builders needed, least ceremony (matches "does the standard library already cover it" — yes, records do).
- **Resolver**: builds a symbol table across all `.vajram` files under a compile root: package_decl → Rust module path, imports_decl → resolved Vajram/type references, and validates dependency invocations reference a known Vajram (by name, respecting `permits`). This is what makes multi-file compilation possible (a single `.vajram` file can depend on Vajrams defined in others).

## 3. Type & operator mapping (the core semantic decisions)
| vajram-lang | Rust mapping | Reasoning |
|---|---|---|
| `string`, `int`, `void`, user types | `String`, `i64`, `()`, same identifier | Direct value types; no boxing scheme needed for V1 |
| `T?` (errable) | `Result<T, VajramError>` | Rust's `Result` + native `?` operator is the idiomatic analog of vajram-lang's errable `?`; `VajramError` is one type defined in the prelude crate |
| `expr?method()` (`default`, `valuePresent`, `valueAbsent`, `isNil`, `value`, `error`, `errorPresent`, `errorAbsent`) | trait method call via `Errable<T>` trait implemented for `Result<T, VajramError>` | Keeps the compiler ignorant of "what these methods do" — it just emits `expr.method(args)`; behavior lives in the prelude, matching the confirmed "treat as trait method calls" approach |
| `nil` literal | `VajramError::nil()` / a `Result::Err(VajramError::NIL)` constant from prelude | Needed as a first-class desugar target since grammar treats `nil` as an expression |
| `T~` (soon) | `impl Future<Output = Result<T, VajramError>>` (or plain `Future<Output=T>` if not errable) returned from an `async fn`; `expr~` access desugars to `expr.await` | Matches confirmed tokio/futures target |
| `T~~` (later) | `tokio::task::JoinHandle<T>` from `tokio::spawn(...)` | "Later" implies detached/eagerly-started work vs. "soon" which is lazily awaited inline |
| Fanout `*` on a dependency invocation or input (`UserInfo* x = getUserInfo(userId =* ...)`) | `futures::future::join_all` / `try_join_all` over an iterator, producing `Vec<T>` | Fanout is inherently "run N async calls concurrently, collect results" — join_all is the direct idiomatic Rust equivalent |
| lambda block `arg -> { stmts }` / `{_.foo()}` | Rust closure `|arg| { stmts }` / `|it| it.foo()` | Closures transliterate almost verbatim; `_` implicit-arg convention maps to a single-param closure with an explicit chosen name (e.g. `it`) since Rust has no implicit `_` receiver in closures |
| `new Foo(args)` | `Foo::new(args)` | Rust convention for constructors; assumes user (or prelude) defines a `new` fn — compiler doesn't validate arity beyond arg count from the parse tree |
| Method chains `a.b().c()` | `a.b().c()` (verbatim) | Confirmed: no per-idiom mapping table; Rust's own method/trait resolution handles the rest at `cargo build` time, with real Rust compiler errors surfacing anything vajram-lang couldn't validate — this is intentionally where "unsupported" cases surface, rather than in our compiler |

## 4. Rust companion prelude (`vajram_rt`)
- Ship a small, hand-written Rust source folder inside the module's resources (e.g. `src/main/resources/rust-prelude/`) containing `errable.rs` (the `Errable<T>` trait + impl for `Result`), `error.rs` (`VajramError`), and a `Cargo.toml` stub.
- The compiler driver copies this prelude verbatim into the output crate's `src/vajram_rt/` on every run (simple `Files.copy`, no packaging/publishing infra needed for V1 — YAGNI on crates.io publishing).

## 5. Codegen shape per Vajram
- Each `vajram` def → one Rust module (`snake_case` file per Vajram) containing:
  - An inputs struct (`pub struct GetUserInfoInputs { user_id: String }`).
  - An injections struct if `inject(...)` present.
  - `pub async fn call(inputs: Inputs, deps: &Deps) -> Result<Output, VajramError>` as the entry point — dependency invocations become `let user_info = get_user_info::call(...).await?;`, fanout invocations become `join_all(ids.iter().map(|id| get_user_info::call(...))).await`.
- Package/import decls map to Rust `mod`/`use` statements mirroring the same path segments, written to a generated `mod.rs`/`lib.rs` tree.

## 6. CLI driver
- A `main()` (`RustCompilerMain`) taking `--src <dir> --out <dir>`, walking `.vajram` files, running the pipeline per-file after building the whole-project symbol table, and writing output. No Gradle plugin/task integration in V1 (matches existing `vajram-lang-compiler` which also has none yet) — just a runnable CLI, kept minimal.
- Best-effort `rustfmt` invocation via `ProcessBuilder` on the output dir after writing files; failures/absence of `rustfmt` are logged and non-fatal (avoid a hard dependency on the Rust toolchain being installed).

## 7. Diagnostics
- A `Diagnostic(Severity, SourceLocation, String message)` record; parser/AST-builder/resolver/emitter all append to a shared `List<Diagnostic>` returned by the driver instead of throwing, mirroring the existing `BaseErrorListener` pattern in `VajramParserTest` but promoted into a reusable structured type.

## 8. Testing strategy
- Reuse all existing fixtures in `vajram-lang-grammar/src/test/resources/*.vajram` as compiler input; golden-file tests assert generated `.rs` text matches a checked-in expected file per fixture (`src/test/resources/expected/*.rs`).
- Since a Rust toolchain is present locally (`cargo`/`rustc`/`rustfmt`), add one opt-in test (e.g. tagged/guarded by checking `rustc` is on `PATH`, skipped otherwise so CI without Rust doesn't fail) that runs `rustc --edition 2024 --crate-type lib` on the emitted output + prelude as a syntax-validity smoke check, without requiring `cargo`/crates.

## Non-goals for V1 (explicitly out of scope)
- Deep Java-stdlib semantic understanding (e.g. inferring what `IntStream.range(...).mapToObj(...)` "means") — the compiler trusts Rust's own compiler/trait resolution once code reaches the target.
- Publishing `vajram_rt` as a real crates.io crate or building a Cargo workspace/build integration.
- Gradle task/plugin wiring, IDE support, incremental compilation.
