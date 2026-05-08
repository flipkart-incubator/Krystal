package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.google.inject.Guice.createInjector;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.ext.sql.vertx.ExecuteVertxSql;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderItemInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderUserIdEquals_ImmutPojo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderWithItems;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserIdEquals_ImmutPojo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNameAndOrders;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNameEquals_ImmutPojo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserWithOrdersAndItems;
import com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.guice.traitbinding.StaticDispatchPolicyImpl;
import com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that start an H2 in-memory database, seed it with data, then execute each SQL
 * trait through a full Krystal {@link KrystexVajramExecutor} and assert the results.
 */
class SqlTraitIntegrationTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final String PACKAGE_PATH =
      "com.flipkart.krystal.vajram.ext.sql.vertx.samples.users";

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static Vertx vertx;
  private static Pool pool;

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeAll
  static void beforeAll() throws Exception {
    EXEC_POOL = new SingleThreadExecutorsPool("SqlTest", 4);
    vertx = Vertx.vertx();
    pool =
        JDBCPool.pool(
            vertx,
            new JDBCConnectOptions()
                .setJdbcUrl(
                    "jdbc:h2:mem:testdb"
                        + ";DB_CLOSE_DELAY=-1"
                        + ";MODE=PostgreSQL"
                        + ";DATABASE_TO_UPPER=FALSE"
                        + ";CASE_INSENSITIVE_IDENTIFIERS=FALSE"),
            new PoolOptions().setMaxSize(4));

    runSql(
        "CREATE TABLE users ("
            + "id BIGINT PRIMARY KEY, "
            + "name VARCHAR(255) NOT NULL, "
            + "email VARCHAR(255), "
            + "phoneNumber VARCHAR(255))");
    runSql(
        "CREATE TABLE orders ("
            + "orderId BIGINT PRIMARY KEY, "
            + "userId BIGINT NOT NULL, "
            + "amountCents BIGINT NOT NULL, "
            + "orderTime BIGINT NOT NULL)");
    runSql(
        "CREATE TABLE orderItems ("
            + "orderItemId BIGINT PRIMARY KEY, "
            + "itemName VARCHAR(255) NOT NULL, "
            + "itemPriceCents BIGINT NOT NULL, "
            + "orderId BIGINT NOT NULL)");

    // Single user: Alice (id=1)
    runSql("INSERT INTO users VALUES (1, 'Alice', 'alice@example.com', '+1-555-0100')");
    // Two orders for Alice: orderId=10 (older), orderId=11 (newer)
    runSql("INSERT INTO orders VALUES (10, 1, 5000, 1000)");
    runSql("INSERT INTO orders VALUES (11, 1, 12000, 2000)");
    // Order 10 has 2 line items, order 11 has 1
    runSql("INSERT INTO orderItems VALUES (100, 'Widget A', 999, 10)");
    runSql("INSERT INTO orderItems VALUES (101, 'Widget B', 1999, 10)");
    runSql("INSERT INTO orderItems VALUES (102, 'Gadget', 4999, 11)");

    // User Bob (id=2) with one order (orderId=20) containing 7 items of varying prices.
    // The @LIMIT(5) @ORDER_BY(itemPriceCents DESC) on orderItems() must return only the 5 most
    // expensive items in descending price order.
    runSql("INSERT INTO users VALUES (2, 'Bob', 'bob@example.com', null)");
    runSql("INSERT INTO orders VALUES (20, 2, 99000, 3000)");
    runSql("INSERT INTO orderItems VALUES (200, 'Item-100', 100, 20)");
    runSql("INSERT INTO orderItems VALUES (201, 'Item-500', 500, 20)");
    runSql("INSERT INTO orderItems VALUES (202, 'Item-200', 200, 20)");
    runSql("INSERT INTO orderItems VALUES (203, 'Item-800', 800, 20)");
    runSql("INSERT INTO orderItems VALUES (204, 'Item-300', 300, 20)");
    runSql("INSERT INTO orderItems VALUES (205, 'Item-700', 700, 20)");
    runSql("INSERT INTO orderItems VALUES (206, 'Item-400', 400, 20)");
  }

  @AfterAll
  static void afterAll() throws Exception {
    pool.close().toCompletionStage().toCompletableFuture().get();
    vertx.close().toCompletionStage().toCompletableFuture().get();
  }

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    executorLease = EXEC_POOL.lease();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  // ─── GetUserInfoById ──────────────────────────────────────────────────────────

  @Test
  void getUserInfoById_returnsUserFromDatabase() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserInfoById")) {
      future =
          executor.execute(
              new GetUserInfoById_ReqImmutPojo(new UserIdEquals_ImmutPojo(1)),
              KryonExecutionConfig.builder().executionId("getUserInfoById_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(1L);
              assertThat(user.name()).isEqualTo("Alice");
              assertThat(user.contactEmail()).isEqualTo("alice@example.com");
              assertThat(user.phoneNumber()).contains("+1-555-0100");
            });
  }

  @Test
  void getUserInfoById_returnsNullForMissingUser() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserInfoById_missing")) {
      future =
          executor.execute(
              GetUserInfoById_ReqImmutPojo._builder()
                  .where(new UserIdEquals_ImmutPojo(999))
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserInfoById_missing_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isNull();
  }

  // ─── GetOrderInfoByUserId ─────────────────────────────────────────────────────

  @Test
  void getOrderInfoByUserId_returnsAllOrdersFromDatabase() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrderInfoByUserId")) {
      future =
          executor.execute(
              GetOrderInfoByUserId_ReqImmutPojo._builder()
                  .where(OrderUserIdEquals_ImmutPojo._builder().userId(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getOrderInfoByUserId_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders).hasSize(2);
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactlyInAnyOrder(10L, 11L);
              assertThat(orders.stream().mapToLong(OrderInfo::amountCents).sum()).isEqualTo(17000L);
            });
  }

  @Test
  void getOrderInfoByUserId_returnsEmptyListForUserWithNoOrders() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrderInfoByUserId_empty")) {
      future =
          executor.execute(
              GetOrderInfoByUserId_ReqImmutPojo._builder()
                  .where(OrderUserIdEquals_ImmutPojo._builder().userId(999L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrderInfoByUserId_empty_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).satisfies(orders -> assertThat(orders).isEmpty());
  }

  // ─── GetUserOrdersByUserName ──────────────────────────────────────────────────

  @Test
  void getUserOrdersByUserName_returnsUserWithOrdersFromDatabase() {
    CompletableFuture<UserNameAndOrders> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserOrdersByUserName")) {
      future =
          executor.execute(
              GetUserOrdersByUserName_ReqImmutPojo._builder()
                  .where(UserNameEquals_ImmutPojo._builder().name("Alice")._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserOrdersByUserName_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            result -> {
              assertThat(result).isNotNull();
              assertThat(result.name()).isEqualTo("Alice");
              assertThat(result.orders()).hasSize(2);
              assertThat(result.orders().stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactlyInAnyOrder(10L, 11L);
            });
  }

  @Test
  void getUserOrdersByUserName_returnsNullForMissingUser() {
    CompletableFuture<UserNameAndOrders> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserOrdersByUserName_missing")) {
      future =
          executor.execute(
              GetUserOrdersByUserName_ReqImmutPojo._builder()
                  .where(UserNameEquals_ImmutPojo._builder().name("NoSuchUser")._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getUserOrdersByUserName_missing_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isNull();
  }

  // ─── GetUserByIdWithOrdersAndItems ───────────────────────────────────────────

  @Test
  void getUserByIdWithOrdersAndItems_returnsTwoLevelNestedDataFromDatabase() {
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserByIdWithOrdersAndItems")) {
      future =
          executor.execute(
              GetUserByIdWithOrdersAndItems_ReqImmutPojo._builder()
                  .where(UserIdEquals_ImmutPojo._builder().id(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getUserByIdWithOrdersAndItems_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            result -> {
              assertThat(result).isNotNull();
              assertThat(result.name()).isEqualTo("Alice");
              assertThat(result.orders()).hasSize(2);
              assertThat(result.orders().stream().filter(o -> o.orderId() == 10L).findFirst())
                  .isPresent()
                  .get()
                  .satisfies(o -> assertThat(o.orderItems()).hasSize(2));
              assertThat(result.orders().stream().filter(o -> o.orderId() == 11L).findFirst())
                  .isPresent()
                  .get()
                  .satisfies(o -> assertThat(o.orderItems()).hasSize(1));
            });
  }

  // ─── GetUserByNameWithOrdersAndItems ─────────────────────────────────────────

  @Test
  void getUserByNameWithOrdersAndItems_returnsTwoLevelNestedDataFromDatabase() {
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserByNameWithOrdersAndItems")) {
      future =
          executor.execute(
              GetUserByNameWithOrdersAndItems_ReqImmutPojo._builder()
                  .where(UserNameEquals_ImmutPojo._builder().name("Alice")._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getUserByNameWithOrdersAndItems_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            result -> {
              assertThat(result).isNotNull();
              assertThat(result.name()).isEqualTo("Alice");
              // Alice has 2 orders, both within the @LIMIT(3) on orders
              assertThat(result.orders()).hasSize(2);
              // orderId=11 (orderTime=2000) comes first — @ORDER_BY(orderTime DESC)
              assertThat(result.orders().get(0).orderId()).isEqualTo(11L);
              assertThat(result.orders().get(0).orderItems()).hasSize(1);
              assertThat(result.orders().get(1).orderId()).isEqualTo(10L);
              assertThat(result.orders().get(1).orderItems()).hasSize(2);
            });
  }

  @Test
  void getUserByNameWithOrdersAndItems_respectsThreeLevelLimits() {
    // Bob has 1 order (orderId=20) with 7 items; @LIMIT(5) @ORDER_BY(itemPriceCents DESC)
    // must return only the 5 most expensive items in descending price order.
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor =
        createExecutor("getUserByNameWithOrdersAndItems_limits")) {
      future =
          executor.execute(
              GetUserByNameWithOrdersAndItems_ReqImmutPojo._builder()
                  .where(UserNameEquals_ImmutPojo._builder().name("Bob")._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getUserByNameWithOrdersAndItems_limits_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            result -> {
              assertThat(result).isNotNull();
              assertThat(result.name()).isEqualTo("Bob");
              assertThat(result.orders()).hasSize(1);
              List<OrderItemInfo> items = result.orders().get(0).orderItems();
              // @LIMIT(5) caps at 5 even though 7 items exist
              assertThat(items).hasSize(5);
              // @ORDER_BY(itemPriceCents DESC) — prices descending: 800, 700, 500, 400, 300
              assertThat(items.stream().mapToLong(OrderItemInfo::itemPriceCents).boxed())
                  .containsExactly(800L, 700L, 500L, 400L, 300L);
            });
  }

  @Test
  void getUserByNameWithOrdersAndItems_returnsNullForMissingUser() {
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor =
        createExecutor("getUserByNameWithOrdersAndItems_missing")) {
      future =
          executor.execute(
              GetUserByNameWithOrdersAndItems_ReqImmutPojo._builder()
                  .where(UserNameEquals_ImmutPojo._builder().name("NoSuchUser")._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getUserByNameWithOrdersAndItems_missing_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isNull();
  }

  // ─── GetOrdersWithItemsByUserId ───────────────────────────────────────────────

  @Test
  void getOrdersWithItemsByUserId_returnsOrdersWithItemsOrderedByTimeDesc() {
    CompletableFuture<List<OrderWithItems>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersWithItemsByUserId")) {
      future =
          executor.execute(
              GetOrdersWithItemsByUserId_ReqImmutPojo._builder()
                  .where(OrderUserIdEquals_ImmutPojo._builder().userId(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersWithItemsByUserId_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders).hasSize(2);
              // orderTime=2000 (orderId=11) must come before orderTime=1000 (orderId=10)
              assertThat(orders.get(0).orderId()).isEqualTo(11L);
              assertThat(orders.get(0).orderItems()).hasSize(1);
              assertThat(orders.get(1).orderId()).isEqualTo(10L);
              assertThat(orders.get(1).orderItems()).hasSize(2);
            });
  }

  @Test
  void getOrdersWithItemsByUserId_respectsJoinLimitAndOrderBy() {
    // Bob (id=2) has order 20 with 7 items. @LIMIT(5) @ORDER_BY(itemPriceCents DESC) must return
    // only the 5 most expensive items, in descending price order.
    CompletableFuture<List<OrderWithItems>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersWithItemsByUserId_joinLimit")) {
      future =
          executor.execute(
              GetOrdersWithItemsByUserId_ReqImmutPojo._builder()
                  .where(OrderUserIdEquals_ImmutPojo._builder().userId(2L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersWithItemsByUserId_joinLimit_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders).hasSize(1);
              List<OrderItemInfo> items = orders.get(0).orderItems();
              // LIMIT(5) on orderItems must cap at 5 even though 7 were inserted
              assertThat(items).hasSize(5);
              // ORDER_BY(itemPriceCents DESC) — prices must be descending: 800, 700, 500, 400, 300
              assertThat(items.stream().mapToLong(OrderItemInfo::itemPriceCents).boxed())
                  .containsExactly(800L, 700L, 500L, 400L, 300L);
            });
  }

  @Test
  void getOrdersWithItemsByUserId_returnsEmptyListForUserWithNoOrders() {
    CompletableFuture<List<OrderWithItems>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersWithItemsByUserId_empty")) {
      future =
          executor.execute(
              GetOrdersWithItemsByUserId_ReqImmutPojo._builder()
                  .where(OrderUserIdEquals_ImmutPojo._builder().userId(999L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersWithItemsByUserId_empty_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).satisfies(orders -> assertThat(orders).isEmpty());
  }

  // ─── GetRecentOrdersByUserId ──────────────────────────────────────────────────

  @Test
  void getRecentOrdersByUserId_returnsOrdersOrderedByTimeDescAndRespectLimit() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getRecentOrdersByUserId")) {
      future =
          executor.execute(
              GetRecentOrdersByUserId_ReqImmutPojo._builder()
                  .where(OrderUserIdEquals_ImmutPojo._builder().userId(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getRecentOrdersByUserId_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders).hasSize(2);
              // orderTime=2000 (orderId=11) must come before orderTime=1000 (orderId=10)
              assertThat(orders.get(0).orderId()).isEqualTo(11L);
              assertThat(orders.get(1).orderId()).isEqualTo(10L);
            });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private KrystexVajramExecutor createExecutor(String executorId) {
    VajramGraph vajramGraph =
        VajramGraph.builder()
            .loadClasses(ExecuteVertxSql.class)
            .loadFromPackage(PACKAGE_PATH)
            .build();
    KrystexGraph.KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(vajramGraph);
    kGraph.injectionProvider(new VajramGuiceInputInjector(createInjector(new GuiceModule())));
    TraitBinder traitBinder = new TraitBinder();
    traitBinder.bindTrait(GetUserInfoById_Req.class).to(GetUserInfoById_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetOrderInfoByUserId_Req.class)
        .to(GetOrderInfoByUserId_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetUserOrdersByUserName_Req.class)
        .to(GetUserOrdersByUserName_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetUserByIdWithOrdersAndItems_Req.class)
        .to(GetUserByIdWithOrdersAndItems_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetUserByNameWithOrdersAndItems_Req.class)
        .to(GetUserByNameWithOrdersAndItems_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetRecentOrdersByUserId_Req.class)
        .to(GetRecentOrdersByUserId_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetOrdersWithItemsByUserId_Req.class)
        .to(GetOrdersWithItemsByUserId_VertxSql_Req.class);

    kGraph.traitDispatchPolicies(
        TraitDispatchPolicies.builder()
            .addTraitDispatchPolicies(
                Stream.of(
                        GetUserInfoById_Req._VAJRAM_ID,
                        GetOrderInfoByUserId_Req._VAJRAM_ID,
                        GetUserOrdersByUserName_Req._VAJRAM_ID,
                        GetUserByIdWithOrdersAndItems_Req._VAJRAM_ID,
                        GetUserByNameWithOrdersAndItems_Req._VAJRAM_ID,
                        GetRecentOrdersByUserId_Req._VAJRAM_ID,
                        GetOrdersWithItemsByUserId_Req._VAJRAM_ID)
                    .map(
                        vajramID ->
                            new StaticDispatchPolicyImpl(vajramGraph, vajramID, traitBinder))
                    .toList())
            .build());
    return kGraph
        .build()
        .createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder()
                        .executorId(executorId)
                        .executorService(executorLease.get())
                        .build()));
  }

  private static void runSql(String sql) throws Exception {
    pool.preparedQuery(sql).execute().toCompletionStage().toCompletableFuture().get();
  }

  private static class GuiceModule extends AbstractModule {
    @Provides
    @Singleton
    @Named("vertxSql_pool")
    public Pool providePool() {
      return pool;
    }
  }
}
