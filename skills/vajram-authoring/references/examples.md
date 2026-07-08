# Worked examples

All examples below are reproduced verbatim (or near-verbatim, trimmed of unrelated imports) from real,
compiling code under `vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/**`. Read
`vajram-anatomy.md` first for what each annotation means; this file is for pattern-matching your own case
against a real one.

## 1. Simplest possible compute vajram, with a default value

`calculator/add/SimpleAdd.java` — no dependencies, one input with `ASSUME_DEFAULT_VALUE`, implements a
Trait (`MultiAdd`, see example 6):

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

`calculator/add/Add.java` — batched inputs, `@Output.Batched` + `@Output.Unbatch`, an injected boolean used
to simulate failure, a static counter for test assertions:

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

Matching test (`AddTest.java`) — the canonical minimal executor pattern:

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

`greet/Greet.java` — a compute vajram with an injected `Logger` and a `@Named` sink, plus a dependency on
an IO vajram (`UserService`, example 4):

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

`greet/UserService.java`:

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

`user/GetUserProfilesFromUserIds.java` — a compute vajram fanning out to a dependency once per element of
a list, reading the fanned-out responses via `FanoutDepResponses`:

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

`GetUserWithProfile.java` (the dependency being fanned out to — also shows sequential dependency chaining,
where the second resolver depends on the *value* of the first dependency's output):

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

`calculator/add/ChainAdd.java` — a compute vajram that depends on itself, using `skipFanout` to hit a base
case, and reading one non-fanout dependency as `Errable` alongside a fanout dependency as
`FanoutDepResponses` in the same output method:

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
`VajramExecutionConfig.builder().disabledDependentChains(computedSet).build()` — see
`references/testing.md` and `ChainAddTest.java` for the exact call. Without this, the executor will keep
enumerating the (in principle unbounded) set of possible dependent chains.

## 7. Trait — static/qualifier dispatch across multiple implementations

`calculator/add/MultiAdd.java` (the Trait) and `calculator/add/AddUsingTraits.java` (a consumer that
disambiguates three dependencies on the same Trait via a custom `@Qualifier`):

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

`chess/GetPiece.java` (conceptual shape — a generic Trait dispatched by an enum-typed
`@UseForPredicateDispatch` input) with `GetKnight`/`GetRook` as concrete implementations selected purely by
the runtime value of that input, no explicit qualifier annotation needed at the call site. Use this pattern
instead of static/qualifier dispatch when the choice of implementation is a genuine runtime decision (e.g.
based on user input) rather than a fixed choice per call site.
