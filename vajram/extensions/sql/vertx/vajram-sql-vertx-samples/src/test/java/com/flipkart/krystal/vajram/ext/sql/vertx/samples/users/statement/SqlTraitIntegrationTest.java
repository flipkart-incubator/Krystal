package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.google.inject.Guice.createInjector;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.ext.sql.vertx.ExecuteVertxSql;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderAmountGtPredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderAmountLtePredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderItemInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderTimeIsInRange;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderTimeRangePredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderUserIdEquals;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderWithItems;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserIdPredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNameAndOrders;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNamePredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserOrPredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserWithAddress;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserWithOrdersAndItems;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Address;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Address_ImmutJson;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order_ImmutPojo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.UserInsertResult;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User_ImmutPojo;
import com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.guice.traitbinding.GuiceyStaticDispatchPolicy;
import com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.common.collect.Range;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests that start a PostgreSQL Testcontainer, seed it with data, then execute each SQL
 * trait through a full Krystal {@link KrystexVajramExecutor} and assert the results.
 */
@Testcontainers
class SqlTraitIntegrationTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final String PACKAGE_PATH =
      "com.flipkart.krystal.vajram.ext.sql.vertx.samples.users";

  @Container
  private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static Vertx vertx;
  private static Pool pool;

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeAll
  static void beforeAll() throws Exception {
    EXEC_POOL = new SingleThreadExecutorsPool("SqlTest", 4);
    vertx = Vertx.vertx();
    pool =
        PgBuilder.pool()
            .with(new PoolOptions().setMaxSize(4))
            .connectingTo(
                new PgConnectOptions()
                    .setHost(PG.getHost())
                    .setPort(PG.getMappedPort(5432))
                    .setDatabase(PG.getDatabaseName())
                    .setUser(PG.getUsername())
                    .setPassword(PG.getPassword()))
            .using(vertx)
            .build();

    runSql(
        "CREATE TABLE UserEntity ("
            + "internalId SERIAL UNIQUE, "
            + "id BIGINT PRIMARY KEY, "
            + "name VARCHAR(255) NOT NULL, "
            + "email VARCHAR(255), "
            + "phoneNumber VARCHAR(255), "
            + "address TEXT, "
            + "secondaryAddresses TEXT)");
    runSql(
        "CREATE TABLE OrderEntity ("
            + "orderId BIGINT PRIMARY KEY, "
            + "userId BIGINT NOT NULL, "
            + "amountCents BIGINT NOT NULL, "
            + "orderTime BIGINT NOT NULL)");
    runSql(
        "CREATE TABLE OrderItem ("
            + "orderItemId BIGINT PRIMARY KEY, "
            + "itemName VARCHAR(255) NOT NULL, "
            + "itemPriceCents BIGINT NOT NULL, "
            + "orderId BIGINT NOT NULL)");

    // Seed user and order data via INSERT vajrams; order items remain as raw SQL (no vajram).
    Lease<SingleThreadExecutor> seedLease = EXEC_POOL.lease();
    try {
      SingleThreadExecutor seedExec = seedLease.get();

      // Alisha (id=1): 6 orders so that @LIMIT(3) on orders AND @LIMIT(5) on the recent-orders
      // list are both exceeded, proving each cap is enforced independently at every nesting level.
      // Each order carries 6 items (prices 100–600) so that @LIMIT(5) on orderItems is also
      // exercised.
      runInsertUser(
          User_ImmutPojo._builder()
              .id(1L)
              .name("Alisha")
              .email("Alisha@example.com")
              .phoneNumber("+1-555-0100")
              .address(Address_ImmutJson._builder().city("NYC").zip("10001")._build())
              .secondaryAddresses(List.of())
              .orders(List.of())
              ._build(),
          seedExec);
      for (int i = 0; i < 6; i++) {
        int orderId = 10 + i;
        runInsertOrder(
            Order_ImmutPojo._builder()
                .orderId((long) orderId)
                .userId(1L)
                .amountCents(5000L)
                .orderTime((long) ((i + 1) * 1000))
                .orderItems(List.of())
                ._build(),
            seedExec);
        for (int j = 0; j < 6; j++) {
          int itemId = orderId * 100 + j;
          int price = (j + 1) * 100;
          runSql(
              "INSERT INTO OrderItem VALUES ("
                  + itemId
                  + ", 'Item-"
                  + price
                  + "', "
                  + price
                  + ", "
                  + orderId
                  + ")");
        }
      }

      // Babu (id=2): 11 orders so that @LIMIT(10) on the orders list trait is exceeded.
      // Each order also carries 6 items so that @LIMIT(5) on orderItems is exercised per order.
      runInsertUser(
          User_ImmutPojo._builder()
              .id(2L)
              .name("Babu")
              .email("Babu@example.com")
              .address(Address_ImmutJson._builder().city("SFO").zip("94105")._build())
              .secondaryAddresses(List.of())
              .orders(List.of())
              ._build(),
          seedExec);
      for (int i = 0; i < 11; i++) {
        int orderId = 20 + i;
        runInsertOrder(
            Order_ImmutPojo._builder()
                .orderId((long) orderId)
                .userId(2L)
                .amountCents(10000L)
                .orderTime((long) ((i + 1) * 1000))
                .orderItems(List.of())
                ._build(),
            seedExec);
        for (int j = 0; j < 6; j++) {
          int itemId = orderId * 100 + j;
          int price = (j + 1) * 100;
          runSql(
              "INSERT INTO OrderItem VALUES ("
                  + itemId
                  + ", 'Item-"
                  + price
                  + "', "
                  + price
                  + ", "
                  + orderId
                  + ")");
        }
      }
    } finally {
      seedLease.close();
    }
  }

  @AfterAll
  static void afterAll() {
    pool.close().toCompletionStage().toCompletableFuture().join();
    vertx.close().toCompletionStage().toCompletableFuture().join();
  }

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    executorLease = EXEC_POOL.lease();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  // ─── GetUserWithAddressById (SELECT with @SerdeWith deserialization) ─────────

  @Test
  void getUserWithAddressById_returnsDeserializedAddress() {
    CompletableFuture<UserWithAddress> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserWithAddressById")) {
      future =
          executor.execute(
              GetUserWithAddressById_Req._builder()
                  .where(UserIdPredicate._builder().idIs(1L))
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserWithAddressById_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(1L);
              assertThat(user.name()).isEqualTo("Alisha");
              // Verify JSON deserialization of address column
              Address addr = user.address();
              assertThat(addr).isNotNull();
              assertThat(addr.city()).isEqualTo("NYC");
              assertThat(addr.zip()).isEqualTo("10001");
            });
  }

  @Test
  void getUserWithAddressById_returnsDeserializedSecondaryAddresses() throws Exception {
    // Insert a user with non-null secondaryAddresses via InsertUser vajram
    CompletableFuture<Integer> insertFuture;
    try (KrystexVajramExecutor insertExec = createExecutor("insertTestSerdeUser")) {
      insertFuture =
          insertExec.execute(
              InsertUser_Req._builder()
                  .user(
                      User_ImmutPojo._builder()
                          .id(100L)
                          .name("TestSerde")
                          .email("serde@example.com")
                          .address(Address_ImmutJson._builder().city("LA").zip("90001")._build())
                          .secondaryAddresses(
                              List.of(
                                  Address_ImmutJson._builder()
                                      .city("Chicago")
                                      .zip("60601")
                                      ._build(),
                                  Address_ImmutJson._builder()
                                      .city("Boston")
                                      .zip("02101")
                                      ._build()))
                          .orders(List.of())
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("insertTestSerdeUser_exec").build());
    }
    assertThat(insertFuture).succeedsWithin(TIMEOUT).isEqualTo(1);
    try {
      CompletableFuture<UserWithAddress> future;
      try (KrystexVajramExecutor executor = createExecutor("getUserWithAddressByIdSecondary")) {
        future =
            executor.execute(
                GetUserWithAddressById_Req._builder()
                    .where(UserIdPredicate._builder().idIs(100L))
                    ._build(),
                KryonExecutionConfig.builder()
                    .executionId("getUserWithAddressByIdSecondary_exec")
                    .build());
      }
      assertThat(future)
          .succeedsWithin(TIMEOUT)
          .satisfies(
              user -> {
                assertThat(user).isNotNull();
                assertThat(user.id()).isEqualTo(100L);
                assertThat(user.name()).isEqualTo("TestSerde");
                // Verify primary address
                assertThat(user.address().city()).isEqualTo("LA");
                assertThat(user.address().zip()).isEqualTo("90001");
                // Verify List<Address> deserialization
                assertThat(user.secondaryAddresses()).hasSize(2);
                assertThat(user.secondaryAddresses().get(0).city()).isEqualTo("Chicago");
                assertThat(user.secondaryAddresses().get(0).zip()).isEqualTo("60601");
                assertThat(user.secondaryAddresses().get(1).city()).isEqualTo("Boston");
                assertThat(user.secondaryAddresses().get(1).zip()).isEqualTo("02101");
              });
    } finally {
      runSql("DELETE FROM UserEntity WHERE id = 100");
    }
  }

  // ─── GetUserInfoById ──────────────────────────────────────────────────────────

  @Test
  void getUserInfoById_returnsUserFromDatabase() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserInfoById")) {
      future =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(1L))._build(),
              KryonExecutionConfig.builder().executionId("getUserInfoById_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(1L);
              assertThat(user.name()).isEqualTo("Alisha");
              assertThat(user.contactEmail()).isEqualTo("Alisha@example.com");
              assertThat(user.phoneNumber()).contains("+1-555-0100");
            });
  }

  @Test
  void getUserInfoById_returnsNullForMissingUser() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserInfoById_missing")) {
      future =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(999L))._build(),
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
              GetOrderInfoByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getOrderInfoByUserId_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // @LIMIT(NO_LIMIT) — all 6 of Alisha's orders are returned
              assertThat(orders).hasSize(6);
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactlyInAnyOrder(10L, 11L, 12L, 13L, 14L, 15L);
              assertThat(orders.stream().mapToLong(OrderInfo::amountCents).sum()).isEqualTo(30000L);
            });
  }

  @Test
  void getOrderInfoByUserId_returnsEmptyListForUserWithNoOrders() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrderInfoByUserId_empty")) {
      future =
          executor.execute(
              GetOrderInfoByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(999L)._build())
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
              GetUserOrdersByUserName_Req._builder()
                  .where(UserNamePredicate._builder().nameIs("Alisha")._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserOrdersByUserName_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            result -> {
              assertThat(result).isNotNull();
              assertThat(result.name()).isEqualTo("Alisha");
              // @LIMIT(10) not exceeded — all 6 orders returned, newest first
              assertThat(result.orders()).hasSize(6);
              assertThat(result.orders().stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(15L, 14L, 13L, 12L, 11L, 10L);
            });
  }

  @Test
  void getUserOrdersByUserName_returnsNullForMissingUser() {
    CompletableFuture<UserNameAndOrders> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserOrdersByUserName_missing")) {
      future =
          executor.execute(
              GetUserOrdersByUserName_Req._builder()
                  .where(UserNamePredicate._builder().nameIs("NoSuchUser")._build())
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
              GetUserByIdWithOrdersAndItems_Req._builder()
                  .where(UserIdPredicate._builder().idIs(1L)._build())
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
              assertThat(result.name()).isEqualTo("Alisha");
              // Alisha has 6 orders; @LIMIT(3) must cap at exactly 3
              assertThat(result.orders()).hasSize(3);
              // @ORDER(by = "orderTime", direction = DESC) — newest 3: orderId 15, 14, 13
              assertThat(result.orders().stream().mapToLong(OrderWithItems::orderId).boxed())
                  .containsExactly(15L, 14L, 13L);
              // Each order has 6 items; @LIMIT(5) must cap at exactly 5 per order
              for (OrderWithItems order : result.orders()) {
                assertThat(order.orderItems()).hasSize(5);
                // @ORDER(by = "itemPriceCents", direction = DESC) — top 5: 600, 500, 400, 300, 200
                assertThat(
                        order.orderItems().stream()
                            .mapToLong(OrderItemInfo::itemPriceCents)
                            .boxed())
                    .containsExactly(600L, 500L, 400L, 300L, 200L);
              }
            });
  }

  // ─── GetUserByNameWithOrdersAndItems ─────────────────────────────────────────

  @Test
  void getUserByNameWithOrdersAndItems_returnsTwoLevelNestedDataFromDatabase() {
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserByNameWithOrdersAndItems")) {
      future =
          executor.execute(
              GetUserByNameWithOrdersAndItems_Req._builder()
                  .where(UserNamePredicate._builder().nameIs("Alisha")._build())
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
              assertThat(result.name()).isEqualTo("Alisha");
              // Alisha has 6 orders; @LIMIT(3) must cap at exactly 3, newest first
              assertThat(result.orders()).hasSize(3);
              assertThat(result.orders().get(0).orderId()).isEqualTo(15L);
              assertThat(result.orders().get(1).orderId()).isEqualTo(14L);
              assertThat(result.orders().get(2).orderId()).isEqualTo(13L);
              // Each order has 6 items; @LIMIT(5) caps at 5 per order, highest price first
              for (OrderWithItems order : result.orders()) {
                assertThat(order.orderItems()).hasSize(5);
                assertThat(
                        order.orderItems().stream()
                            .mapToLong(OrderItemInfo::itemPriceCents)
                            .boxed())
                    .containsExactly(600L, 500L, 400L, 300L, 200L);
              }
            });
  }

  @Test
  void getUserByNameWithOrdersAndItems_respectsOrderAndItemLimitsIndependently() {
    // Babu has 11 orders each with 6 items.
    // @LIMIT(3) on orders caps at 3 (not 11); @LIMIT(5) on items caps at 5 per order (not 6).
    // Both limits are verified to be independent of each other.
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor =
        createExecutor("getUserByNameWithOrdersAndItems_limits")) {
      future =
          executor.execute(
              GetUserByNameWithOrdersAndItems_Req._builder()
                  .where(UserNamePredicate._builder().nameIs("Babu")._build())
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
              assertThat(result.name()).isEqualTo("Babu");
              // Babu has 11 orders; @LIMIT(3) must cap at exactly 3
              assertThat(result.orders()).hasSize(3);
              // @ORDER(by = "orderTime", direction = DESC) — newest 3: orderId 30, 29, 28
              assertThat(result.orders().stream().mapToLong(OrderWithItems::orderId).boxed())
                  .containsExactly(30L, 29L, 28L);
              // Each order has 6 items; @LIMIT(5) must cap at exactly 5 per order
              for (OrderWithItems order : result.orders()) {
                assertThat(order.orderItems()).hasSize(5);
                // @ORDER(by = "itemPriceCents", direction = DESC) — top 5: 600, 500, 400, 300, 200
                assertThat(
                        order.orderItems().stream()
                            .mapToLong(OrderItemInfo::itemPriceCents)
                            .boxed())
                    .containsExactly(600L, 500L, 400L, 300L, 200L);
              }
            });
  }

  @Test
  void getUserByNameWithOrdersAndItems_returnsNullForMissingUser() {
    CompletableFuture<UserWithOrdersAndItems> future;
    try (KrystexVajramExecutor executor =
        createExecutor("getUserByNameWithOrdersAndItems_missing")) {
      future =
          executor.execute(
              GetUserByNameWithOrdersAndItems_Req._builder()
                  .where(UserNamePredicate._builder().nameIs("NoSuchUser")._build())
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
              GetOrdersWithItemsByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersWithItemsByUserId_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // @LIMIT(10) not exceeded — all 6 of Alisha's orders returned, newest first
              assertThat(orders).hasSize(6);
              assertThat(orders.stream().mapToLong(OrderWithItems::orderId).boxed())
                  .containsExactly(15L, 14L, 13L, 12L, 11L, 10L);
              // Each order has 6 items; @LIMIT(5) caps at 5 per order, highest price first
              for (OrderWithItems order : orders) {
                assertThat(order.orderItems()).hasSize(5);
                assertThat(
                        order.orderItems().stream()
                            .mapToLong(OrderItemInfo::itemPriceCents)
                            .boxed())
                    .containsExactly(600L, 500L, 400L, 300L, 200L);
              }
            });
  }

  @Test
  void getOrdersWithItemsByUserId_respectsOrderAndItemLimitsIndependently() {
    // Babu (id=2) has 11 orders each with 6 items.
    // @LIMIT(10) on the list trait caps orders at 10 (not 11); @LIMIT(5) on orderItems caps items
    // at 5 per order (not 6). Both limits are verified independently.
    CompletableFuture<List<OrderWithItems>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersWithItemsByUserId_joinLimit")) {
      future =
          executor.execute(
              GetOrdersWithItemsByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(2L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersWithItemsByUserId_joinLimit_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Babu has 11 orders; @LIMIT(10) must cap at exactly 10
              assertThat(orders).hasSize(10);
              // @ORDER(by = "orderTime", direction = DESC) — newest 10: orderId 30..21
              assertThat(orders.stream().mapToLong(OrderWithItems::orderId).boxed())
                  .containsExactly(30L, 29L, 28L, 27L, 26L, 25L, 24L, 23L, 22L, 21L);
              // Each order has 6 items; @LIMIT(5) caps at 5 per order, highest price first
              for (OrderWithItems order : orders) {
                assertThat(order.orderItems()).hasSize(5);
                assertThat(
                        order.orderItems().stream()
                            .mapToLong(OrderItemInfo::itemPriceCents)
                            .boxed())
                    .containsExactly(600L, 500L, 400L, 300L, 200L);
              }
            });
  }

  @Test
  void getOrdersWithItemsByUserId_returnsEmptyListForUserWithNoOrders() {
    CompletableFuture<List<OrderWithItems>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersWithItemsByUserId_empty")) {
      future =
          executor.execute(
              GetOrdersWithItemsByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(999L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersWithItemsByUserId_empty_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).satisfies(orders -> assertThat(orders).isEmpty());
  }

  // ─── GetUserByIdOrName ────────────────────────────────────────────────────────

  @Test
  void getUserByIdOrName_matchesById() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserByIdOrName_byId")) {
      future =
          executor.execute(
              GetUserByIdOrName_Req._builder()
                  .where(
                      UserOrPredicate._builder()
                          .orWithUserId(UserIdPredicate._builder().idIs(1L)._build())
                          .orWithUserName(
                              UserNamePredicate._builder().nameIs("NoSuchUser")._build())
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserByIdOrName_byId_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(1L);
              assertThat(user.name()).isEqualTo("Alisha");
            });
  }

  @Test
  void getUserByIdOrName_matchesByName() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserByIdOrName_byName")) {
      future =
          executor.execute(
              GetUserByIdOrName_Req._builder()
                  .where(
                      UserOrPredicate._builder()
                          .orWithUserId(UserIdPredicate._builder().idIs(999L))
                          .orWithUserName(UserNamePredicate._builder().nameIs("Babu"))
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserByIdOrName_byName_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(2L);
              assertThat(user.name()).isEqualTo("Babu");
            });
  }

  @Test
  void getUserByIdOrName_returnsNullWhenNeitherMatches() {
    CompletableFuture<UserInfo> future;
    try (KrystexVajramExecutor executor = createExecutor("getUserByIdOrName_noMatch")) {
      future =
          executor.execute(
              GetUserByIdOrName_Req._builder()
                  .where(
                      UserOrPredicate._builder()
                          .orWithUserId(UserIdPredicate._builder().idIs(999L)._build())
                          .orWithUserName(
                              UserNamePredicate._builder().nameIs("NoSuchUser")._build())
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getUserByIdOrName_noMatch_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isNull();
  }

  // ─── GetOrdersByTimeRange (@IsGreaterThanOrEqual + @IsLessThan) ─────────────────

  @Test
  void getOrdersByTimeRange_returnsOrdersInHalfOpenRange() {
    // Alisha's orders: orderTime 1000,2000,3000,4000,5000,6000
    // Babu's orders:   orderTime 1000,2000,...,11000
    // Range [3000, 6000) should match orderTime 3000,4000,5000 for both users
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeRange")) {
      future =
          executor.execute(
              GetOrdersByTimeRange_Req._builder()
                  .where(
                      OrderTimeRangePredicate._builder()
                          .orderTimeFrom(3000L)
                          .orderTimeTo(6000L)
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getOrdersByTimeRange_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Alisha: orderId 12(3000), 13(4000), 14(5000) + Babu: orderId 22(3000), 23(4000),
              // 24(5000)
              assertThat(orders).hasSize(6);
              // @ORDER(by = "orderTime", direction = ASC)
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(12L, 22L, 13L, 23L, 14L, 24L);
            });
  }

  @Test
  void getOrdersByTimeRange_returnsEmptyWhenNoOrdersInRange() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeRange_empty")) {
      future =
          executor.execute(
              GetOrdersByTimeRange_Req._builder()
                  .where(
                      OrderTimeRangePredicate._builder()
                          .orderTimeFrom(99000L)
                          .orderTimeTo(100000L)
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByTimeRange_empty_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).satisfies(orders -> assertThat(orders).isEmpty());
  }

  // ─── GetOrdersByMinAmount (@IsGreaterThan) ────────────────────────────────────

  @Test
  void getOrdersByMinAmount_returnsOrdersAboveThreshold() {
    // Alisha's orders: amountCents=5000, Babu's orders: amountCents=10000
    // amountCents > 5000 should return only Babu's 11 orders
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByMinAmount")) {
      future =
          executor.execute(
              GetOrdersByMinAmount_Req._builder()
                  .where(OrderAmountGtPredicate._builder().amountGreaterThan(5000L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getOrdersByMinAmount_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders).hasSize(11);
              // All should be Babu's orders (amountCents=10000)
              assertThat(orders).allSatisfy(o -> assertThat(o.amountCents()).isEqualTo(10000L));
            });
  }

  @Test
  void getOrdersByMinAmount_returnsEmptyWhenNoneAboveThreshold() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByMinAmount_empty")) {
      future =
          executor.execute(
              GetOrdersByMinAmount_Req._builder()
                  .where(OrderAmountGtPredicate._builder().amountGreaterThan(99999L)._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByMinAmount_empty_exec")
                  .build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).satisfies(orders -> assertThat(orders).isEmpty());
  }

  // ─── GetOrdersByMaxAmount (@IsLessThanOrEqual) ──────────────────────────────

  @Test
  void getOrdersByMaxAmount_returnsOrdersAtOrBelowThreshold() {
    // Alisha's orders: amountCents=5000, Babu's orders: amountCents=10000
    // amountCents <= 5000 should return only Alisha's 6 orders
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByMaxAmount")) {
      future =
          executor.execute(
              GetOrdersByMaxAmount_Req._builder()
                  .where(OrderAmountLtePredicate._builder().amountAtMost(5000L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getOrdersByMaxAmount_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders).hasSize(6);
              // All should be Alisha's orders (amountCents=5000)
              assertThat(orders).allSatisfy(o -> assertThat(o.amountCents()).isEqualTo(5000L));
            });
  }

  @Test
  void getOrdersByMaxAmount_returnsAllWhenThresholdIsHigh() {
    // amountCents <= 99999 should return all 17 orders (6 Alisha + 11 Babu)
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByMaxAmount_all")) {
      future =
          executor.execute(
              GetOrdersByMaxAmount_Req._builder()
                  .where(OrderAmountLtePredicate._builder().amountAtMost(99999L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getOrdersByMaxAmount_all_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).satisfies(orders -> assertThat(orders).hasSize(17));
  }

  // ─── GetOrdersByTimeInRange (@IsInRange — closed range) ────────────────────────

  @Test
  void getOrdersByTimeInRange_closedRange_returnsOrdersIncludingBothEndpoints() {
    // Alisha's orders: orderTime 1000,2000,3000,4000,5000,6000
    // Babu's orders:   orderTime 1000,2000,...,11000
    // Range.closed(3000, 5000) should match orderTime 3000, 4000, 5000 for both users
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeInRange_closed")) {
      future =
          executor.execute(
              GetOrdersByTimeInRange_Req._builder()
                  .where(
                      OrderTimeIsInRange._builder()
                          .orderTimeRange(Range.closed(3000L, 5000L))
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByTimeInRange_closed_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Alisha: orderId 12(3000), 13(4000), 14(5000)
              // Babu:   orderId 22(3000), 23(4000), 24(5000)
              assertThat(orders).hasSize(6);
              // @ORDER(by = "orderTime", direction = ASC)
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(12L, 22L, 13L, 23L, 14L, 24L);
            });
  }

  // ─── GetOrdersByTimeInRange (@IsInRange — open range) ──────────────────────────

  @Test
  void getOrdersByTimeInRange_openRange_excludesBothEndpoints() {
    // Range.open(3000, 6000) should match orderTime 4000, 5000 (excludes 3000 and 6000)
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeInRange_open")) {
      future =
          executor.execute(
              GetOrdersByTimeInRange_Req._builder()
                  .where(
                      OrderTimeIsInRange._builder()
                          .orderTimeRange(Range.open(3000L, 6000L))
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByTimeInRange_open_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Alisha: orderId 13(4000), 14(5000)
              // Babu:   orderId 23(4000), 24(5000)
              assertThat(orders).hasSize(4);
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(13L, 23L, 14L, 24L);
            });
  }

  // ─── GetOrdersByTimeInRange (@IsInRange — closedOpen range) ────────────────────

  @Test
  void getOrdersByTimeInRange_closedOpenRange_includesLowerExcludesUpper() {
    // Range.closedOpen(3000, 6000) should match orderTime 3000, 4000, 5000 (excludes 6000)
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeInRange_closedOpen")) {
      future =
          executor.execute(
              GetOrdersByTimeInRange_Req._builder()
                  .where(
                      OrderTimeIsInRange._builder()
                          .orderTimeRange(Range.closedOpen(3000L, 6000L))
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByTimeInRange_closedOpen_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Same as the existing half-open range test: 3000, 4000, 5000 for both users
              assertThat(orders).hasSize(6);
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(12L, 22L, 13L, 23L, 14L, 24L);
            });
  }

  // ─── GetOrdersByTimeInRange (@IsInRange — openClosed range) ────────────────────

  @Test
  void getOrdersByTimeInRange_openClosedRange_excludesLowerIncludesUpper() {
    // Range.openClosed(3000, 6000) should match orderTime 4000, 5000, 6000 (excludes 3000)
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeInRange_openClosed")) {
      future =
          executor.execute(
              GetOrdersByTimeInRange_Req._builder()
                  .where(
                      OrderTimeIsInRange._builder()
                          .orderTimeRange(Range.openClosed(3000L, 6000L))
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByTimeInRange_openClosed_exec")
                  .build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Alisha: orderId 13(4000), 14(5000), 15(6000)
              // Babu:   orderId 23(4000), 24(5000), 25(6000)
              assertThat(orders).hasSize(6);
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(13L, 23L, 14L, 24L, 15L, 25L);
            });
  }

  // ─── GetOrdersByTimeInRange (@IsInRange — empty result) ────────────────────────

  @Test
  void getOrdersByTimeInRange_returnsEmptyWhenNoOrdersInRange() {
    CompletableFuture<List<OrderInfo>> future;
    try (KrystexVajramExecutor executor = createExecutor("getOrdersByTimeInRange_empty")) {
      future =
          executor.execute(
              GetOrdersByTimeInRange_Req._builder()
                  .where(
                      OrderTimeIsInRange._builder()
                          .orderTimeRange(Range.closed(99000L, 100000L))
                          ._build())
                  ._build(),
              KryonExecutionConfig.builder()
                  .executionId("getOrdersByTimeInRange_empty_exec")
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
              GetRecentOrdersByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("getRecentOrdersByUserId_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              // Alisha has 6 orders; @LIMIT(5) must cap at exactly 5
              assertThat(orders).hasSize(5);
              // @ORDER(by = "orderTime", direction = DESC) — newest 5: orderId 15..11
              assertThat(orders.stream().mapToLong(OrderInfo::orderId).boxed())
                  .containsExactly(15L, 14L, 13L, 12L, 11L);
            });
  }

  // ─── InsertUser ──────────────────────────────────────────────────────────────

  @Test
  void insertUser_insertsOneRowAndReturnsRowCount() throws Exception {
    CompletableFuture<Integer> future;
    User newUser =
        User_ImmutPojo._builder()
            .id(100L)
            .name("Charulatha")
            .email("charulatha@example.com")
            .phoneNumber("+91-555-0300")
            .address(Address_ImmutJson._builder().city("BLR").zip("560001")._build())
            .secondaryAddresses(List.of())
            .orders(List.of())
            ._build();
    try (KrystexVajramExecutor executor = createExecutor("insertUser")) {
      future =
          executor.execute(
              InsertUser_Req._builder().user(newUser)._build(),
              KryonExecutionConfig.builder().executionId("insertUser_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo(1);

    // Verify the row was actually inserted by reading it back
    CompletableFuture<UserInfo> verifyFuture;
    try (KrystexVajramExecutor executor = createExecutor("insertUser_verify")) {
      verifyFuture =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(100L))._build(),
              KryonExecutionConfig.builder().executionId("insertUser_verify_exec").build());
    }
    assertThat(verifyFuture)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(100L);
              assertThat(user.name()).isEqualTo("Charulatha");
              assertThat(user.contactEmail()).isEqualTo("charulatha@example.com");
              assertThat(user.phoneNumber()).contains("+91-555-0300");
            });

    // Cleanup
    runSql("DELETE FROM UserEntity WHERE id = 100");
  }

  @Test
  void insertUser_insertsUserWithNullPhoneNumber() throws Exception {
    CompletableFuture<Integer> future;
    User newUser =
        User_ImmutPojo._builder()
            .id(101L)
            .name("Danayya")
            .email("danayya@example.com")
            .address(Address_ImmutJson._builder().city("HYD").zip("500001")._build())
            .secondaryAddresses(List.of())
            .orders(List.of())
            ._build();
    try (KrystexVajramExecutor executor = createExecutor("insertUser_nullable")) {
      future =
          executor.execute(
              InsertUser_Req._builder().user(newUser)._build(),
              KryonExecutionConfig.builder().executionId("insertUser_nullable_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo(1);

    // Verify null phoneNumber
    CompletableFuture<UserInfo> verifyFuture;
    try (KrystexVajramExecutor executor = createExecutor("insertUser_nullable_verify")) {
      verifyFuture =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(101L))._build(),
              KryonExecutionConfig.builder()
                  .executionId("insertUser_nullable_verify_exec")
                  .build());
    }
    assertThat(verifyFuture)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.id()).isEqualTo(101L);
              assertThat(user.name()).isEqualTo("Danayya");
              assertThat(user.phoneNumber()).isEmpty();
            });

    // Cleanup
    runSql("DELETE FROM UserEntity WHERE id = 101");
  }

  // ─── InsertOrder ────────────────────────────────────────────────────────────

  @Test
  void insertOrder_insertsOrderWithForeignKey() throws Exception {
    CompletableFuture<Integer> future;
    Order newOrder =
        Order_ImmutPojo._builder()
            .orderId(999L)
            .userId(1L)
            .amountCents(7500L)
            .orderTime(99000L)
            .orderItems(List.of())
            ._build();
    try (KrystexVajramExecutor executor = createExecutor("insertOrder")) {
      future =
          executor.execute(
              InsertOrder_Req._builder().order(newOrder)._build(),
              KryonExecutionConfig.builder().executionId("insertOrder_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo(1);

    // Verify the order was inserted with the correct FK value
    CompletableFuture<List<OrderInfo>> verifyFuture;
    try (KrystexVajramExecutor executor = createExecutor("insertOrder_verify")) {
      verifyFuture =
          executor.execute(
              GetOrderInfoByUserId_Req._builder()
                  .where(OrderUserIdEquals._builder().userIdIs(1L)._build())
                  ._build(),
              KryonExecutionConfig.builder().executionId("insertOrder_verify_exec").build());
    }
    assertThat(verifyFuture)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            orders -> {
              assertThat(orders.stream().filter(o -> o.orderId() == 999L).findFirst())
                  .isPresent()
                  .hasValueSatisfying(
                      o -> {
                        assertThat(o.userId()).isEqualTo(1L);
                        assertThat(o.amountCents()).isEqualTo(7500L);
                      });
            });

    // Cleanup
    runSql("DELETE FROM OrderEntity WHERE orderId = 999");
  }

  // ─── InsertUsers (batch / List<User>) ───────────────────────────────────────

  @Test
  void insertUsers_insertsMultipleUsersAndReturnsRowCount() throws Exception {
    CompletableFuture<Integer> future;
    List<User> newUsers =
        List.of(
            User_ImmutPojo._builder()
                .id(200L)
                .name("Eve")
                .email("eve@example.com")
                .phoneNumber("+1-555-0500")
                .address(Address_ImmutJson._builder().city("LON").zip("E1")._build())
                .secondaryAddresses(List.of())
                .orders(List.of())
                ._build(),
            User_ImmutPojo._builder()
                .id(201L)
                .name("Frank")
                .email("frank@example.com")
                .address(Address_ImmutJson._builder().city("PAR").zip("75001")._build())
                .secondaryAddresses(List.of())
                .orders(List.of())
                ._build());
    try (KrystexVajramExecutor executor = createExecutor("insertUsers")) {
      future =
          executor.execute(
              InsertUsers_Req._builder().users(newUsers)._build(),
              KryonExecutionConfig.builder().executionId("insertUsers_exec").build());
    }
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo(2);

    // Verify both users were inserted
    CompletableFuture<UserInfo> verifyEve;
    try (KrystexVajramExecutor executor = createExecutor("insertUsers_verify_eve")) {
      verifyEve =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(200L))._build(),
              KryonExecutionConfig.builder().executionId("insertUsers_verify_eve_exec").build());
    }
    assertThat(verifyEve)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.name()).isEqualTo("Eve");
              assertThat(user.phoneNumber()).contains("+1-555-0500");
            });

    CompletableFuture<UserInfo> verifyFrank;
    try (KrystexVajramExecutor executor = createExecutor("insertUsers_verify_frank")) {
      verifyFrank =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(201L))._build(),
              KryonExecutionConfig.builder().executionId("insertUsers_verify_frank_exec").build());
    }
    assertThat(verifyFrank)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.name()).isEqualTo("Frank");
              assertThat(user.phoneNumber()).isEmpty();
            });

    // Cleanup
    runSql("DELETE FROM UserEntity WHERE id IN (200, 201)");
  }

  // ─── InsertUserReturning (single INSERT … RETURNING) ───────────────────────

  @Test
  void insertUserReturning_insertsOneRowAndReturnsInsertResult() throws Exception {
    CompletableFuture<UserInsertResult> future;
    User newUser =
        User_ImmutPojo._builder()
            .id(300L)
            .name("Grace")
            .email("grace@example.com")
            .phoneNumber("+1-555-0600")
            .address(Address_ImmutJson._builder().city("TKY").zip("100-0001")._build())
            .secondaryAddresses(List.of())
            .orders(List.of())
            ._build();
    try (KrystexVajramExecutor executor = createExecutor("insertUserReturning")) {
      future =
          executor.execute(
              InsertUserReturning_Req._builder().user(newUser)._build(),
              KryonExecutionConfig.builder().executionId("insertUserReturning_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            result -> {
              assertThat(result).isNotNull();
              assertThat(result.id()).isEqualTo(300L);
            });

    // Verify the row was actually inserted by reading it back
    CompletableFuture<UserInfo> verifyFuture;
    try (KrystexVajramExecutor executor = createExecutor("insertUserReturning_verify")) {
      verifyFuture =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(300L))._build(),
              KryonExecutionConfig.builder()
                  .executionId("insertUserReturning_verify_exec")
                  .build());
    }
    assertThat(verifyFuture)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.name()).isEqualTo("Grace");
            });

    // Cleanup
    runSql("DELETE FROM UserEntity WHERE id = 300");
  }

  // ─── InsertUsersReturning (batch INSERT … RETURNING) ──────────────────────

  @Test
  void insertUsersReturning_insertsMultipleUsersAndReturnsInsertResults() throws Exception {
    CompletableFuture<List<UserInsertResult>> future;
    List<User> newUsers =
        List.of(
            User_ImmutPojo._builder()
                .id(400L)
                .name("Henry")
                .email("henry@example.com")
                .phoneNumber("+1-555-0700")
                .address(Address_ImmutJson._builder().city("BER").zip("10115")._build())
                .secondaryAddresses(List.of())
                .orders(List.of())
                ._build(),
            User_ImmutPojo._builder()
                .id(401L)
                .name("Ivy")
                .email("ivy@example.com")
                .address(Address_ImmutJson._builder().city("SYD").zip("2000")._build())
                .secondaryAddresses(List.of())
                .orders(List.of())
                ._build());
    try (KrystexVajramExecutor executor = createExecutor("insertUsersReturning")) {
      future =
          executor.execute(
              InsertUsersReturning_Req._builder().users(newUsers)._build(),
              KryonExecutionConfig.builder().executionId("insertUsersReturning_exec").build());
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            results -> {
              assertThat(results).hasSize(2);
              assertThat(results.get(0).id()).isEqualTo(400L);
              assertThat(results.get(1).id()).isEqualTo(401L);
            });

    // Verify both users were inserted
    CompletableFuture<UserInfo> verifyHenry;
    try (KrystexVajramExecutor executor = createExecutor("insertUsersReturning_verify_henry")) {
      verifyHenry =
          executor.execute(
              GetUserInfoById_Req._builder().where(UserIdPredicate._builder().idIs(400L))._build(),
              KryonExecutionConfig.builder()
                  .executionId("insertUsersReturning_verify_henry_exec")
                  .build());
    }
    assertThat(verifyHenry)
        .succeedsWithin(TIMEOUT)
        .satisfies(
            user -> {
              assertThat(user).isNotNull();
              assertThat(user.name()).isEqualTo("Henry");
            });

    // Cleanup
    runSql("DELETE FROM UserEntity WHERE id IN (400, 401)");
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private KrystexVajramExecutor createExecutor(String executorId) {
    return createExecutor(executorId, executorLease.get());
  }

  private static KrystexVajramExecutor createExecutor(
      String executorId, SingleThreadExecutor executor) {
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
    traitBinder.bindTrait(GetUserByIdOrName_Req.class).to(GetUserByIdOrName_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetOrdersByTimeRange_Req.class)
        .to(GetOrdersByTimeRange_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetOrdersByMinAmount_Req.class)
        .to(GetOrdersByMinAmount_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetOrdersByMaxAmount_Req.class)
        .to(GetOrdersByMaxAmount_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetOrdersByTimeInRange_Req.class)
        .to(GetOrdersByTimeInRange_VertxSql_Req.class);
    traitBinder.bindTrait(InsertUser_Req.class).to(InsertUser_VertxSql_Req.class);
    traitBinder.bindTrait(InsertOrder_Req.class).to(InsertOrder_VertxSql_Req.class);
    traitBinder.bindTrait(InsertUsers_Req.class).to(InsertUsers_VertxSql_Req.class);
    traitBinder.bindTrait(InsertUserReturning_Req.class).to(InsertUserReturning_VertxSql_Req.class);
    traitBinder
        .bindTrait(InsertUsersReturning_Req.class)
        .to(InsertUsersReturning_VertxSql_Req.class);
    traitBinder
        .bindTrait(GetUserWithAddressById_Req.class)
        .to(GetUserWithAddressById_VertxSql_Req.class);

    kGraph
        .traitDispatchPolicies(
            Stream.of(
                    GetUserInfoById_Req._VAJRAM_ID,
                    GetOrderInfoByUserId_Req._VAJRAM_ID,
                    GetUserOrdersByUserName_Req._VAJRAM_ID,
                    GetUserByIdWithOrdersAndItems_Req._VAJRAM_ID,
                    GetUserByNameWithOrdersAndItems_Req._VAJRAM_ID,
                    GetRecentOrdersByUserId_Req._VAJRAM_ID,
                    GetOrdersWithItemsByUserId_Req._VAJRAM_ID,
                    GetUserByIdOrName_Req._VAJRAM_ID,
                    GetOrdersByTimeRange_Req._VAJRAM_ID,
                    GetOrdersByMinAmount_Req._VAJRAM_ID,
                    GetOrdersByMaxAmount_Req._VAJRAM_ID,
                    GetOrdersByTimeInRange_Req._VAJRAM_ID,
                    InsertUser_Req._VAJRAM_ID,
                    InsertOrder_Req._VAJRAM_ID,
                    InsertUsers_Req._VAJRAM_ID,
                    InsertUserReturning_Req._VAJRAM_ID,
                    InsertUsersReturning_Req._VAJRAM_ID,
                    GetUserWithAddressById_Req._VAJRAM_ID)
                .map(vajramID -> new GuiceyStaticDispatchPolicy(vajramGraph, vajramID, traitBinder))
                .toList())
        .build();
    return kGraph
        .build()
        .createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder()
                        .executorId(executorId)
                        .executorService(executor)
                        .build()));
  }

  private static void runInsertUser(User user, SingleThreadExecutor executor) throws Exception {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor exec = createExecutor("seed_insertUser_" + user.id(), executor)) {
      future =
          exec.execute(
              InsertUser_Req._builder().user(user)._build(),
              KryonExecutionConfig.builder()
                  .executionId("seed_insertUser_" + user.id() + "_exec")
                  .build());
    }
    future.get();
  }

  private static void runInsertOrder(Order order, SingleThreadExecutor executor) throws Exception {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor exec =
        createExecutor("seed_insertOrder_" + order.orderId(), executor)) {
      future =
          exec.execute(
              InsertOrder_Req._builder().order(order)._build(),
              KryonExecutionConfig.builder()
                  .executionId("seed_insertOrder_" + order.orderId() + "_exec")
                  .build());
    }
    future.get();
  }

  private static void runSql(String sql) throws Exception {
    pool.preparedQuery(sql).execute().toCompletionStage().toCompletableFuture().get();
  }

  @Test
  void diagnostic_pgInsertAndReturning() throws Exception {
    try {
      // 1) Basic INSERT without RETURNING — check rowCount
      var rs1 =
          pool.preparedQuery(
                  "INSERT INTO UserEntity (id, name, email, phoneNumber, address, secondaryAddresses) "
                      + "VALUES ($1, $2, $3, $4, $5, $6)")
              .execute(
                  io.vertx.sqlclient.Tuple.of(9999L, "Diag", "diag@test.com", null, "{}", "[]"))
              .toCompletionStage()
              .toCompletableFuture()
              .get();
      System.out.println("[DIAG] INSERT rowCount=" + rs1.rowCount() + " size=" + rs1.size());

      // 2) INSERT with RETURNING — check rows
      var rs2 =
          pool.preparedQuery(
                  "INSERT INTO UserEntity (id, name, email, phoneNumber, address, secondaryAddresses) "
                      + "VALUES ($1, $2, $3, $4, $5, $6) RETURNING id")
              .execute(
                  io.vertx.sqlclient.Tuple.of(
                      9998L, "DiagReturn", "diag2@test.com", null, "{}", "[]"))
              .toCompletionStage()
              .toCompletableFuture()
              .get();
      System.out.println(
          "[DIAG] INSERT RETURNING rowCount=" + rs2.rowCount() + " size=" + rs2.size());
      for (var row : rs2) {
        System.out.println("[DIAG] row: id=" + row.getValue("id") + " columns=" + row.size());
      }

      // 3) Simple SELECT — verify data was inserted
      var rs3 =
          pool.preparedQuery("SELECT count(*) AS cnt FROM UserEntity WHERE id IN ($1, $2)")
              .execute(io.vertx.sqlclient.Tuple.of(9998L, 9999L))
              .toCompletionStage()
              .toCompletableFuture()
              .get();
      for (var row : rs3) {
        System.out.println("[DIAG] SELECT count=" + row.getValue("cnt"));
      }
    } finally {
      runSql("DELETE FROM UserEntity WHERE id IN (9998, 9999)");
    }
  }

  private static class GuiceModule extends AbstractModule {
    @Provides
    @Singleton
    @Named("vertxSql_pool")
    public Pool providePool() {
      return pool;
    }

    @Provides
    @Singleton
    public Json provideJsonProtocol() {
      return Json.JSON;
    }
  }
}
