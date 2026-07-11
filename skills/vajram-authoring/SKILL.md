---
name: vajram-authoring
description: Writes business logic as Krystal Vajrams — the composable, concurrently-executed unit of logic in flipkart-incubator/Krystal (vajram-java-sdk + krystex runtime). Covers ComputeVajramDef vs IOVajramDef, _Inputs/_InternalFacets, @IfAbsent optionality, @Dependency wiring, @Resolve resolvers (fan-out via FanoutCommand, skips via One2OneCommand), @Output/@Output.Batched+@Output.Unbatch, writing Vajram Traits (@Trait/TraitDef interfaces with no implementation) and dispatching among their conforming Vajrams via static/qualifier dispatch (@Qualifier + TraitBinder) or predicate dispatch (@UseForPredicateDispatch + PredicateDispatchPolicy), and tests via VajramGraph/KrystexGraph/VajramKryonExecutor (incl. VajramTestHarness mocking and wiring trait dispatch policies). Use whenever adding/changing a Vajram, defining or implementing a Trait, modeling logic as a DAG node, wiring a dependency, adding fan-out/batching, or writing/debugging a vajram-graph test — even if the user just says "add a vajram for X", "make this pluggable/strategy-based", or "call service Y as a step in Z" without naming Krystal, as long as the repo uses Krystal/Vajram. Not for SQL table modeling or SELECT/INSERT traits — use vajram-sql-data-modelling for that.
---

# Writing business logic as Krystal Vajrams

A Vajram is a function-like, composable unit of business logic: it declares typed inputs, optional
dependencies on other Vajrams, and exactly one output. The Krystex runtime loads every Vajram's
inputs/dependencies/resolvers *statically* (without running any code) to build a DAG, so it can run
independent branches concurrently and detect call-graph problems at build time. That static analysis is
the reason the framework looks more verbose than a plain method call — every piece of it (`_Inputs`,
`@Dependency`, `@Resolve`) exists so the graph shape is knowable without executing anything.

Before writing a Vajram, decide if you should. If the logic performs no IO, directly or indirectly, and
doesn't need to be composed as a DAG node (fanned out, depended on by other vajrams, swapped via a Trait),
just write a plain static Java method — wrapping pure computation in a Vajram only adds DAG overhead for
no benefit.

## Step 1 — Find how this codebase already does this

Every team that adopts Krystal settles into its own conventions on top of the framework's rules — before
writing anything, spend a minute finding them in *this* repo rather than assuming the patterns below are
the only valid ones:
- Search this repo for existing Vajrams near the feature you're adding (same module, or a sibling domain
  package) to match its package layout, naming, and whether it prefers `@Resolve` methods vs. the
  `getSimpleInputResolvers()` DSL for similar cases.
- If this is the first Vajram in a module, check a sibling module's build file for how the `vajram-java-sdk`
  dependency and annotation processor are wired (Gradle and Maven both work; match whichever this repo
  already uses), and copy that wiring rather than inventing your own.
- If anything below is ambiguous for your case, the Krystal framework's own naming/dev guides (in the
  [flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal) repo) go into more depth —
  but this skill's `references/` files already fold in their content plus corrections against the real,
  current framework source, so you shouldn't need to leave this skill for the common cases.

## Step 2 — Agree on coding conventions

Before writing any Vajram code, confirm the style choices below with the user. If existing Vajrams in
this codebase already answer a question clearly, note the existing pattern, state the default, and ask
the user to confirm rather than re-asking from scratch. Record the answers — every Vajram authored in
this session must follow them consistently.

Present **both questions at once** in the following format exactly:

---

> **Before I write any Vajram code, I need to confirm two style conventions with you. Please answer
> both:**
>
> ---
>
> **Convention A — How should optional-facet parameters be typed?**
>
> When a `@Resolve` or `@Output` method receives a facet declared `@IfAbsent(WILL_NEVER_FAIL)` or
> `@IfAbsent(MAY_FAIL_CONDITIONALLY)`, it may be absent at runtime. Which style do you prefer for
> expressing that?
>
> **Option A1 — `@Nullable T`** *(Recommended)*
> - ✅ No wrapper object allocated — leaner on hot paths
> - ✅ Integrates with null-analysis tooling (Checker Framework, NullAway, IntelliJ inspections)
> - ✅ Cleaner signatures — null checks read naturally (`if (x == null)`)
> - ⚠️ Requires discipline: the `@Nullable` annotation is the only signal that a value may be absent;
>   forgetting to check is a runtime NPE (though tooling catches this statically)
>
> **Option A2 — `Optional<T>`**
> - ✅ The type system forces every callsite to unpack it, making absence impossible to ignore
> - ✅ Familiar Java idiom; works well if the team already standardizes on `Optional`
> - ⚠️ Extra object allocation per invocation — can matter on high-throughput paths
> - ⚠️ Verbose `.isPresent()/.get()` or `.map()/.orElse()` chains throughout resolver/output bodies
>
> *If you choose Option A1, one more sub-question:*
> **Which `@Nullable` annotation flavour?**
> - **A1a — Checker Framework** (`org.checkerframework.checker.nullness.qual.Nullable`) *(Recommended)*
>   - ✅ mature tooling, 
>   - ✅ already used throughout the Krystal framework itself; 
>   - ⚠️ heavier dependency (`checker-qual`) compared to jspecify
> - **A1b — JSpecify** (`org.jspecify.annotations.Nullable`)
>   - ✅ standardized, lighter; 
>   - ⚠️ tooling support still maturing
>
> ---
>
> **Convention B — How should dependency resolvers be structured?**
>
> **Option B1 — One `@Resolve` per dependency, `depInputs` left empty, returns `<Dep>_Req`** *(Recommended)*
>
> ```java
> @Resolve(dep = userInfo_n)
> static UserService_Req resolveUserInfo(String userId, String region) {
>     return UserService_Req._builder().userId(userId).region(region);
> }
> ```
>
> - ✅ All inputs assembled in one place — easy to read and audit
> - ✅ Most concise: one method, no `depInputs` needed, framework reads all inputs from the builder
> - ✅ **Future-proof:** empty `depInputs` means "all inputs, including any added later" — adding a
>   new input to the dependency doesn't require a new resolver, just an updated builder call
> - ⚠️ **Hard constraint:** the Vajram must have **no** other resolver for this dependency — no
>   second `@Resolve` method and no DSL entry in `getSimpleInputResolvers()`
> - ⚠️ Does not support lazy/streaming scenarios where different dependency inputs become available at
>   different points in time (uncommon in practice)
>
> **Option B2 — Per-input `@Resolve` methods (explicit `depInputs`) + DSL for single-facet resolvers**
>
> ```java
> // Exactly one dep input resolved → returns the value directly:
> @Resolve(dep = userInfo_n, depInputs = UserService_Req.userId_n)
> static String userIdForUserService(String userId, String tenantId) { ... }
>
> // Single-facet resolver → DSL inside getSimpleInputResolvers():
> // dep(userInfo_s, depInput(UserService_Req.region_s).usingAsIs(region_s).asResolver())
>
> // Two or more dep inputs resolved at once → must return a builder (same rule as B1):
> @Resolve(dep = userInfo_n, depInputs = {UserService_Req.userId_n, UserService_Req.region_n})
> static UserService_Req resolvePartial(String userId, String region) { ... }
> ```
>
> - ✅ Supports lazy/streaming use cases where dependency inputs become available at different times
> - ✅ Each resolver is maximally focused on one (or a named subset of) dependency inputs
> - ⚠️ More verbose: typically several methods where B1 uses one
> - ⚠️ Inputs scattered across methods — harder to see the full dependency request shape at a glance
> - ⚠️ Mixes two syntaxes (`@Resolve` and the DSL) in the same class
> - ⚠️ **Does not auto-cover future inputs** — adding a new input to the dependency silently leaves
>   it unresolved unless a new resolver is added

---

## Step 3 — Pick the Vajram type

- **`ComputeVajramDef<T>`** — no blocking, no IO, no executor submission, ever. Use for logic that
  combines/transforms the outputs of other Vajrams, fans out over a collection, or implements a `Trait`.
- **`IOVajramDef<T>`** — the Vajram itself makes a non-blocking call outside the current thread (network,
  disk, another process). Output method returns `CompletableFuture<T>`, and must return quickly — kick off
  the async work and return the future, never block waiting on it.
- **`Trait`** (`interface extends TraitDef<T>`, annotated `@Trait`) — a behavioral contract with no
  implementation, when multiple concrete Vajrams need to be interchangeable behind one dependency slot
  (see Step 7).

Every Vajram is an `abstract class` (or, for a Trait, an `interface`) annotated `@Vajram` (or `@Trait`).
See `references/vajram-anatomy.md` for the full annotation reference — read it before writing the
`_Inputs`/`_InternalFacets`/`@Resolve`/`@Output` members, since getting the generated-constant usage
(`_n`/`_s` suffixes) and `@IfAbsent` semantics right the first time avoids a slow trial-and-error loop
against the codegen.

## Step 4 — Declare inputs

In an `interface _Inputs { ... }` nested type, one no-arg method per client-facing input. Always use
the `interface` form — the `static class`-with-fields style is legacy and may lose codegen support in a
future framework version. Every input needs an `@IfAbsent(...)` choice — this isn't boilerplate, it's
the contract the runtime enforces at execution time:
- `FAIL` — mandatory; missing input fails the Vajram. Default choice for new inputs unless there's a
  genuinely valid default.
- `ASSUME_DEFAULT_VALUE` — falls back to the Java/protobuf-style zero value (0, "", empty collection,
  false, or a `@DefaultValue`-annotated enum constant) when absent.
- `WILL_NEVER_FAIL` / `MAY_FAIL_CONDITIONALLY` — optional, for cases where the author can guarantee (or
  can't guarantee) downstream logic tolerates absence; see `references/vajram-anatomy.md` for the exact
  distinction. When consuming these facets in `@Resolve` or `@Output` method parameters, use the style
  agreed in Convention A (Step 2).

**Never use `Optional<T>` as a facet's data type.** All facets are optional-by-default at the framework
level; optionality is declared exclusively via `@IfAbsent`. Wrapping the type in `Optional` is redundant,
and will fail in the codegen phase — declare the raw type `T` and let
`@IfAbsent` carry the optionality contract.

Optionality is a one-way door: don't flip a mandatory input to optional or vice versa on a Vajram that
already has callers — that's a breaking change to every dependent.

Keep inputs minimal — they drive a hashcode used for caching/idempotency, so fewer and smaller inputs is
better than passing a big object when only one field of it is needed.

## Step 5 — Wire dependencies and resolvers

Dependencies on other Vajrams go in `interface _InternalFacets { ... }`, annotated `@Dependency` (plus
`@IfAbsent(...)` for what happens if the dependency never completes successfully). Runtime-injected values
(loggers, feature-flag booleans, cache invalidators) go in the same nested type, annotated `@Inject`
(optionally `@Named("...")`).

**Choosing `onVajram` vs `withVajramReq`:** use `onVajram = SomeDep.class` when the dependency
implementation is on this project's compile classpath (same project or a project this one depends on).
Use `withVajramReq = SomeDep_Req.class` only when the dependency runs in a separate process and its
implementation is intentionally absent from this project's classpath. Using `withVajramReq` when the
implementation is actually on the classpath is wrong — the runtime cannot locate and wire the Vajram.

As with inputs, **never use `Optional<T>` as the return type of a facet method in `_InternalFacets`**.
Declare the raw dependency output type; optionality is expressed solely through `@IfAbsent`.

For every dependency, write a resolver that computes its inputs from already-available facets of the
current Vajram — this Vajram's own inputs, or another dependency's already-resolved output. Use
whichever resolver style the user selected in Convention B (Step 2); see `references/vajram-anatomy.md`
for the full `@Resolve` reference and both style options with examples.

Resolvers must never do IO — they only compute values or a `ResolverCommand`. For a single (non-fanout)
dependency, return `<Dep>_Req` (Option 1 / Option 2b) or the value directly (Option 2a), or wrap it
in `One2OneCommand` to skip conditionally (`One2OneCommand.executeWith(value)` /
`skipExecution("reason")`). For a fan-out dependency (`@Dependency(..., canFanout = true)` — invoke a
dependency once per element of a collection): use Option 1 / Option 2b resolvers and return
`FanoutCommand<<Dep>_ReqImmut.Builder>` — Krystal requires the concrete builder type (not `<Dep>_Req`)
for fanout resolvers for performance reasons (`executeFanoutWith(builderCollection)` /
`skipFanout("reason")`). Always reference dependency facets via
generated constants (`SomeDep_Req.someInput_n`, `dep = someFacet_n`) — never hand-written strings.

**Static imports for facet constants** — always statically import the current Vajram's own `_n`/`_s`
constants. Never statically import facet constants from other Vajrams — leave those qualified
(`OtherVajram_Req.someInput_n`) so it is always clear which Vajram a constant belongs to.

`@Resolve` methods must be **package-private** (no access modifier). They cannot be `private` because the
generated `_Wrpr` subclass must be able to invoke them, and must not be `public` or `protected` — there
is no valid reason for callers outside the package to invoke them directly.

## Step 6 — Write the output method

Exactly one `@Output`-annotated method (or, for `IOVajramDef`, one whose return type is
`CompletableFuture<T>`) computes the final result once all dependencies have completed. Parameters bind by
name to this Vajram's own facets — inputs, dependency outputs, injected values. Parameter types:
- `T` — mandatory facet (`@IfAbsent(FAIL)` / `ASSUME_DEFAULT_VALUE`).
- For optional facets (`@IfAbsent(WILL_NEVER_FAIL)` / `MAY_FAIL_CONDITIONALLY`): use whichever the
  user selected in Convention A (Step 2) — `@Nullable T` or `Optional<T>`. Apply the same choice
  consistently in `@Resolve` method parameters.
- `Errable<T>` — when you need to distinguish "absent" from "dependency failed" (independent of
  Convention A — this is a business-logic choice, not a style choice).
- `FanoutDepResponses<DepReq, DepOutput>` — for a fan-out dependency; iterate via
  `.requestResponsePairs()`, each pair's `.response()` is an `Errable<DepOutput>`.

`@Output` (and `@Output.Batched` / `@Output.Unbatch`) methods must also be **package-private** for the
same reason as `@Resolve` methods — invoked by the generated `_Wrpr` subclass, not by external callers.

If this `IOVajramDef` needs to batch multiple independent callers' inputs into one downstream call, mark
the relevant `_Inputs` members `@Batched`, and split the output logic into an `@Output.Batched` method
(receives `Collection<VajramName_BatchItem>`, makes the one combined call) and an `@Output.Unbatch` method
(maps the combined result back to `Map<VajramName_BatchItem, Errable<T>>` per caller). See
`references/vajram-anatomy.md` for the full pattern and its constraint (batched facets generally shouldn't
also be resolved via the `getSimpleInputResolvers()` DSL).

## Step 7 — Use a Trait when a dependency slot needs multiple implementations

If several concrete Vajrams should be interchangeable behind one dependency (e.g. picking a strategy, or
letting the runtime choose an implementation based on an input's value), define a Trait: an
`interface extends TraitDef<T>` annotated `@Trait` (with no `_InternalFacets`, `@Resolve`, or `@Output` of
its own — it's a contract, not an implementation), have each concrete `@Vajram` `implements` it (restating
any inherited `_Inputs`), and pick a dispatch mechanism based on *when* the choice is known:
- **Static/qualifier dispatch** — the implementation is a fixed, per-call-site wiring decision (independent
  of any particular request). Define a custom `@jakarta.inject.Qualifier` annotation, put it on each
  `@Dependency` declaration, and bind qualifier values to implementations once via a `TraitBinder` when
  building the graph.
- **Predicate dispatch** — the implementation is a genuine runtime decision based on a value inside the
  request. Mark exactly the deciding input(s) `@UseForPredicateDispatch` on the Trait's `_Inputs`, and
  register a `PredicateDispatchPolicy` (built via `PredicateDispatchUtil`) mapping input values to
  implementations when building the graph.

A single Trait can support both mechanisms at once for different call sites — see `MultiAdd` in
`references/examples.md`. Read `references/vajram-anatomy.md`'s Traits section for the full annotation
reference and `references/testing.md`'s Trait dispatch wiring section for exactly how to register either
policy on a `KrystexGraph`; full worked examples of both dispatch mechanisms (drawn from the Krystal
framework's own sample module) are in `references/examples.md`.

## Step 8 — Sanity-check before calling it done

- Vajram name is a verb, UpperCamelCase, with no `Node`/`Vajram`/`Kryon`/`Batch` suffix (see
  `references/vajram-anatomy.md`'s Naming section — batching is an internal detail that can change later
  without a rename).
- `_Inputs` and `_InternalFacets` are declared as `interface`, not `static class`.
- Every input has a deliberate `@IfAbsent(...)`, not a default you didn't think about.
- Every `@Dependency` has resolvers written in the Convention B style chosen in Step 2; every fan-out
  dependency has exactly one fanout resolver.
- Optional-facet parameters in `@Resolve` and `@Output` methods use the Convention A style (Step 2)
  consistently — `@Nullable T` or `Optional<T>`, not a mix.
- No resolver does IO or blocks; only `IOVajramDef` output methods touch the network/disk, and they return
  a `CompletableFuture` immediately rather than blocking on one.
- Expected-skip resolver paths use `skipExecution`/`skipFanout`, never `throw`.
- When throwing for unexpected errors in resolvers or output methods, throw `KrystalCompletionException`
  (or a custom subtype) rather than a plain `RuntimeException` — it skips expensive stack-trace filling
  in production and avoids `CompletableFuture` double-wrapping.
- All `@Resolve` and `@Output` methods are package-private (no access modifier) — not `private` (breaks
  `_Wrpr` invocation) and not `public`/`protected` (needless exposure).
- `@Resolve` annotation attributes use generated `_n` constants, never string literals.
- The current Vajram's own `_n`/`_s` facet constants are statically imported; other Vajrams' constants
  are left qualified.
- Platform-level constants (`IfAbsentThen.FAIL`, `ComputeDelegationMode.SYNC`, etc.) are statically
  imported.
- If you added a new module's first Vajram, the build wiring from Step 1 is in place.
- If *production* code needs to call the feature directly (not just as another Vajram's dependency), it's
  annotated `@InvocableOutsideGraph`. Don't add it just so a test can invoke the Vajram directly — see
  `references/vajram-anatomy.md` for the test-only escape hatch that covers that case instead.

## Step 9 — Write or update the test

Read `references/testing.md` for the full boilerplate reference (graph construction, executor config,
mocking dependencies). The shape of a test:

1. Build a `VajramGraph` via `VajramGraph.builder().loadClasses(YourVajram.class, ...)` (or
   `.loadFromPackage("...")`), wrap it in `KrystexGraph.builder().vajramGraph(graph).build()`.
2. Build a `VajramKryonExecutor` from a `KrystalExecutorConfig` (needs an `executorId` and an
   `executorService`), inside a try-with-resources block.
3. Call `executor.execute(YourVajram_Req._builder()....._build(), VajramExecutionConfig.builder().build())`
   inside the try block, capture the returned `CompletableFuture`, and assert on it after the block closes
   (closing the executor is what actually flushes and runs the queued work).
4. If you need to stub a dependency's response rather than run its real logic (e.g. to test error paths or
   avoid a real downstream call in a unit test), use `VajramTestHarness.prepareForTest(...).withMock(...)`
   — see `references/testing.md` for the exact call shape.

Match whichever pattern (a fast module-local unit test vs. a broader cross-module/end-to-end test) this repo
already uses for Vajrams of similar scope — check any project-level contributor docs (e.g. a `CONTRIBUTING.md`
or `CLAUDE.md`) for a required sequence first. After writing the Vajram and its test, run this repo's normal
build/test command (`./gradlew test`/`./gradlew build`, `mvn test`, or whatever it actually uses) to trigger
the annotation processor and confirm the generated code compiles — the codegen only runs as part of a real
build, so a Vajram that merely "looks right" isn't confirmed until this has run at least once.

## Reference files

- `references/vajram-anatomy.md` — full annotation reference (`@Vajram`, `@IfAbsent`, `@Dependency`,
  `@Resolve`, `@Output`/`@Output.Batched`/`@Output.Unbatch`, `@Batched`, `@Inject`, `@DataAccess`,
  `@Trait`/`@Qualifier`/`@UseForPredicateDispatch`), what's hand-written vs. codegen'd, and the naming
  rules — read this before writing `_Inputs`/`_InternalFacets`/resolvers/output methods.
- `references/testing.md` — `VajramGraph`/`KrystexGraph`/`VajramKryonExecutor` setup, request-object
  construction, `VajramTestHarness` mocking, and the non-blocking-execution rule that governs what
  output/resolver logic is allowed to do.
- `references/examples.md` — real worked examples from the Krystal framework's own sample module: a simple
  compute vajram with a default value, an IO vajram with batching, dependency chaining, fan-out with
  conditional skip, and Trait-based static + predicate dispatch.
