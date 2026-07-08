# Vajram anatomy — annotations, generated code, and naming

This reference is verified against the actual current source under
`vajram/vajram-java-sdk/src/main/java/com/flipkart/krystal/vajram/**` and the real, compiling sample
Vajrams under `vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/**`, not just the
prose docs (`README.md`, `VAJRAM_DEV_GUIDE.md`). Where the docs describe older concepts (`@VajramDef`,
`IOVajram`/`ComputeVajram` base classes, field-level `@Input`, `_Facets`), this file gives the corrected,
current syntax. Trust this file and the real samples over the prose docs when they disagree.

## Class shape

```java
@Vajram
public abstract class GetUserWithProfile extends ComputeVajramDef<UserWithProfile> {

  interface _Inputs {
    @IfAbsent(FAIL)
    String userId();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = GetUser.class)
    User user();

    @IfAbsent(FAIL)
    @Dependency(onVajram = GetUserProfile.class)
    UserProfile userProfile();
  }

  @Resolve(dep = user_n, depInputs = GetUser_Req.userId_n)
  public static One2OneCommand<String> userIdForGetUser(String userId) {
    return executeWith(userId);
  }

  @Resolve(dep = userProfile_n, depInputs = GetUserProfile_Req.userProfileId_n)
  public static One2OneCommand<String> profileIdForGetUserProfile(User user) {
    return executeWith(user.userProfileId());
  }

  @Output
  static UserWithProfile combine(String userId, User user, UserProfile userProfile) {
    return new UserWithProfile(userId, user.userProfileId(), userProfile.profileData());
  }
}
```

Base class hierarchy: `VajramDefRoot<T>` ← `VajramDef<T>` (sealed, `permits ComputeVajramDef, IOVajramDef`)
and, separately, `TraitDef<T>` (also extends `VajramDefRoot<T>`). `T` is the single output type and must be
deeply immutable (records are idiomatic) — the runtime's execution order varies run to run based on IO
latency, so a mutable input or output risks data races.

The nested types are literally named `_Inputs` and `_InternalFacets` (underscore-prefixed) — the
annotation processor scans for these exact names. `_Inputs` holds client-facing inputs (the public
contract); `_InternalFacets` holds `@Dependency` and `@Inject` facets the client never supplies.

## `@IfAbsent` — the only optionality mechanism

`com.flipkart.krystal.model.IfAbsent`, on every input/dependency facet. This replaced older
`@Mandatory`-style annotations you may see mentioned in prose docs.

- `FAIL` — mandatory. The Vajram fails if the facet is absent when needed. Default choice for new inputs.
- `ASSUME_DEFAULT_VALUE` — falls back to a zero-like default (0, `""`, empty collection, `false`, or an
  enum constant annotated `@DefaultValue`) if absent. Useful for protobuf-style interop.
- `WILL_NEVER_FAIL` — optional; the author guarantees the logic never actually fails when this is missing
  (e.g. a sensible default is always applied downstream).
- `MAY_FAIL_CONDITIONALLY` — optional, but might fail under specific conditions; requires a
  `conditionalFailureInfo` string. This is the implicit default if `@IfAbsent` is omitted on a request-only
  facet.

On a `@Dependency` facet, `@IfAbsent` describes what happens if the dependency was never invoked, or all
its (possibly fanned-out) invocations failed.

**One-way door**: never flip a facet between mandatory and optional on a Vajram that already has callers —
treat it as a breaking API change.

## `@Dependency`

`com.flipkart.krystal.vajram.facets.Dependency`, on an `_InternalFacets` member:

- `onVajram = SomeVajram.class` — direct reference to the dependency's implementation class. Only use this
  within the same buildable module; it's tight coupling (pulls the dependency's implementation onto this
  Vajram's classpath).
- `withVajramReq = SomeVajram_Req.class` — reference only the dependency's generated request class.
  Prefer this when possible — it keeps the dependency's implementation off this Vajram's compile classpath,
  so the dependency can later move to a different service/runtime without touching dependents.
- `canFanout = true` — this dependency may be invoked multiple times per invocation of the current Vajram
  (e.g. once per element of a list). Requires exactly one fanout resolver (see below).

## `@Resolve` — the primary resolver mechanism

A Vajram is responsible for computing the inputs of its own dependencies. Prefer `@Resolve`-annotated
static methods over the `getSimpleInputResolvers()` DSL (below) whenever possible — only `@Resolve`
methods are statically analyzable by the annotation processor, which matters for batching correctness and
build-time DAG validation.

```java
@Resolve(dep = userInfo_n, depInputs = UserService_Req.userId_n)
public static String userIdForUserService(String userId) { return userId; }
```

- `dep` — the qualified facet-name constant (generated, `<facetName>_n`) of the dependency facet on *this*
  Vajram being resolved. Never a hand-written string.
- `depInputs` — one or more qualified facet-name constants from the dependency's generated `<Dep>_Req`
  class, identifying which input(s) of the dependency this method resolves. A `String[]` for multiple
  (tuple) inputs.
- Parameters bind by name to already-available facets of the current Vajram: its own inputs, or another
  dependency's already-resolved output. Parameter type is the raw type `T` (mandatory facet), `Optional<T>`,
  or `Errable<T>` (optional facet).
- Return type, non-fanout dependency:
  - `T` or `Optional<T>` — value(s) bound directly to the dependency input(s).
  - `One2OneCommand<T>` — `One2OneCommand.executeWith(value)` or `.skipExecution("reason")`, when the
    resolver needs to conditionally skip the dependency call entirely.
- Return type, fanout dependency (`canFanout = true`):
  - `FanoutCommand<T>` — `FanoutCommand.executeFanoutWith(collection)` or `.skipFanout("reason")`.
  - Or a plain `Collection<T>`/`Collection<R>` (R = dependency's request type), one element per invocation.
- At most one fanout resolver per fanout dependency.
- Resolvers must never perform IO or block. Prefer `skipExecution`/`skipFanout` over throwing for
  expected-skip cases (throwing carries stack-trace-creation overhead); if you must throw for a
  performance-sensitive path, use `StackTracelessException` from `krystal-common`.
- No resolver cycles — the build fails if resolvers form a dependency cycle.

## `getSimpleInputResolvers()` — the DSL alternative

Override this default method from `VajramDef<T>` when the same resolution shape repeats across several
dependencies (typically Trait implementations selecting among multiple concrete strategies):

```java
@Override
public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
  return resolve(
      dep(sum1_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers1_s).asResolver()),
      dep(sum2_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers2_s).asResolver()),
      dep(sum3_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers3_s).asResolver()));
}
```

Constraint: the annotation processor cannot statically analyze this DSL (it can only introspect it by
constructing the object at runtime), so **resolvers written this way must not consume batched facets** —
if a dependency chain involves `@Batched` inputs, resolve it with `@Resolve` methods instead.

## `@Output`, `@Output.Batched`, `@Output.Unbatch`

Exactly one `@Output` method per Vajram computes the final result, after dependencies complete.
`ComputeVajramDef` output methods return the raw type `T`; `IOVajramDef` output methods return
`CompletableFuture<T>` and must return promptly — kick off async work and return the future, never block.

Parameters bind by name to facets, typed as `T`, `Optional<T>`, `Errable<T>`, or — for a fan-out dependency
— `FanoutDepResponses<DepReq, DepOutput>`, which exposes `.requestResponsePairs()`; each pair's
`.response()` is an `Errable<DepOutput>` (use `.valueOpt()`).

For an `IOVajramDef` that should batch multiple independent callers' calls into one downstream call:

```java
interface _Inputs {
  @IfAbsent(FAIL) @Batched String userId();
}

@Output.Batched
static CompletableFuture<Map<UserService_BatchItem, UserInfo>> callUserService(
    Collection<UserService_BatchItem> _batchItems) { ... }

@Output.Unbatch
static Map<UserService_BatchItem, Errable<UserInfo>> unbatch(
    Map<UserService_BatchItem, UserInfo> _batchedOutput) { ... }
```

- Mark the relevant `_Inputs` members `@Batched` (`com.flipkart.krystal.vajram.batching.Batched`).
  `@BatchesGroupedBy` on an internal/injected facet marks it as a partition key that must match across
  batched items (e.g. a per-tenant flag).
- `@Output.Batched` receives `Collection<<VajramId>_BatchItem>` (one generated item per caller's batched
  input values) and makes the single combined downstream call.
- `@Output.Unbatch` demultiplexes the combined result back into `Map<BatchItem, Errable<T>>`, one entry per
  caller.
- Batched Vajrams generally should not also declare `@Dependency` facets resolved via the
  `getSimpleInputResolvers()` DSL — the codegen can't classify a facet as batched-or-not unless it can
  statically trace resolution, which only `@Resolve` methods support. Prefer `@Resolve` methods for any
  dependency chain that touches a batched facet.

## `@Inject` and `@DataAccess`

- `@jakarta.inject.Inject` (optionally `@jakarta.inject.Named("qualifier")`) on an `_InternalFacets`
  member — a runtime-injected value (logger, cache invalidator, feature-flag boolean). Combine with
  `@IfAbsent(FAIL)` to make injection mandatory.
- `@DataAccess(datasetName = "...", accessPattern = QUERY|MUTATION)` (`com.flipkart.krystal.data.DataAccess`,
  repeatable via `@DataAccess.DataAccesses`), on the Vajram class — declares which logical dataset(s) this
  Vajram (transitively, including via dependencies) reads or mutates. Used for tooling/impact analysis and
  for cache-invalidation permission checks (a mutating Vajram can only invalidate cache keys of Vajrams
  declaring the same dataset — a mismatch throws `IllegalStateException` at runtime).

## `@InvocableOutsideGraph`

`com.flipkart.krystal.annos.InvocableOutsideGraph` — marks a Vajram as a valid entry point for
`executor.execute(...)`. Without it, `execute` throws `RejectedExecutionException`; the Vajram is only
reachable as another Vajram's dependency inside the graph.

This is a **production contract, not a testability requirement** — add it only when application code
outside the graph genuinely needs to call the Vajram directly. Don't add it to a Vajram purely so a test can
invoke it: the Krystal Gradle plugin automatically sets the system property
`RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME` (`com.flipkart.krystal.config.PropertyNames`,
`@TestOnly`) to `true` for test runs, which disables this check entirely — so any Vajram, annotated or not,
is directly `execute()`-able from test code with no extra setup. You never need to set this property
yourself; just don't let its existence tempt you into tagging a Vajram `@InvocableOutsideGraph` when its
only caller is a test.

## Traits — polymorphism across implementations

A `Trait` defines a behavioral contract (inputs + output shape) with **no implementation** — like a Java
`interface`/`FunctionalInterface`, Scala trait, or Rust trait. Write one whenever a dependency slot should be
fulfillable by more than one concrete Vajram (a strategy choice, an A/B'd implementation, a
type-per-subclass dispatch), so callers depend on the behavioral contract rather than one hard-coded
implementation.

### Declaring a Trait

```java
package com.flipkart.krystal.vajram.samples.chess;

@Trait
@CallGraphDelegationMode(SYNC)
public interface GetPiece<T extends Piece> extends TraitDef<T> {
  interface _Inputs {
    @UseForPredicateDispatch
    @IfAbsent(FAIL)
    PieceType type();
  }
}
```

- `@Trait` (`com.flipkart.krystal.vajram.Trait`, `@Target(TYPE)`) marks the interface.
- It must `extends TraitDef<T>` (`com.flipkart.krystal.vajram.TraitDef`, a `non-sealed interface extends
  VajramDefRoot<T>` — the same root `ComputeVajramDef`/`IOVajramDef` extend, so a Trait's generated request
  class slots into a `@Dependency` exactly like a regular Vajram's does).
- `T` is generic-friendly (`GetPiece<T extends Piece>` above) when different implementations return
  different subtypes of a common output type — each implementation binds `T` to its own concrete type
  (`GetKnight` binds it to `Knight`, `GetRook` to `Rook`).
- A Trait's `_Inputs` is optional — declare it only for inputs every implementation shares, most commonly
  the one field predicate dispatch will read (see below). A Trait has no `_InternalFacets`, `@Resolve`
  methods, or `@Output` method of its own — those all belong to the implementing Vajrams, since the Trait
  declares no logic. This isn't just a style convention: the annotation processor raises a build error
  ("Traits cannot have dependencies") if a `@Trait` interface's facets include anything but plain inputs.
- `@CallGraphDelegationMode(SYNC)` caps how much the runtime is allowed to internally reorder/delegate calls
  to conforming Vajrams; app code never has to reason about the enum values (`ComputeDelegationMode`) beyond
  copying `SYNC` here — omitting it defaults to the same value. It exists so a runtime that upgrades a Trait
  to allow more delegation can't silently break a Vajram that was written assuming less.

### Implementing a Trait

Any `@Vajram` (compute or IO) `implements` the Trait interface, generic parameter bound to its own concrete
output type, alongside its normal `ComputeVajramDef<T>`/`IOVajramDef<T>` supertype:

```java
@Vajram
abstract class GetKnight extends ComputeVajramDef<Knight> implements GetPiece<Knight> {
  interface _Inputs {
    @IfAbsent(FAIL)
    PieceType type();
  }

  @Output
  static Knight get() {
    return new Knight();
  }
}
```

The implementation restates any `_Inputs` it inherits from the Trait's contract (`type()` above) — the
Trait interface doesn't hand down field implementations, only the contract shape.

**A Vajram conforms to at most one Trait.** Many Vajrams can implement the same Trait (`GetKnight` and
`GetRook` both implement `GetPiece`), but the reverse isn't supported: the codegen picks up only the first
`@Trait`-annotated interface it finds in a Vajram's `implements` list to build that Vajram's generated
request hierarchy — a second one compiles without error but is silently ignored for that purpose. If a
Vajram genuinely needs to conform to two independent behavioral contracts, that's a sign the two contracts
should be modeled as one Trait, or that the Vajram should be split.

### Choosing a dispatch mechanism

A `@Dependency` slot typed to the Trait (or a client's top-level request typed to the Trait, if
`@InvocableOutsideGraph`) needs to resolve to exactly one concrete implementation. Two mechanisms, and the
choice is about *when the choice is known*:

- **Static/qualifier dispatch** — the implementation is fixed per call site, known at graph-build time (e.g.
  "this particular dependency slot always means the CHAIN algorithm"). Define a custom annotation
  meta-annotated `@jakarta.inject.Qualifier` (Krystal has no Trait-specific qualifier annotation of its own —
  it reuses the standard JSR-330 one), annotate each `@Dependency` declaration with a specific value, and
  bind qualifier → implementation once via a `TraitBinder` when building the graph (see
  `references/testing.md`). At most one such qualifier annotation may be present per `@Dependency` facet —
  more than one throws at graph-build time. Static dispatch only works for a Trait reached through a
  `@Dependency` facet — it can't route a Trait's request when it's executed directly from outside the graph
  (there's no dependency site for the qualifier to live on), so a Trait meant to be called directly needs
  predicate dispatch instead.
- **Predicate dispatch** — the implementation is a genuine runtime decision based on a value in the request
  (e.g. dispatching on an enum, or on which subtype of an input was supplied). Mark exactly the input(s) the
  decision depends on `@UseForPredicateDispatch` on the Trait's `_Inputs`, then register a
  `PredicateDispatchPolicy` mapping input values to implementations when building the graph (see
  `references/testing.md`). Only `@UseForPredicateDispatch`-annotated inputs are legal to key a dispatch
  case on — the framework enforces this at graph-build time, not just by convention.

A Trait isn't restricted to exactly one mechanism — `MultiAdd` in `references/examples.md` declares both a
`@UseForPredicateDispatch` input and a custom `@Qualifier`, and different call sites use whichever fits. In
practice, pick predicate dispatch when the deciding value only exists inside the request (a client picks it
at request time); pick static/qualifier dispatch when the deciding value is a wiring decision the graph
author makes once, independent of any particular request.

See `references/examples.md` for both patterns fully worked out, verbatim (`AddUsingTraits`/`MultiAdd` for
static dispatch, `GetPiece`/chess samples for predicate dispatch), and `references/testing.md` for exactly
how to wire the dispatch policy into a `KrystexGraph` for tests.

## What's hand-written vs. codegen'd

You write: the abstract `@Vajram`/`@Trait` class or interface, its `_Inputs`/`_InternalFacets` nested
types, `@Resolve` methods, the `@Output` method(s), optionally a `getSimpleInputResolvers()` override, and
plain domain POJOs/records used as input/output/dependency types.

Codegen produces (never hand-edit or instantiate these directly):
- `<VajramId>_Req` / `<VajramId>_ReqImmutPojo` — the request type, built via `._builder()...._build()`,
  used both by clients constructing a top-level request and by other Vajrams' `@Resolve` methods
  referencing this Vajram's input facet constants (`<VajramId>_Req.someInput_n`).
- `<VajramId>_Fac` — facet-spec constants: `<facetName>_n` (qualified name, for `@Resolve(dep = ...)`) and
  `<facetName>_s` (facet spec, for the `getSimpleInputResolvers()` DSL), for every facet.
- `<VajramId>_BatchItem` — for batched Vajrams, one row per unbatched caller, with a
  `._pojoBuilder()...._build()` builder, used in `@Output.Unbatch`.
- `<VajramId>Impl` — the final class implementing all the framework-internal plumbing. Framework-internal
  only; you never reference it.

Build commands trigger this: `./gradlew build` (or `./gradlew krystalModelsGen` for just model generation).
A bare Vajram with nothing depending on it or referencing it can still trigger codegen once it's loaded
into a `VajramGraph`; don't be surprised if nothing appears to happen until something actually exercises
the class.

## Naming (from `VAJRAM_NAMING_GUIDE.md`)

1. Vajram names are verbs, UpperCamelCase: `GetX` (retrieval), `UpdateY` (mutation), `GetAndUpdateZ`,
   `LoginUser`, `SendSMS`. The Vajram ID is the class's simple name unless overridden, and — unlike a plain
   Java class name — is globally unique across the whole platform, not just per-package: moving the file to
   a different package/module/repo doesn't change its identity.
2. Never include `Node`, `Vajram`, `Kryon`, or `Batch` in the name. These are internal implementation
   details (every Vajram maps to one Kryon; batching can be added or removed later) that shouldn't leak
   into a name describing *what the Vajram does*.
3. Keep names short — the Vajram ID prefixes every generated class name (`<Id>_Req`, `<Id>_Fac`,
   `<Id>_BatchItem`, `<Id>Impl`), so a long Vajram name cascades into unwieldy generated names.
4. Input/facet names: lowerCamelCase, descriptive, unique within the Vajram.
