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

## Step 1 — Find how the repo already does this

Before writing anything:
- Search for existing Vajrams near the feature you're adding (same module, or a sibling domain package
  under `vajram-samples`-style projects) to match package layout, naming, and whether the repo prefers
  `@Resolve` methods vs. the `getSimpleInputResolvers()` DSL for similar cases.
- If this is the first Vajram in a module, check a sibling module's `build.gradle`/`build.gradle.kts` for
  how the `vajram-java-sdk` dependency and annotation processor are wired, and match that.
- Check `vajram/vajram-java-sdk/VAJRAM_NAMING_GUIDE.md` and `VAJRAM_DEV_GUIDE.md` directly if anything below
  is ambiguous for your case — they're short and this skill's `references/` files already fold in their
  content plus corrections against the real, current source.

## Step 2 — Pick the Vajram type

- **`ComputeVajramDef<T>`** — no blocking, no IO, no executor submission, ever. Use for logic that
  combines/transforms the outputs of other Vajrams, fans out over a collection, or implements a `Trait`.
- **`IOVajramDef<T>`** — the Vajram itself makes a non-blocking call outside the current thread (network,
  disk, another process). Output method returns `CompletableFuture<T>`, and must return quickly — kick off
  the async work and return the future, never block waiting on it.
- **`Trait`** (`interface extends TraitDef<T>`, annotated `@Trait`) — a behavioral contract with no
  implementation, when multiple concrete Vajrams need to be interchangeable behind one dependency slot
  (see Step 6).

Every Vajram is an `abstract class` (or, for a Trait, an `interface`) annotated `@Vajram` (or `@Trait`).
See `references/vajram-anatomy.md` for the full annotation reference — read it before writing the
`_Inputs`/`_InternalFacets`/`@Resolve`/`@Output` members, since getting the generated-constant usage
(`_n`/`_s` suffixes) and `@IfAbsent` semantics right the first time avoids a slow trial-and-error loop
against the codegen.

## Step 3 — Declare inputs

In an `interface _Inputs { ... }` (preferred) or `static class _Inputs { ... }` nested type, one no-arg
method (or field) per client-facing input. Every input needs an `@IfAbsent(...)` choice — this isn't
boilerplate, it's the contract the runtime enforces at execution time:
- `FAIL` — mandatory; missing input fails the Vajram. Default choice for new inputs unless there's a
  genuinely valid default.
- `ASSUME_DEFAULT_VALUE` — falls back to the Java/protobuf-style zero value (0, "", empty collection,
  false, or a `@DefaultValue`-annotated enum constant) when absent.
- `WILL_NEVER_FAIL` / `MAY_FAIL_CONDITIONALLY` — optional, for cases where the author can guarantee (or
  can't guarantee) downstream logic tolerates absence; see `references/vajram-anatomy.md` for the exact
  distinction.

Optionality is a one-way door: don't flip a mandatory input to optional or vice versa on a Vajram that
already has callers — that's a breaking change to every dependent.

Keep inputs minimal — they drive a hashcode used for caching/idempotency, so fewer and smaller inputs is
better than passing a big object when only one field of it is needed.

## Step 4 — Wire dependencies and resolvers

Dependencies on other Vajrams go in `interface _InternalFacets { ... }`, annotated `@Dependency` (plus
`@IfAbsent(...)` for what happens if the dependency never completes successfully). Runtime-injected values
(loggers, feature-flag booleans, cache invalidators) go in the same nested type, annotated `@Inject`
(optionally `@Named("...")`).

For every dependency, write a resolver that computes its inputs from already-available facets of the
current Vajram — this Vajram's own inputs, or another dependency's already-resolved output. Prefer a
static `@Resolve`-annotated method per dependency-input over the `getSimpleInputResolvers()` DSL: the
annotation processor can statically analyze `@Resolve` methods (needed for batching to work correctly and
for build-time DAG validation), but it cannot see into the DSL. Reach for the DSL only when the same
resolution shape repeats across several dependencies of a Trait implementation (see
`references/vajram-anatomy.md` for that pattern).

Resolvers must never do IO — they only compute values or a `ResolverCommand`. For a single (non-fanout)
dependency, return the value directly, or a `One2OneCommand` if you need to skip the call conditionally
(`One2OneCommand.executeWith(value)` / `skipExecution("reason")`). For a fan-out dependency
(`@Dependency(..., canFanout = true)` — invoke a dependency once per element of a collection), return a
`FanoutCommand` (`executeFanoutWith(collection)` / `skipFanout("reason")`). Always reference the
dependency's facets via its generated constants (`SomeDep_Req.someInput_n`, and `dep = someFacet_n` for the
facet on this Vajram being resolved) — never hand-written strings.

## Step 5 — Write the output method

Exactly one `@Output`-annotated method (or, for `IOVajramDef`, one whose return type is
`CompletableFuture<T>`) computes the final result once all dependencies have completed. Parameters bind by
name to this Vajram's own facets — inputs, dependency outputs, injected values — typed as the raw type
`T` (mandatory facet), `Optional<T>` (optional facet), `Errable<T>` (when you need to distinguish "empty"
from "failed"), or `FanoutDepResponses<DepReq, DepOutput>` (for a fan-out dependency — iterate via
`.requestResponsePairs()`, each pair's `.response()` is an `Errable<DepOutput>`).

If this `IOVajramDef` needs to batch multiple independent callers' inputs into one downstream call, mark
the relevant `_Inputs` members `@Batched`, and split the output logic into an `@Output.Batched` method
(receives `Collection<VajramName_BatchItem>`, makes the one combined call) and an `@Output.Unbatch` method
(maps the combined result back to `Map<VajramName_BatchItem, Errable<T>>` per caller). See
`references/vajram-anatomy.md` for the full pattern and its constraint (batched facets generally shouldn't
also be resolved via the `getSimpleInputResolvers()` DSL).

## Step 6 — Use a Trait when a dependency slot needs multiple implementations

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
policy on a `KrystexGraph`; full worked examples of both dispatch mechanisms (verbatim, from real compiling
samples) are in `references/examples.md`.

## Step 7 — Sanity-check before calling it done

- Vajram name is a verb, UpperCamelCase, with no `Node`/`Vajram`/`Kryon`/`Batch` suffix (see
  `VAJRAM_NAMING_GUIDE.md` — batching is an internal detail that can change later without a rename).
- Every input has a deliberate `@IfAbsent(...)`, not a default you didn't think about.
- Every `@Dependency` has exactly one resolver per input it needs; every fan-out dependency has exactly one
  fanout resolver.
- No resolver does IO or blocks; only `IOVajramDef` output methods touch the network/disk, and they return
  a `CompletableFuture` immediately rather than blocking on one.
- `@Resolve(dep = ..., depInputs = ...)` uses generated `_n` constants, never string literals.
- If you added a new module's first Vajram, the build wiring from Step 1 is in place.
- If the feature needs to be callable directly (not just as another Vajram's dependency), it's annotated
  `@InvocableOutsideGraph`.

## Step 8 — Write or update the test

Read `references/testing.md` for the full boilerplate reference (graph construction, executor config,
mocking dependencies). The shape of a test:

1. Build a `VajramGraph` via `VajramGraph.builder().loadClasses(YourVajram.class, ...)` (or
   `.loadFromPackage("...")`), wrap it in `KrystexGraph.builder().vajramGraph(graph).build()`.
2. Build a `VajramKryonExecutor` from a `KrystalExecutorConfig` (needs an `executorId` and an
   `executorService`), inside a try-with-resources block.
3. Call `executor.execute(YourVajram_ReqImmutPojo._builder()....._build(), VajramExecutionConfig.builder().build())`
   inside the try block, capture the returned `CompletableFuture`, and assert on it after the block closes
   (closing the executor is what actually flushes and runs the queued work).
4. If you need to stub a dependency's response rather than run its real logic (e.g. to test error paths or
   avoid a real downstream call in a unit test), use `VajramTestHarness.prepareForTest(...).withMock(...)`
   — see `references/testing.md` for the exact call shape.

Match whichever pattern (module-specific unit test vs. cross-module `*-sample`/end-to-end test) this
repo's `CLAUDE.md` calls for given the scope of the change, and after writing the vajram + test, follow the
publish/test/build sequence in `CONTRIBUTING.md`: `upgradeVersionLocal.macOS.sh`, then
`./gradlew test --rerun-tasks -PunsafeCompile=true`, then `./gradlew build -PunsafeCompile=true`.

## Reference files

- `references/vajram-anatomy.md` — full annotation reference (`@Vajram`, `@IfAbsent`, `@Dependency`,
  `@Resolve`, `@Output`/`@Output.Batched`/`@Output.Unbatch`, `@Batched`, `@Inject`, `@DataAccess`,
  `@Trait`/`@Qualifier`/`@UseForPredicateDispatch`), what's hand-written vs. codegen'd, and the naming
  rules — read this before writing `_Inputs`/`_InternalFacets`/resolvers/output methods.
- `references/testing.md` — `VajramGraph`/`KrystexGraph`/`VajramKryonExecutor` setup, request-object
  construction, `VajramTestHarness` mocking, and the non-blocking-execution rule that governs what
  output/resolver logic is allowed to do.
- `references/examples.md` — full, verbatim, real worked examples: a simple compute vajram with a default
  value, an IO vajram with batching, dependency chaining, fan-out with conditional skip, and Trait-based
  static + predicate dispatch.
