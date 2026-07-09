# Worked examples

All examples below are drawn (verbatim or near-verbatim, trimmed of unrelated imports) from the Krystal
framework's own sample module, in
[flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal/tree/main/vajram/vajram-samples).
That's a separate codebase from wherever you're authoring Vajrams — these classes, and the packages they're
named after (`calculator.add`, `greet`, `chess`, ...), won't exist in your repo. Treat every class/package
name below as illustrative naming from the framework's own samples, not a convention you need to match; the
point is the annotation usage and structure, which is real and current regardless of which codebase you're
writing in. Read `vajram-anatomy.md` first for what each annotation means; this file is for pattern-matching
your own case against a real one.

## 1. Simplest possible compute vajram, with a default value

`SimpleAdd` — no dependencies, one input with `ASSUME_DEFAULT_VALUE`, implements a Trait (`MultiAdd`, see
example 7):

```java
@Vajram
public abstract class SimpleAdd extends ComputeVajramDef<Integer> implements MultiAdd {
  interface _Inputs {
    @IfAbsent(ASSUME_DEFAULT_VALUE)
    List<Integer> numbers();
  }

  @Output
  public static int output(List<Integer> numbers) {
    return numbers.stream().mapToInt(i -> i).sum();
  }
}
```

## 2. IO vajram with input batching, and an injected control flag

`Add` — batched inputs, `@Output.Batched` + `@Output.Unbatch`, an injected boolean used to simulate failure,
a static counter for test assertions:

```java
@Slf4j
@Vajram
@DataAccess(datasetName = "Global")
public abstract class Add extends IOVajramDef<Integer> {

  interface _Inputs {
    @IfAbsent(FAIL)
    @Batched
    int numberOne();

    @IfAbsent(ASSUME_DEFAULT_VALUE)
    @Batched
    int numberTwo();
  }

  interface _InternalFacets {
    @BatchesGroupedBy
    @Named(FAIL_ADDER_FLAG)
    @Inject
    boolean fail();
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();
  public static final String FAIL_ADDER_FLAG = "failAdder";

  @Output.Batched
  static CompletableFuture<BatchAddResult> batchedOutput(
      Collection<Add_BatchItem> _batchItems, @Nullable Boolean fail) {
    CALL_COUNTER.increment();
    if (TRUE.equals(fail)) {
      throw new RuntimeException("Adder failed because fail flag was set");
    }
    return supplyAsync(() -> new BatchAddResult(
        _batchItems.stream().collect(toImmutableMap(
            addBatchItem -> ImmutableList.of(addBatchItem.numberOne(), addBatchItem.numberTwo()),
            batch -> add(batch.numberOne(), batch.numberTwo())))));
  }

  @Output.Unbatch
  static Map<Add_BatchItem, Errable<Integer>> unbatchOutput(
      Collection<Add_BatchItem> _batchItems, Errable<BatchAddResult> _batchedOutput) {
    if (!(_batchedOutput instanceof NonNil<BatchAddResult> nonNil)) {
      return ImmutableMap.of();
    }
    return nonNil.value().result().entrySet().stream().collect(toImmutableMap(
        entry -> Add_BatchItem._pojoBuilder()
            .numberOne(entry.getKey().get(0)).numberTwo(entry.getKey().get(1))._build(),
        entry -> Errable.withValue(entry.getValue())));
  }

  public static int add(int a, int b) { return a + b; }

  record BatchAddResult(ImmutableMap<ImmutableList<Integer>, Integer> result) {}
}
```

Matching test — the canonical minimal executor pattern:

```java
VajramGraph graph = VajramGraph.builder().loadClasses(Add.class).build();
KrystexGraph.KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(graph);

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

## 3. Dependency injection + composing a vajram dependency

`Greet` — a compute vajram with an injected `Logger` and a `@Named` sink, plus a dependency on an IO vajram
(`UserService`, example 4):

```java
@InvocableOutsideGraph
@Vajram
public abstract class Greet extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String userId;
  }

  static class _InternalFacets {
    @Inject Logger log;

    @IfAbsent(FAIL)
    @Inject
    @Named("analytics_sink")
    AnalyticsEventSink analyticsEventSink;

    @Dependency(onVajram = UserService.class)
    UserInfo userInfo;
  }

  @Resolve(dep = userInfo_n, depInputs = UserService_Req.userId_n)
  public static String userIdForUserService(String userId) {
    return userId;
  }

  @Output
  static String createGreetingMessage(
      String userId,
      @Nullable UserInfo userInfo,
      @Nullable Logger log,
      AnalyticsEventSink analyticsEventSink) {
    String userName = userInfo != null ? userInfo.userName() : "friend";
    String greeting = "Hello " + userName + "! Hope you are doing well!";
    if (log != null) {
      log.log(Level.INFO, greeting);
      log.log(Level.INFO, "Greeting user " + userId);
    }
    analyticsEventSink.pushEvent("event_type", new GreetingEvent(userId, greeting));
    return greeting;
  }
}
```

Note this example uses the older field-style `_Inputs`/`_InternalFacets` (fields instead of interface
methods) — both styles compile and are seen in real code; prefer the `interface` method style shown in
other examples for new code, as it's the more common current convention.

## 4. IO vajram with batching, as a dependency

`UserService`:

```java
@Vajram
@DataAccess(datasetName = "User")
public abstract class UserService extends IOVajramDef<UserInfo> {

  interface _Inputs {
    @IfAbsent(FAIL)
    @Batched
    String userId();
  }

  @Output.Batched
  static CompletableFuture<Map<UserService_BatchItem, UserInfo>> callUserService(
      Collection<UserService_BatchItem> _batchItems) {
    return completedFuture(
        _batchItems.stream()
            .collect(
                Collectors.toMap(
                    identity(),
                    batch ->
                        new UserInfo(
                            batch.userId(), "Firstname Lastname (%s)".formatted(batch.userId())))));
  }

  @Output.Unbatch
  static Map<UserService_BatchItem, Errable<UserInfo>> unbatch(
      Map<UserService_BatchItem, UserInfo> _batchedOutput) {
    return _batchedOutput.entrySet().stream()
        .collect(toImmutableMap(Map.Entry::getKey, entry -> Errable.withValue(entry.getValue())));
  }
}
```

## 5. Fan-out over a list, with cache invalidation via an injected facet

`GetUserProfilesFromUserIds` — a compute vajram fanning out to a dependency once per element of a list,
reading the fanned-out responses via `FanoutDepResponses`:

```java
@Vajram
@DataAccess(datasetName = "UserProfile")
public abstract class GetUserProfilesFromUserIds extends ComputeVajramDef<List<UserWithProfile>> {

  interface _Inputs {
    @IfAbsent(FAIL)
    List<String> userIds();
  }

  interface _InternalFacets {
    @Dependency(onVajram = GetUserWithProfile.class, canFanout = true)
    UserWithProfile userProfiles();

    @Inject
    RequestLevelCacheInvalidator reqCacheInvalidator();

    @Inject
    @Named("GetUser.shouldInvalidate")
    boolean shouldInvalidate();
  }

  @Resolve(dep = userProfiles_n, depInputs = GetUserWithProfile_Req.userId_n)
  public static FanoutCommand<String> userIdsForFanout(List<String> userIds) {
    return executeFanoutWith(userIds);
  }

  @Output
  static List<UserWithProfile> collectProfiles(
      FanoutDepResponses<GetUserWithProfile_Req, UserWithProfile> userProfiles,
      Optional<RequestLevelCacheInvalidator> reqCacheInvalidator,
      Optional<Boolean> shouldInvalidate) {
    List<UserWithProfile> results = new ArrayList<>();
    userProfiles
        .requestResponsePairs()
        .forEach(pair -> pair.response().valueOpt().ifPresent(results::add));
    if (reqCacheInvalidator.isPresent() && shouldInvalidate.orElse(false)) {
      reqCacheInvalidator
          .get()
          .invalidateCacheKeys(GetUserProfile_Req.class, getUserProfileReq -> true);
    }
    return results;
  }
}
```

`GetUserWithProfile` (the dependency being fanned out to — also shows sequential dependency chaining, where
the second resolver depends on the *value* of the first dependency's output):

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

## 6. Recursive fan-out with a conditional skip

`ChainAdd` — a compute vajram that depends on itself, using `skipFanout` to hit a base case, and reading one
non-fanout dependency as `Errable` alongside a fanout dependency as `FanoutDepResponses` in the same output
method:

```java
@Resolve(dep = chainSum_n, depInputs = ChainAdd_Req.numbers_n)
public static FanoutCommand<List<Integer>> numbersForSubChainer(List<Integer> numbers) {
  if (numbers.size() < 3) {
    return skipFanout("Skipping chainer as count of numbers is less than 3. Will call adder instead");
  } else {
    return executeFanoutWith(
        ImmutableList.of(
            new ArrayList<>(numbers.subList(0, numbers.size() - 1)),
            new ArrayList<>(List.of(numbers.get(numbers.size() - 1)))));
  }
}

@Output
static Integer add(Errable<Integer> sum, FanoutDepResponses<ChainAdd_Req, Integer> chainSum) {
  return sum.valueOpt().orElse(0)
      + chainSum.requestResponsePairs().stream()
          .mapToInt(response -> response.response().valueOpt().orElse(0))
          .sum();
}
```

Because this vajram depends on itself, a test invoking it must bound the recursion via
`VajramExecutionConfig.builder().disabledDependentChains(computedSet).build()` — see `references/testing.md`
for the exact call. Without this, the executor will keep enumerating the (in principle unbounded) set of
possible dependent chains.

## 7. Trait — static/qualifier dispatch across multiple implementations

`MultiAdd` (the Trait) and `AddUsingTraits` (a consumer that disambiguates three dependencies on the same
Trait via a custom `@Qualifier`):

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

```java
@Vajram
public abstract class AddUsingTraits extends ComputeVajramDef<ThreeSums> {
  interface _InternalFacets {
    @Dependency(onVajram = MultiAdd.class)
    @AdditionMethod(SIMPLE)
    int sum1();

    @Dependency(onVajram = MultiAdd.class)
    @AdditionMethod(CHAIN)
    int sum2();

    @Dependency(onVajram = MultiAdd.class)
    @AdditionMethod(SPLIT)
    int sum3();
  }

  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(sum1_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers1_s).asResolver()),
        dep(sum2_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers2_s).asResolver()),
        dep(sum3_s, depInput(MultiAdd_Req.numbers_s).usingAsIs(numbers3_s).asResolver()));
  }

  @Output
  public static ThreeSums output(
      Optional<Integer> sum1, Optional<Integer> sum2, Optional<Integer> sum3) {
    return new ThreeSums(sum1.orElse(0), sum2.orElse(0), sum3.orElse(0));
  }

  public record ThreeSums(int sum1, int sum2, int sum3) {}
}
```

`SimpleAdd`, `ChainAdd`, and `SplitAdd` each `implements MultiAdd` — this is the DSL-resolver case flagged
in `vajram-anatomy.md`: three dependencies on the same Trait, each resolved identically apart from which
input feeds it, is exactly the repeated-shape case where `getSimpleInputResolvers()` is preferable to
writing three near-identical `@Resolve` methods.

## 8. Trait — predicate dispatch based on a runtime value

`GetPiece` — a generic Trait (`T extends Piece`) dispatched purely by the runtime value of an enum-typed
`@UseForPredicateDispatch` input, no qualifier annotation needed at any call site. Use this pattern instead
of static/qualifier dispatch when the choice of implementation is a genuine runtime decision (e.g. based on
user input) rather than a fixed choice per call site.

```java
public interface Piece {}

public enum PieceType { KNIGHT, ROOK }

public record Knight() implements Piece {}
public record Rook() implements Piece {}

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

`GetKnight` / `GetRook` — each restates the Trait's `_Inputs` shape and binds `T` to its own output type;
the `type` input isn't otherwise used inside the implementation, since dispatch already consumed it before
the implementation was ever selected:

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

The matching test — registering the predicate dispatch policy and executing directly against the Trait's
generated request type, parameterized by the concrete output type:

```java
kGraph.traitDispatchPolicies(
    new TraitDispatchPolicies(
        dispatchTrait(GetPiece_Req.class, graph)
            .conditionally(
                when(type_s, equalsEnum(KNIGHT)).to(GetKnight_Req.class),
                when(type_s, equalsEnum(ROOK)).to(GetRook_Req.class))));

CompletableFuture<Knight> result;
try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
  result = executor.execute(GetPiece_ReqImmutPojo.<Knight>_builder().type(KNIGHT)._build());
}
assertThat(result).succeedsWithin(TEST_TIMEOUT).isEqualTo(new Knight());
```

Note `GetPiece` has no `@InvocableOutsideGraph` yet its request is still executed directly in this sample —
unlike a plain Vajram, a Trait's own `@InvocableOutsideGraph` status isn't the only thing that matters here;
don't assume you need to add it purely to make a Trait request executable in a test. If you hit
`RejectedExecutionException` in your own case, add `@InvocableOutsideGraph` to the Trait interface (as
`MultiAdd` in example 7 does) rather than guessing further.

## 9. Trait — predicate dispatch with multiple inputs and a cascading fallback

`CustomerServiceAgent` — a Trait dispatched on *two* `@UseForPredicateDispatch` inputs at once (an enum and
a sealed-interface subtype), with seven conforming Vajrams
(`L1CallAgent`/`L1EmailAgent`/`L2CallAgent`/`L3EmailAgent`/`DefaultCallAgent`/`DefaultEmailAgent`/
`DefaultCustomerServiceAgent`) — a realistic shape for "handle this the specific way if we can, otherwise
fall back":

```java
@Trait
@CallGraphDelegationMode(NONE)
@InvocableOutsideGraph
public interface CustomerServiceAgent extends TraitDef<String> {
  interface _Inputs {
    @IfAbsent(FAIL)
    @UseForPredicateDispatch
    AgentType agentType();

    @IfAbsent(FAIL)
    @UseForPredicateDispatch
    InitialCommunication initialCommunication();

    @IfAbsent(FAIL)
    @UseForPredicateDispatch
    String customerName();
  }

  enum AgentType { L1, L2, L3 }

  sealed interface InitialCommunication {}
  record Email(String emailContent) implements InitialCommunication {}
  record Call(String callRecording) implements InitialCommunication {}
  record Ticket(String ticketSummary) implements InitialCommunication {}
}
```

The dispatch policy (from the matching test) combines two matcher kinds — `equalsEnum` for `AgentType`,
`isInstanceOf` for which `InitialCommunication` subtype was supplied — and chains them with `.and(...)` so a
case only matches when *both* conditions hold. Specific cases are listed before broader ones, since **the
first matching case wins**:

```java
kGraph.traitDispatchPolicies(
    new TraitDispatchPolicies(
        dispatchTrait(CustomerServiceAgent_Req.class, graph)
            .conditionally(
                when(agentType_s, equalsEnum(L1))
                    .and(initialCommunication_s, isInstanceOf(Call.class))
                    .to(L1CallAgent_Req.class),
                when(agentType_s, equalsEnum(L1))
                    .and(initialCommunication_s, isInstanceOf(Email.class))
                    .to(L1EmailAgent_Req.class),
                when(agentType_s, equalsEnum(L2))
                    .and(initialCommunication_s, isInstanceOf(Call.class))
                    .to(L2CallAgent_Req.class),
                when(agentType_s, equalsEnum(L3))
                    .and(initialCommunication_s, isInstanceOf(Email.class))
                    .to(L3EmailAgent_Req.class),
                // Falls back to comm-type-only agents when no (agentType, commType) case matches
                when(initialCommunication_s, isInstanceOf(Call.class)).to(DefaultCallAgent_Req.class),
                when(initialCommunication_s, isInstanceOf(Email.class)).to(DefaultEmailAgent_Req.class),
                // True catch-all — must be last
                when(agentType_s, isAnyValue())
                    .and(initialCommunication_s, isAnyValue())
                    .to(DefaultCustomerServiceAgent_Req.class))));
```

Reordering these cases (e.g. putting the catch-all first) would silently swallow every other case, since
evaluation stops at the first match — this is why the skill's testing guidance calls out case order as
load-bearing, not cosmetic.
