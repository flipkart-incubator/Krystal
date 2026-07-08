# Executing and testing a Vajram graph (krystex)

Verified against `krystex/README.md` and its current source
(`krystex/src/main/java/com/flipkart/krystal/krystex/**`), and real test code under
`vajram/vajram-samples/src/test/java/com/flipkart/krystal/vajram/samples/**`. The README's class names are
slightly stale relative to the code (it says `KrystexVajramExecutor`/`KryonExecutor`); the real executor
class is `VajramKryonExecutor`. Trust the code/samples over the README's prose where they disagree.

## The one rule that governs everything: never block the event loop

A `VajramKryonExecutor` runs its entire graph on a **single-threaded event loop**. Resolver logic and
output logic are not allowed to block for any amount of time. All IO must either use genuinely
non-blocking async APIs, or be wrapped in a thread pool call that returns a `CompletableFuture`
immediately. This is why `ComputeVajramDef` output methods must never touch IO, and why `IOVajramDef`
output methods return a `CompletableFuture<T>` rather than a blocking `T`.

## Kryon vs. Vajram (only matters for understanding stack traces / behavior)

A Vajram is the type-safe, developer-facing unit. Internally, `VajramGraph` compiles every registered
Vajram (and, recursively, its dependencies) into a **Kryon** — a type-agnostic runtime primitive (values
are `Object`/`Throwable`/`HashMap` at this layer, no batching built in). You never write or reference
Kryons directly; this translation is automatic. It's useful to know when reading a stack trace that
mentions `KryonDefinition`/`Kryon` — that's the compiled form of the Vajram you wrote.

## Minimal executor setup (from real sample tests)

```java
// 1. Build a VajramGraph — loads/compiles vajram definitions into kryon definitions.
VajramGraph graph = VajramGraph.builder()
    .loadClasses(Add.class)        // or .loadFromPackage("com.foo.vajrams")
    .build();

// 2. Wrap in a KrystexGraph — wires cross-cutting decorators (batching, injection, trait dispatch).
KrystexGraph.KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(graph);

// 3. Build executor config and create the executor (one per unit of work / test).
KrystalExecutorConfigBuilder config = KrystalExecutorConfig.builder()
    .executorId("adderTest")
    .executorService(new SingleThreadExecutor("adderTest"));

try (VajramKryonExecutor executor = kGraph.build().createExecutor(config)) {
  future = executor.execute(
      Add_ReqImmutPojo._builder().numberOne(5)._build(),
      VajramExecutionConfig.builder().build());
}
assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(5);
```

Key behavioral notes:
- `VajramGraph.builder()` only exposes `.loadClasses(Class<? extends VajramDefRoot>...)` and
  `.loadFromPackage(String)` publicly (plus `.ignoreMissingDependencies(true)` if needed) — use one of
  these, not the raw builder setters.
- `executor.execute(...)` only **enqueues** the request; nothing runs until `close()` (end of the
  try-with-resources block) flushes the queue and actually submits the work. Capture the returned
  `CompletableFuture` inside the block, then assert on it after the block exits.
- Request objects are the generated `<VajramName>_ReqImmutPojo`, built via `._builder()....(fields)....
  _build()`. For a Trait, the generated request is parameterized by the concrete implementation, e.g.
  `GetPiece_ReqImmutPojo.<Knight>_builder()`.
- Only Vajrams annotated `@InvocableOutsideGraph` can be passed directly to `execute(...)` in *production*
  code — otherwise it throws `RejectedExecutionException`. This doesn't affect tests: the Krystal Gradle
  plugin auto-sets the `@TestOnly` system property `PropertyNames
  .RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME` to `true` for test runs, so any Vajram is
  directly `execute()`-able from a test with no extra setup on your part. Reserve `@InvocableOutsideGraph`
  itself for Vajrams application code genuinely calls from outside the graph — don't add it just because a
  test needs direct access.
- `VajramExecutionConfig` (per-call): `executionId`, `disabledDependentChains` (bounds recursive/cyclic
  dependency chains — required for testing a self-referential fan-out Vajram like `ChainAdd`, computed via
  `graph.computeDependentChain(vajramId, depSlot, depSlot, ...)`), `staticDispatchQualifier` (for Trait
  static dispatch).
- `KrystalExecutorConfig` (per-executor): `executorId`, `executorService` (a `SingleThreadExecutor` — lease
  one from a `SingleThreadExecutorsPool` for reuse across tests rather than constructing fresh each time),
  `kryonExecStrategy` (`DIRECT` default, or `BATCH`), `graphTraversalStrategy` (`DEPTH` default — required
  for `DIRECT`; `BREADTH` for debugging), plus decorator config maps and a `debug` flag (adds stack
  tracing / readable names, at a perf cost — fine for tests).
- Assertions: AssertJ's `assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(expected)` /
  `.failsWithin(TEST_TIMEOUT).withThrowableOfType(SomeException.class)` is the pattern used throughout the
  samples.

## Testing a batched vajram — batching does not happen by itself

A `@Batched`/`@Output.Batched` vajram is only actually batched at runtime if a batching decorator is
registered on the `KrystexGraph`. Without one, `executor.execute(...)` runs each invocation independently
(`DIRECT` strategy) exactly as if the vajram weren't batched at all — so a test that calls a batched
vajram twice and asserts a call-counter to prove coalescing **will pass or fail based on whether this
config is present, not based on the vajram's own `@Batched` annotations.** Forgetting this makes the test
silently meaningless (it'll likely fail, or worse, pass for the wrong reason if the counter isn't checked
precisely).

Wire one via `DepChainBatcherConfig.computeSharedBatcherConfig` (`com.flipkart.krystal.krystex.batching`),
passed to `kGraph.inputBatcherConfig(...)`:

```java
import static com.flipkart.krystal.krystex.batching.DepChainBatcherConfig.computeSharedBatcherConfig;

kGraph.inputBatcherConfig(computeSharedBatcherConfig(graph, _vajramId -> 100));
```

The second argument is a `BatchSizeSupplier` (`VajramID -> int`) — a trivial constant lambda is the norm in
real tests (`_v -> 100`, `_v -> 3`). `computeSharedBatcherConfig` walks the graph to find every batchable IO
vajram reachable from an entry point and registers a shared batcher for each.

**This traversal only starts from vajrams annotated `@InvocableOutsideGraph`** — it collects
`graph.vajramDefinitions()` filtered to that annotation as its root set, then walks dependency chains from
there. A batched vajram reachable only through a non-`@InvocableOutsideGraph` entry point will never get a
batcher registered, and the batching test needs a way to trigger multiple concurrent invocations of the
batched vajram within one execution anyway (a single top-level call obviously can't batch against itself).
The established pattern (`ChainAddTest`, `VajramKryonExecutorTest`) is a small `@InvocableOutsideGraph`
compute vajram that fans out over a list to the batched IO vajram, so one test execution triggers several
concurrent calls to it:

```java
@Dependency(onVajram = MyBatchedIOVajram.class, canFanout = true)
```

Then assert the batched vajram's own call-counter (a static `LongAdder`, following `Add`/`UserService`'s
pattern) is `1` despite N logical invocations, proving they were coalesced — not just that the fan-out
vajram's output is correct.

## Dependency injection into the graph

```java
kGraph.injectionProvider(
    new VajramGuiceInputInjector(
        Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(Logger.class).toInstance(myLogger);
                bind(AnalyticsEventSink.class).annotatedWith(Names.named("analytics_sink")).toInstance(sink);
              }
            })));
```

Any `@Inject`/`@Inject @Named(...)` facet on a Vajram is resolved through whatever `injectionProvider` is
configured on the `KrystexGraph` — wire one via Guice (as above) whenever a Vajram under test has injected
facets.

## Trait dispatch wiring

A graph containing a Trait dependency doesn't know which implementation to run until you register a
dispatch policy on the `KrystexGraph` — without one, executing a request against the Trait fails to resolve.
Which policy type you register must match the dispatch mechanism the Trait's authors chose (see
`references/vajram-anatomy.md`).

**Static/qualifier dispatch** — bind each qualifier value to a concrete implementation's request class via a
`TraitBinder`, then wrap it in a `GuiceyStaticDispatchPolicy`:

```java
TraitBinder traitBinder = new TraitBinder();
traitBinder.bindTrait(MultiAdd_Req.class)
    .annotatedWith(AdditionMethod.Creator.create(SIMPLE))
    .to(SimpleAdd_Req.class);
traitBinder.bindTrait(MultiAdd_Req.class)
    .annotatedWith(AdditionMethod.Creator.create(CHAIN))
    .to(ChainAdd_Req.class);

kGraph.traitDispatchPolicies(
    new TraitDispatchPolicies(
        new GuiceyStaticDispatchPolicy(
            graph, graph.getVajramIdByVajramDefType(MultiAdd.class), traitBinder)));
```

`AdditionMethod.Creator.create(...)` builds an instance of the custom `@Qualifier` annotation (the
`@AutoAnnotation`-generated factory that ships alongside a hand-written `@Qualifier` interface) — don't
hand-instantiate the annotation.

**Predicate dispatch** — build a `PredicateDispatchPolicy` via `PredicateDispatchUtil`, matching each case on
one or more `@UseForPredicateDispatch` inputs (referenced by their `_s` facet-spec constant, same as the
`getSimpleInputResolvers()` DSL):

```java
import static com.flipkart.krystal.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.krystex.traits.PredicateDispatchUtil.when;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.equalsEnum;

kGraph.traitDispatchPolicies(
    new TraitDispatchPolicies(
        dispatchTrait(GetPiece_Req.class, graph)
            .conditionally(
                when(GetPiece_Req.type_s, equalsEnum(KNIGHT)).to(GetKnight_Req.class),
                when(GetPiece_Req.type_s, equalsEnum(ROOK)).to(GetRook_Req.class))));
```

- `InputValueMatcher` (`com.flipkart.krystal.traits.matchers`) factories: `equalsEnum(enumValue)`,
  `isInstanceOf(type)` (for dispatching on which subtype was supplied), `isAnyValue()`/`isAnyNonNullValue()`
  (wildcard/fallback case). Chain `.and(otherInputSlot, otherMatcher)` on a `when(...)` case to match on more
  than one input at once.
- `InputDispatcherBuilder` also offers `.alwaysTo(ConcreteReq.class)` (single implementation, no real
  dispatch — mostly useful in tests) and `.computingTargetWith(selectorFn, targets)` for dispatch logic that
  doesn't reduce to per-input value matching.
- **Cases are evaluated in the order passed to `.conditionally(...)`, and the first matching case wins** —
  order specific combinations before broader fallbacks, not the other way around. The real
  `CustomerServiceAgent` sample dispatches on `(agentType, initialCommunication)` this way: exact
  `(L1, Call)`/`(L1, Email)`/`(L2, Call)`/`(L3, Email)` cases first, then comm-type-only fallbacks
  (`when(initialCommunication_s, isInstanceOf(Call.class)).to(DefaultCallAgent_Req.class)`), then a true
  catch-all (`when(agentType_s, isAnyValue()).and(initialCommunication_s, isAnyValue()).to(...)`) last.
  Putting the catch-all first would shadow every other case.
- **A Trait with no registered dispatch policy at all throws `IllegalStateException` the moment it's
  invoked** (`"Unknown dispatch policy: null"`), and a predicate-dispatch case set that leaves some possible
  input combination unmatched routes that request into the Trait's own definition — which has no output
  logic (Traits can't have one), so it fails there instead of surfacing a targeted error. If a Trait's
  legitimate inputs don't cleanly map into your `.conditionally(...)` cases, add an explicit catch-all case
  (`isAnyValue()`) rather than relying on every real input matching one of the specific cases.
- A request built against the Trait type (e.g. `GetPiece_ReqImmutPojo.<Knight>_builder().type(KNIGHT).
  _build()`) can be `execute()`d directly if the resolved concrete implementation (or the Trait itself) is
  reachable per the `@InvocableOutsideGraph` rule above — match `MultiAdd`'s pattern (annotate the Trait
  interface itself) if the top-level caller invokes the Trait's request type directly.

## Mocking a dependency in isolation — `VajramTestHarness`

To unit-test a Vajram without running its real dependencies (e.g. to force a specific dependency
success/failure without a real downstream call):

```java
VajramTestHarness harness = VajramTestHarness
    .prepareForTest(executorConfigBuilder, testRequestLevelCache)
    .withMock(SomeDependency_FacImmutPojo.builder()...build(), Errable.withValue(mockResponse));
    // or Errable.withError(someException) to force a failure

KrystalExecutorConfigBuilder configuredBuilder = harness.buildConfig();
// build the VajramKryonExecutor from configuredBuilder exactly as in the minimal setup above
```

`withMock` registers the mock response keyed by the dependency's Vajram ID + facet values;
`buildConfig()` installs a decorator that intercepts execution for any mocked Vajram ID and returns the
primed response instead of running its real logic. Calling `buildConfig()` twice, or combining it with
another decorator of the same type already on the config, throws `IllegalStateException` — don't
double-register.

## Choosing where the test lives

Per this repo's `CONTRIBUTING.md`/`CLAUDE.md`:
- **Module-specific unit test** (`src/test` of the module the Vajram lives in) — for a Vajram whose logic
  is locally testable within that one module.
- **End-to-end test** (in a `*-sample` project) — for a Vajram that composes across modules, so the
  cross-module wiring itself is what's being verified.

Prefer covering new Vajrams in both layers where the feature genuinely spans both concerns (unit-level
resolver/output correctness, and end-to-end graph wiring).

After writing/updating the Vajram and its test, run the full sequence from `CONTRIBUTING.md`:
`upgradeVersionLocal.macOS.sh`, then `./gradlew test --rerun-tasks -PunsafeCompile=true`, then
`./gradlew build -PunsafeCompile=true`.
