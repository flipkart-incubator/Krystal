# Vajram anatomy — annotations, generated code, and naming

This reference is verified against the Krystal framework's actual current source
(`com.flipkart.krystal.vajram.**` in `vajram-java-sdk`) and its own real, compiling sample Vajrams, in
[flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal) — not just the framework's prose
docs. That framework repo is a separate codebase from whatever repo you're authoring Vajrams in; you won't
find these files locally unless you're working inside Krystal itself. Where the framework's own prose docs
describe older concepts (`@VajramDef`, `IOVajram`/`ComputeVajram` base classes, field-level `@Input`,
`_Facets`), this file gives the corrected, current syntax — trust this file (and the class/annotation names
it cites) over anything older you might find in blog posts or outdated internal docs.

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
  static One2OneCommand<String> userIdForGetUser(String userId) {
    return executeWith(userId);
  }

  @Resolve(dep = userProfile_n, depInputs = GetUserProfile_Req.userProfileId_n)
  static One2OneCommand<String> profileIdForGetUserProfile(User user) {
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

**Always declare both as `interface` with no-arg methods.** The older `static class`-with-fields style
is legacy and may lose codegen support in a future framework version — do not use it for new code.

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

**Facet data types must never be `Optional<T>`.** All facets are optional-by-default at the framework
level; the `@IfAbsent` annotation is the sole mechanism for expressing optionality. Always declare the raw
return type `T` on `_Inputs` and `_InternalFacets` methods — wrapping in `Optional` is incorrect, is not
understood by the codegen, and will produce confusing behavior. `Optional<T>` is valid *only* as a
parameter type in `@Resolve` or `@Output` methods, where it means "give me the value if present".

**One-way door**: never flip a facet between mandatory and optional on a Vajram that already has callers —
treat it as a breaking API change.

## `@Dependency`

`com.flipkart.krystal.vajram.facets.Dependency`, on an `_InternalFacets` member:

- `onVajram = SomeVajram.class` — use when the dependency Vajram's implementation is on this project's
  compile classpath, i.e. it lives in the same project or in a project that this project directly depends
  on. This is the common case for intra-service dependencies.
- `withVajramReq = SomeVajram_Req.class` — use **only** when the dependency Vajram runs in a separate
  process and its implementation class is intentionally **not** on this project's classpath (e.g. a
  cross-service call whose impl lives in another deployable). Using `withVajramReq` when the implementation
  is actually on the classpath is incorrect — the runtime won't be able to locate and wire the Vajram.
- `canFanout = true` — this dependency may be invoked multiple times per invocation of the current Vajram
  (e.g. once per element of a list). Requires exactly one fanout resolver (see below).

## `@Resolve` — the primary resolver mechanism

A Vajram is responsible for computing the inputs of its own dependencies using `@Resolve`-annotated
package-private static methods. Two project-level style options exist (choose one per project via
Convention B in the skill's Step 2 — the choice applies uniformly across all Vajrams in the codebase):

### Option 1 *(Recommended)* — one `@Resolve` per dependency, returning the req builder

Resolves **all** inputs of a dependency in a single method by leaving `depInputs` unset (empty default)
and returning the dependency's **request builder**. Declare the return type as `<Dep>_Req` — the
generated builder (`<Dep>_ReqImmut.Builder`) extends `<Dep>_Req`, so this compiles and reads more
concisely. The framework interprets an empty `depInputs` as "resolve every input of this dependency".
Do **not** call `._build()` — return the builder itself. Instantiate via `<Dep>_Req._builder()`.

```java
@Resolve(dep = userInfo_n)
static UserService_Req resolveUserInfo(String userId, String region) {
    return UserService_Req._builder().userId(userId).region(region);
}
```

Two important properties of the empty-`depInputs` form:

1. **Covers future inputs automatically.** Because it is interpreted as "all inputs, including any
   added later", adding a new input to the dependency does not require creating a new resolver in the
   calling Vajram — the existing method just needs the new input set on the builder.
2. **No other resolvers allowed.** A Vajram that has a resolver with empty `depInputs` for a given
   dependency **must not** have any other resolver for that same dependency — neither another
   `@Resolve` method nor an entry in `getSimpleInputResolvers()`. This is a hard framework constraint.

### Option 2 — per-input resolvers (plus DSL for single-facet resolvers)

Set `depInputs` to one or more `<Dep>_Req.<inputName>_n` constants to resolve specific inputs
individually. The framework enforces different return-type rules depending on how many constants you
list:

**Sub-form 2a — exactly one `depInputs` entry** (typical case for Option 2):
The method resolves one dependency input and returns a single value.

```java
// One facet consumed to resolve one dep input:
@Resolve(dep = userInfo_n, depInputs = UserService_Req.userId_n)
static String userIdForUserService(String userId, String tenantId) { ... }

// Single-facet → DSL (getSimpleInputResolvers):
// dep(userInfo_s, depInput(UserService_Req.region_s).usingAsIs(region_s).asResolver())
```

**Sub-form 2b — two or more `depInputs` entries** (partial bulk-resolution):
The method resolves multiple dependency inputs at once and **must** return a `<Dep>_Req` (same as
Option 1) — the framework requires a request/builder whenever more than one input is being set.

```java
@Resolve(dep = userInfo_n, depInputs = {UserService_Req.userId_n, UserService_Req.region_n})
static UserService_Req resolveUserInfoPartial(String userId, String region) {
    return UserService_Req._builder().userId(userId).region(region);
}
```

More verbose than Option 1, but supports lazy/streaming use cases where individual dependency inputs
become available at different times.

### Common rules for both options

- `dep` — always the generated `<facetName>_n` constant of the dependency facet on *this* Vajram. Never
  a hand-written string.
- `depInputs` — omit (empty) for Option 1; one or more `<Dep>_Req.<inputName>_n` constants for
  Option 2. Never hand-written strings.
- Parameters bind by name to already-available facets of the current Vajram: its own inputs or another
  dependency's already-resolved output. Parameter type options:
  - `T` — mandatory facet (`@IfAbsent(FAIL)` or `ASSUME_DEFAULT_VALUE`).
  - `@Nullable T` or `Optional<T>` — optional facet; use whichever the project chose in Convention A.
  - `Errable<T>` — only when you need to distinguish "value was absent" from "dependency failed".
- **Return type — non-fanout dependency, determined by `depInputs` length:**
  - `depInputs` **empty** (Option 1) or **2+ entries** (Option 2b): return `<Dep>_Req`
    (the builder from `<Dep>_Req._builder()` is a `<Dep>_Req`; do not call `._build()`), or
    `One2OneCommand<<Dep>_Req>` to conditionally skip.
  - `depInputs` **exactly one entry** (Option 2a): return `T`, `Optional<T>`, or
    `One2OneCommand<T>` to conditionally skip.
- **Return type — fanout dependency** (`canFanout = true`):
  - For Option 1 (empty `depInputs`) and Option 2b (2+ `depInputs`): must return
    `FanoutCommand<<Dep>_ReqImmut.Builder>` — Krystal enforces the concrete builder type (not
    `<Dep>_Req`) for fanout resolvers for performance reasons. Use
    `FanoutCommand.executeFanoutWith(builderCollection)` or `.skipFanout("reason")`.
  - For Option 2a (single `depInputs`): return `FanoutCommand<T>` where `T` is the single input's
    value type, or a plain `Collection<T>`, one element per invocation.
- At most one fanout resolver per fanout dependency.
- Resolvers must never perform IO or block.
- **For expected-skip cases, always use `skipExecution`/`skipFanout` — never throw.** Throwing creates a
  stack trace even when the exception is caught immediately, which is expensive on hot paths. Only throw
  for genuinely unexpected errors.
- **When you must throw**, prefer `KrystalCompletionException`
  (`com.flipkart.krystal.except.KrystalCompletionException`) or a custom subtype over a plain
  `RuntimeException`. Benefits:
  - Stack-trace filling is controlled per-thread via `KrystalExceptions.getStackTracingStrategyForCurrentThread()`
    (`FILL` / `DEFAULT` / `DONT_FILL`) — production threads can skip the expensive `fillInStackTrace`
    call while debugging threads still get full traces.
  - It extends `CompletionException` directly, so `CompletableFuture` does not double-wrap it in
    another `CompletionException` (double-wrapping has been observed to cost up to 20% extra CPU).
  - Use the static helper `KrystalCompletionException.wrapAsCompletionException(Throwable)` to safely
    wrap an arbitrary exception without double-wrapping.
  - Create a domain-specific subtype (e.g. `class UserNotFoundException extends KrystalCompletionException`)
    for better caller-side error categorisation without sacrificing any of the above.
- No resolver cycles — the build fails if resolvers form a dependency cycle.
- **Access modifier**: `@Resolve` methods must be package-private (no modifier). They cannot be `private`
  because the generated `_Wrpr` subclass invokes them; they must not be `public` or `protected` because
  nothing outside the package should call them directly.

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

Parameters bind by name to facets. Supported parameter types:
- `T` — mandatory facet (`@IfAbsent(FAIL)` / `ASSUME_DEFAULT_VALUE`).
- `@Nullable T` or `Optional<T>` — optional facet (`@IfAbsent(WILL_NEVER_FAIL)` /
  `MAY_FAIL_CONDITIONALLY`); use whichever the project chose in Convention A (skill Step 2).
  `@Nullable T` options: `org.checkerframework.checker.nullness.qual.Nullable` (recommended) or
  `org.jspecify.annotations.Nullable`.
- `Errable<T>` — when you need to distinguish "absent" from "dependency failed" (business-logic choice,
  independent of Convention A).
- `FanoutDepResponses<DepReq, DepOutput>` — for a fan-out dependency; exposes `.requestResponsePairs()`,
  each pair's `.response()` is an `Errable<DepOutput>` (use `.valueOpt()`).

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
- **Access modifier**: `@Output`, `@Output.Batched`, and `@Output.Unbatch` methods must be package-private
  (no modifier). They cannot be `private` because the generated `_Wrpr` subclass invokes them; they must
  not be `public` or `protected` because nothing outside the package should call them directly.

## `@Inject` and `@DataAccess`

- `@jakarta.inject.Inject` (optionally `@jakarta.inject.Named("qualifier")`) on an `_InternalFacets`
  member — a runtime-injected value (logger, cache invalidator, feature-flag boolean). Combine with
  `@IfAbsent(FAIL)` to make injection mandatory.
- `@DataAccess(datasetName = "...", accessPattern = QUERY|MUTATION)` (`com.flipkart.krystal.data.DataAccess`,
  repeatable via `@DataAccess.DataAccesses`), on the Vajram class — declares which logical dataset(s) this
  Vajram (transitively, including via dependencies) reads or mutates. Used for tooling/impact analysis and
  for cache-invalidation permission checks (a mutating Vajram can only invalidate cache keys of Vajrams
  declaring the same dataset — a mismatch throws `IllegalStateException` at runtime).

## `@SupportedModelProtocol` — usually omit it

`com.flipkart.krystal.model.SupportedModelProtocol`, on a `@ModelRoot`/`Model` type (a Vajram's output type, an
`_Inputs`/`_InternalFacets` facet type, or any nested type used as one), declares which wire protocol(s) the model
can be (de)serialized as (JSON, protobuf, Fory, plain Java object, ...). **Plain Java object is what the codegen
falls back to whenever no `@SupportedModelProtocol` is declared at all**, so `@SupportedModelProtocol
(PlainJavaObject.class)` on its own says nothing the absence of the annotation didn't already say — don't add it.
Reach for it only when a model genuinely needs a non-default protocol (or more than one at once, since it's
`@Repeatable`), e.g. a Vajram output that also needs to cross a gRPC boundary as protobuf.

## `@InvocableOutsideGraph`

`com.flipkart.krystal.annos.InvocableOutsideGraph` — marks a Vajram as a valid entry point for
`executor.execute(...)`. Without it, `execute` throws `RejectedExecutionException`; the Vajram is only
reachable as another Vajram's dependency inside the graph.

This is a **production contract, not a testability requirement** — add it only when application code
outside the graph genuinely needs to call the Vajram directly. Don't add it to a Vajram purely so a test can
invoke it: on Gradle, Krystal's own Gradle plugin automatically sets the system property
`RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME` (`com.flipkart.krystal.config.PropertyNames`,
`@TestOnly`) to `true` for `Test` tasks, which disables this check entirely — so any Vajram, annotated or
not, is directly `execute()`-able from test code with no extra setup on your part. If this repo doesn't use
the Krystal Gradle plugin (e.g. it's a Maven build, or a Gradle build that didn't apply the plugin), set that
system property yourself in test setup instead of reaching for `@InvocableOutsideGraph` — either way, don't
let this annotation's existence tempt you into adding it just because a test needs direct access.

## Traits — polymorphism across implementations

A `Trait` defines a behavioral contract (inputs + output shape) with **no implementation** — like a Java
`interface`/`FunctionalInterface`, Scala trait, or Rust trait. Write one whenever a dependency slot should be
fulfillable by more than one concrete Vajram (a strategy choice, an A/B'd implementation, a
type-per-subclass dispatch), so callers depend on the behavioral contract rather than one hard-coded
implementation.

### Declaring a Trait

```java
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
- `<VajramId>_Req` / `<VajramId>_ReqImmutPojo` — the request type. Prefer `<VajramId>_Req._builder()...._build()`
  to construct requests; `_Req` is the generated interface and its `_builder()` delegates to the
  concrete `_ReqImmutPojo`. Use `_ReqImmutPojo.<T>_builder()` only when you need to supply a generic
  type parameter (e.g. parameterized Trait requests). Used both by clients constructing top-level
  requests and by `@Resolve` methods referencing input facet constants (`<VajramId>_Req.someInput_n`).
- `<VajramId>_Fac` — facet-spec constants: `<facetName>_n` (qualified name, for `@Resolve(dep = ...)`) and
  `<facetName>_s` (facet spec, for the `getSimpleInputResolvers()` DSL), for every facet.
- `<VajramId>_BatchItem` — for batched Vajrams, one row per unbatched caller, with a
  `._pojoBuilder()...._build()` builder, used in `@Output.Unbatch`.
- `<VajramId>Impl` — the final class implementing all the framework-internal plumbing. Framework-internal
  only; you never reference it.

This repo's normal build command triggers it (`./gradlew build`/`./gradlew krystalModelsGen` for Gradle
projects, `mvn compile` for Maven — whichever this repo actually uses). A bare Vajram with nothing depending
on it or referencing it can still trigger codegen once it's loaded into a `VajramGraph`; don't be surprised
if nothing appears to happen until something actually exercises the class.

## Naming

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

## Fully-qualified names — annotations and platform classes

Use these when writing import statements. If any name below causes a compilation error after a
framework upgrade, look in the framework jars on the classpath (or in the
`flipkart-incubator/Krystal` source repo) for the current package — don't guess.

### Annotations

| Annotation | Fully-qualified name |
|---|---|
| `@Vajram` | `com.flipkart.krystal.vajram.Vajram` |
| `@Trait` | `com.flipkart.krystal.vajram.Trait` |
| `@CallGraphDelegationMode` | `com.flipkart.krystal.annos.CallGraphDelegationMode` |
| `@InvocableOutsideGraph` | `com.flipkart.krystal.annos.InvocableOutsideGraph` |
| `@IfAbsent` | `com.flipkart.krystal.model.IfAbsent` |
| `@DefaultValue` | `com.flipkart.krystal.model.DefaultValue` |
| `@SupportedModelProtocol` | `com.flipkart.krystal.model.SupportedModelProtocol` |
| `@Dependency` | `com.flipkart.krystal.vajram.facets.Dependency` |
| `@Resolve` | `com.flipkart.krystal.vajram.facets.resolution.Resolve` |
| `@Output` | `com.flipkart.krystal.vajram.facets.Output` |
| `@Batched` | `com.flipkart.krystal.vajram.batching.Batched` |
| `@BatchesGroupedBy` | `com.flipkart.krystal.vajram.batching.BatchesGroupedBy` |
| `@UseForPredicateDispatch` | `com.flipkart.krystal.traits.UseForPredicateDispatch` |
| `@DataAccess` | `com.flipkart.krystal.data.DataAccess` |
| `@Inject` | `jakarta.inject.Inject` |
| `@Named` | `jakarta.inject.Named` |
| `@Qualifier` | `jakarta.inject.Qualifier` |
| `@Nullable` (checkerframework) | `org.checkerframework.checker.nullness.qual.Nullable` |
| `@Nullable` (jspecify) | `org.jspecify.annotations.Nullable` |

### Vajram base classes

| Class | Fully-qualified name |
|---|---|
| `ComputeVajramDef<T>` | `com.flipkart.krystal.vajram.ComputeVajramDef` |
| `IOVajramDef<T>` | `com.flipkart.krystal.vajram.IOVajramDef` |
| `VajramDef<T>` | `com.flipkart.krystal.vajram.VajramDef` |
| `VajramDefRoot<T>` | `com.flipkart.krystal.vajram.VajramDefRoot` |
| `TraitDef<T>` | `com.flipkart.krystal.vajram.TraitDef` |

### Resolver and output helpers

| Class | Fully-qualified name |
|---|---|
| `One2OneCommand<T>` | `com.flipkart.krystal.vajram.facets.One2OneCommand` |
| `FanoutCommand<T>` | `com.flipkart.krystal.vajram.facets.FanoutCommand` |
| `FanoutDepResponses<Req, Out>` | `com.flipkart.krystal.data.FanoutDepResponses` |
| `Errable<T>` | `com.flipkart.krystal.data.Errable` |
| `NonNil<T>` | `com.flipkart.krystal.data.NonNil` |
| `ResolverCommand<T>` | `com.flipkart.krystal.facets.resolution.ResolverCommand` |
| `SimpleInputResolver` | `com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver` |
| `SimpleInputResolverSpec` | `com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec` |

### Graph construction and execution

| Class | Fully-qualified name |
|---|---|
| `VajramGraph` | `com.flipkart.krystal.krystex.VajramGraph` |
| `KrystexGraph` | `com.flipkart.krystal.krystex.KrystexGraph` |
| `VajramKryonExecutor` | `com.flipkart.krystal.krystex.kryon.VajramKryonExecutor` |
| `KrystalExecutorConfig` | `com.flipkart.krystal.krystex.KrystalExecutorConfig` |
| `VajramExecutionConfig` | `com.flipkart.krystal.krystex.kryon.VajramExecutionConfig` |
| `VajramID` | `com.flipkart.krystal.core.VajramID` |

### Testing

| Class | Fully-qualified name |
|---|---|
| `VajramTestHarness` | `com.flipkart.krystal.krystex.testharness.VajramTestHarness` |

### Concurrency

| Class | Fully-qualified name |
|---|---|
| `SingleThreadExecutor` | `com.flipkart.krystal.concurrent.SingleThreadExecutor` |
| `SingleThreadExecutorsPool` | `com.flipkart.krystal.concurrent.SingleThreadExecutorsPool` |

### Trait dispatch

| Class | Fully-qualified name |
|---|---|
| `TraitDispatchPolicies` | `com.flipkart.krystal.traits.TraitDispatchPolicies` |
| `TraitBinder` | `com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder` |
| `GuiceyStaticDispatchPolicy` | `com.flipkart.krystal.vajram.guice.traitbinding.GuiceyStaticDispatchPolicy` |
| `PredicateDispatchUtil` | `com.flipkart.krystal.krystex.traits.PredicateDispatchUtil` |
| `InputValueMatcher` | `com.flipkart.krystal.traits.matchers.InputValueMatcher` |

### Batching

| Class | Fully-qualified name |
|---|---|
| `DepChainBatcherConfig` | `com.flipkart.krystal.krystex.batching.DepChainBatcherConfig` |

### Injection and caching

| Class | Fully-qualified name |
|---|---|
| `VajramGuiceInputInjector` | `com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector` |
| `RequestLevelCacheInvalidator` | `com.flipkart.krystal.krystex.caching.RequestLevelCacheInvalidator` |

### Exceptions

| Class | Fully-qualified name |
|---|---|
| `KrystalCompletionException` | `com.flipkart.krystal.except.KrystalCompletionException` |
| `KrystalExceptions` (thread strategy util) | `com.flipkart.krystal.except.KrystalExceptions` |

## Import style (rules, not conventions)

**Platform-level constants** (`IfAbsentThen.FAIL`, `ComputeDelegationMode.SYNC`,
`equalsEnum`/`isInstanceOf` from `InputValueMatcher`, and similar) must always be statically imported.
Qualifying them inline (`IfAbsent.IfAbsentThen.FAIL`) is unnecessarily verbose and inconsistent with
every real Krystal sample.

**Codegen-generated facet constants** (`_n` / `_s` suffixes) follow a fixed rule:
- **Always statically import** the current Vajram's own facet constants (e.g. `userId_n`, `userInfo_n`,
  `numbers_s`). This is the norm in all Vajram method bodies and `@Resolve` / `@Output` signatures.
- **Never statically import** facet constants from other Vajrams. Leave them qualified
  (`OtherVajram_Req.someInput_n`, `GetPiece_Req.type_s`) so it is always unambiguous which Vajram's
  constant is being referenced.
