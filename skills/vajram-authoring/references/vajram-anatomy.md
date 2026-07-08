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

## Traits — polymorphism across implementations

A `Trait` defines a behavioral contract (inputs + output shape) with no implementation:

```java
@Trait
@CallGraphDelegationMode(SYNC)
@InvocableOutsideGraph
public interface MultiAdd extends TraitDef<Integer> {
  interface _Inputs {
    @UseForPredicateDispatch
    @IfAbsent(FAIL)
    List<Integer> numbers();
  }

  @Qualifier
  @Retention(RUNTIME)
  @Target({FIELD, METHOD})
  @interface AdditionMethod {
    MultiAddType value();
  }

  enum MultiAddType { SIMPLE, CHAIN, SPLIT }
}
```

Concrete Vajrams `implements` the Trait interface. A dependency slot typed to the Trait can be fulfilled by
different implementations, selected by:

- **Static/qualifier dispatch** — define a custom `@Qualifier` annotation (like `@AdditionMethod` above)
  and annotate each `@Dependency` declaration with a specific value; resolved at graph-build time /
  per-call-site.
- **Predicate dispatch** — mark a Trait input `@UseForPredicateDispatch`; the runtime picks the
  implementation based on that input's actual value at request time (e.g. dispatching on an enum or an
  `instanceof` check).

See `references/examples.md` for both patterns fully worked out (`AddUsingTraits`/`MultiAdd` for static
dispatch, `GetPiece`/chess samples for predicate dispatch).

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
