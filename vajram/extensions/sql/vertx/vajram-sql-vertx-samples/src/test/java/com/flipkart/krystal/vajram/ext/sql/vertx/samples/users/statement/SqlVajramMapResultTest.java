package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderItemInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderWithItems;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNameAndOrders;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserWithOrdersAndItems;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code mapResult} static methods on the generated {@code *_VertxSql} vajrams.
 *
 * <p>Each test uses Mockito-mocked {@link Row} and {@link RowSet} objects so that no real database
 * connection is required. The tests verify that the generated SQL mapping logic correctly reads
 * column aliases from rows and assembles the projection immutable objects.
 */
class SqlVajramMapResultTest {

  // ─── GetUserInfoById (single row, simple projection) ─────────────────────────

  @Test
  void getUserInfoById_mapsColumnsFromSingleRow() {
    Row row = mockRow();
    stubLong(row, "id", 1L);
    stubString(row, "name", "Alice");
    stubString(row, "contactEmail", "alice@example.com");
    stubString(row, "phoneNumber", "+1-555-0100");

    UserInfo result = GetUserInfoById_VertxSql.mapResult(rowSetOf(row));

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Alice");
    assertThat(result.contactEmail()).isEqualTo("alice@example.com");
    assertThat(result.phoneNumber()).contains("+1-555-0100");
  }

  @Test
  void getUserInfoById_returnsNullForEmptyResultSet() {
    assertThat(GetUserInfoById_VertxSql.mapResult(emptyRowSet())).isNull();
  }

  @Test
  void getUserInfoById_sql_isCorrect() {
    assertThat(GetUserInfoById_VertxSql.resolveSql())
        .isEqualTo(
            "SELECT id, name, email AS contactEmail, phoneNumber FROM users WHERE id = $1 LIMIT 1");
  }

  // ─── GetOrderInfoByUserId (multi-row, simple projection) ─────────────────────

  @Test
  void getOrderInfoByUserId_mapsMultipleRows() {
    Row r1 = mockRow();
    stubLong(r1, "orderId", 10L);
    stubLong(r1, "userId", 1L);
    stubLong(r1, "amountCents", 5000L);

    Row r2 = mockRow();
    stubLong(r2, "orderId", 11L);
    stubLong(r2, "userId", 1L);
    stubLong(r2, "amountCents", 12000L);

    List<OrderInfo> result = GetOrderInfoByUserId_VertxSql.mapResult(rowSetOf(r1, r2));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).orderId()).isEqualTo(10L);
    assertThat(result.get(0).amountCents()).isEqualTo(5000L);
    assertThat(result.get(1).orderId()).isEqualTo(11L);
    assertThat(result.get(1).amountCents()).isEqualTo(12000L);
  }

  @Test
  void getOrderInfoByUserId_returnsEmptyListForNoRows() {
    assertThat(GetOrderInfoByUserId_VertxSql.mapResult(emptyRowSet())).isEmpty();
  }

  @Test
  void getOrderInfoByUserId_sql_isCorrect() {
    assertThat(GetOrderInfoByUserId_VertxSql.resolveSql())
        .isEqualTo("SELECT orderId, userId, amountCents FROM orders WHERE userId = $1");
  }

  // ─── GetRecentOrdersByUserId (@ORDER_BY + @LIMIT on list trait) ───────────────

  @Test
  void getRecentOrdersByUserId_mapsMultipleRows() {
    Row r1 = mockRow();
    stubLong(r1, "orderId", 11L);
    stubLong(r1, "userId", 1L);
    stubLong(r1, "amountCents", 12000L);

    Row r2 = mockRow();
    stubLong(r2, "orderId", 10L);
    stubLong(r2, "userId", 1L);
    stubLong(r2, "amountCents", 5000L);

    List<OrderInfo> result = GetRecentOrdersByUserId_VertxSql.mapResult(rowSetOf(r1, r2));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).orderId()).isEqualTo(11L);
    assertThat(result.get(1).orderId()).isEqualTo(10L);
  }

  @Test
  void getRecentOrdersByUserId_sql_isCorrect() {
    assertThat(GetRecentOrdersByUserId_VertxSql.resolveSql())
        .isEqualTo(
            "SELECT orderId, userId, amountCents FROM orders WHERE userId = $1"
                + " ORDER BY orderTime DESC LIMIT 5");
  }

  // ─── GetOrdersWithItemsByUserId (multi-row LEFT JOIN ordered + limited) ──────

  @Test
  void getOrdersWithItemsByUserId_mapsMultipleParentsWithItems() {
    // Order 10: two items
    Row r1 = orderRow(1L, 10L, 9000L, 2000L, 100L, "Widget A", 999L);
    Row r2 = orderRow(1L, 10L, 9000L, 2000L, 101L, "Widget B", 1999L);
    // Order 11: one item
    Row r3 = orderRow(1L, 11L, 4000L, 1000L, 102L, "Gadget", 4999L);
    // Order 12: no items (LEFT JOIN null)
    Row r4 = mockRow();
    stubObject(r4, "orders_orderId", 12L);
    stubLong(r4, "orders_orderId", 12L);
    stubLong(r4, "orders_amountCents", 500L);
    stubLong(r4, "orders_orderTime", 500L);
    stubObject(r4, "orderItems_orderItemId", null);

    List<OrderWithItems> result =
        GetOrdersWithItemsByUserId_VertxSql.mapResult(rowSetOf(r1, r2, r3, r4));

    assertThat(result).hasSize(3);

    OrderWithItems order10 = result.get(0);
    assertThat(order10.orderId()).isEqualTo(10L);
    assertThat(order10.amountCents()).isEqualTo(9000L);
    assertThat(order10.orderTime()).isEqualTo(2000L);
    assertThat(order10.orderItems()).hasSize(2);
    assertThat(order10.orderItems().get(0).itemName()).isEqualTo("Widget A");
    assertThat(order10.orderItems().get(1).itemName()).isEqualTo("Widget B");

    OrderWithItems order11 = result.get(1);
    assertThat(order11.orderId()).isEqualTo(11L);
    assertThat(order11.orderItems()).hasSize(1);
    assertThat(order11.orderItems().get(0).itemName()).isEqualTo("Gadget");

    OrderWithItems order12 = result.get(2);
    assertThat(order12.orderId()).isEqualTo(12L);
    assertThat(order12.orderItems()).isEmpty();
  }

  @Test
  void getOrdersWithItemsByUserId_returnsEmptyListForNoRows() {
    assertThat(GetOrdersWithItemsByUserId_VertxSql.mapResult(emptyRowSet())).isEmpty();
  }

  @Test
  void getOrdersWithItemsByUserId_sql_isCorrect() {
    String sql = GetOrdersWithItemsByUserId_VertxSql.resolveSql();
    // LIMIT(10) on the root List<T> type scopes to parent rows via a subquery.
    assertThat(sql)
        .contains("FROM (SELECT * FROM orders WHERE userId = $1 ORDER BY orderTime DESC LIMIT 10)")
        // Per-order item limit uses ROW_NUMBER for H2/PostgreSQL compatibility
        .contains(
            "LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY orderId"
                + " ORDER BY itemPriceCents DESC) AS _rn FROM orderItems) orderItems"
                + " ON orders.orderId = orderItems.orderId AND orderItems._rn <= 5")
        // WHERE clause is inside the subquery — must NOT appear at the outer level
        .doesNotContain("WHERE orders.userId")
        // Outer ORDER BY preserves parent ordering and sorts children by price
        .contains("ORDER BY orders.orderTime DESC, orderItems.itemPriceCents DESC");
  }

  // ─── GetUserOrdersByUserName (single-level LEFT JOIN) ────────────────────────

  @Test
  void getUserOrdersByUserName_mapsParentAndChildRows() {
    // Two order rows for the same user
    Row r1 = mockRow();
    stubObject(r1, "users_id", 1L);
    stubString(r1, "users_name", "Bob");
    stubObject(r1, "orders_orderId", 20L);
    stubLong(r1, "orders_orderId", 20L);
    stubLong(r1, "orders_userId", 1L);
    stubLong(r1, "orders_amountCents", 8000L);

    Row r2 = mockRow();
    stubObject(r2, "users_id", 1L);
    stubString(r2, "users_name", "Bob");
    stubObject(r2, "orders_orderId", 21L);
    stubLong(r2, "orders_orderId", 21L);
    stubLong(r2, "orders_userId", 1L);
    stubLong(r2, "orders_amountCents", 3500L);

    UserNameAndOrders result = GetUserOrdersByUserName_VertxSql.mapResult(rowSetOf(r1, r2));

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Bob");
    assertThat(result.orders()).hasSize(2);
    assertThat(result.orders().get(0).orderId()).isEqualTo(20L);
    assertThat(result.orders().get(1).orderId()).isEqualTo(21L);
  }

  @Test
  void getUserOrdersByUserName_returnsParentWithEmptyListWhenNoChildRows() {
    Row r1 = mockRow();
    stubObject(r1, "users_id", 1L);
    stubString(r1, "users_name", "Carol");
    stubObject(r1, "orders_orderId", null); // LEFT JOIN with no matching orders

    UserNameAndOrders result = GetUserOrdersByUserName_VertxSql.mapResult(rowSetOf(r1));

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Carol");
    assertThat(result.orders()).isEmpty();
  }

  @Test
  void getUserOrdersByUserName_throwsWhenMultipleParentsDetected() {
    Row r1 = mockRow();
    stubObject(r1, "users_id", 1L);
    stubString(r1, "users_name", "Dan");
    stubObject(r1, "orders_orderId", null);

    Row r2 = mockRow();
    stubObject(r2, "users_id", 2L); // different parent PK — should trigger the guard
    stubString(r2, "users_name", "Eve");
    stubObject(r2, "orders_orderId", null);

    assertThatThrownBy(() -> GetUserOrdersByUserName_VertxSql.mapResult(rowSetOf(r1, r2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("WHERE clause returned multiple parent rows");
  }

  @Test
  void getUserOrdersByUserName_sql_isCorrect() {
    assertThat(GetUserOrdersByUserName_VertxSql.resolveSql())
        .startsWith("SELECT users.id AS users_id, users.name AS users_name")
        // @LIMIT(1) on type arg → parent wrapped in a subquery with LIMIT 1
        .contains("FROM (SELECT * FROM users WHERE name = $1 LIMIT 1) users")
        // orders has @LIMIT(10) → ROW_NUMBER (parent is multi-row in subquery path)
        .contains(
            "LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY userId"
                + " ORDER BY orderTime DESC) AS _rn FROM orders) orders"
                + " ON users.id = orders.userId AND orders._rn <= 10")
        .contains("ORDER BY orders.orderTime DESC");
  }

  // ─── GetUserByIdWithOrdersAndItems (two-level nested LEFT JOIN) ──────────────

  @Test
  void getUserByIdWithOrdersAndItems_mapsNestedRows() {
    // 2 orders for user 1; order 30 has 2 items, order 31 has 1 item
    Row r1 = orderItemRow(1L, "Alice", 30L, 9000L, 0L, 100L, "Widget A", 999L);
    Row r2 = orderItemRow(1L, "Alice", 30L, 9000L, 0L, 101L, "Widget B", 1999L);
    Row r3 = orderItemRow(1L, "Alice", 31L, 4000L, 0L, 102L, "Gadget", 4999L);
    // order 32 exists but has no items (LEFT JOIN null)
    Row r4 = mockRow();
    stubObject(r4, "users_id", 1L);
    stubString(r4, "users_name", "Alice");
    stubObject(r4, "orders_orderId", 32L);
    stubLong(r4, "orders_orderId", 32L);
    stubLong(r4, "orders_amountCents", 0L);
    stubLong(r4, "orders_orderTime", 1000L);
    stubObject(r4, "orderItems_orderItemId", null); // no items for order 32

    UserWithOrdersAndItems result =
        GetUserByIdWithOrdersAndItems_VertxSql.mapResult(rowSetOf(r1, r2, r3, r4));

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Alice");
    assertThat(result.orders()).hasSize(3);

    OrderWithItems order30 = result.orders().get(0);
    assertThat(order30.orderId()).isEqualTo(30L);
    assertThat(order30.amountCents()).isEqualTo(9000L);
    assertThat(order30.orderItems()).hasSize(2);

    OrderItemInfo item100 = order30.orderItems().get(0);
    assertThat(item100.orderItemId()).isEqualTo(100L);
    assertThat(item100.itemName()).isEqualTo("Widget A");
    assertThat(item100.itemPriceCents()).isEqualTo(999L);

    OrderItemInfo item101 = order30.orderItems().get(1);
    assertThat(item101.itemName()).isEqualTo("Widget B");

    OrderWithItems order31 = result.orders().get(1);
    assertThat(order31.orderItems()).hasSize(1);
    assertThat(order31.orderItems().get(0).itemName()).isEqualTo("Gadget");

    OrderWithItems order32 = result.orders().get(2);
    assertThat(order32.orderItems()).isEmpty();
  }

  @Test
  void getUserByIdWithOrdersAndItems_returnsNullForEmptyResultSet() {
    assertThat(GetUserByIdWithOrdersAndItems_VertxSql.mapResult(emptyRowSet())).isNull();
  }

  @Test
  void getUserByIdWithOrdersAndItems_sql_isCorrect() {
    assertThat(GetUserByIdWithOrdersAndItems_VertxSql.resolveSql())
        // No @LIMIT on type arg → standard path; WHERE goes directly on the outer query.
        // orders has @LIMIT(3) AND nested orderItems → ROW_NUMBER required (outer LIMIT would
        // truncate grandchild rows instead of bounding the number of orders per user).
        .contains(
            "LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY userId"
                + " ORDER BY orderTime DESC) AS _rn FROM orders) orders"
                + " ON users.id = orders.userId AND orders._rn <= 3")
        // orderItems is a nested join (level-2) → always ROW_NUMBER
        .contains(
            "LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY orderId"
                + " ORDER BY itemPriceCents DESC) AS _rn FROM orderItems) orderItems"
                + " ON orders.orderId = orderItems.orderId AND orderItems._rn <= 5")
        .contains("WHERE users.id = $1")
        .contains("ORDER BY orders.orderTime DESC, orderItems.itemPriceCents DESC")
        // No outer LIMIT — ROW_NUMBER is used instead
        .doesNotContain("LIMIT 3");
  }

  // ─── GetUserByNameWithOrdersAndItems (three-level limits: user×orders×items) ──

  @Test
  void getUserByNameWithOrdersAndItems_sql_isCorrect() {
    assertThat(GetUserByNameWithOrdersAndItems_VertxSql.resolveSql())
        // @LIMIT(1) on type arg → parent wrapped in LIMIT 1 subquery
        .contains("FROM (SELECT * FROM users WHERE name = $1 LIMIT 1) users")
        // orders has @LIMIT(3) → ROW_NUMBER per user (subquery path forces useRowNumber)
        .contains(
            "LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY userId"
                + " ORDER BY orderTime DESC) AS _rn FROM orders) orders"
                + " ON users.id = orders.userId AND orders._rn <= 3")
        // orderItems has @LIMIT(5) → ROW_NUMBER per order (nested, always ROW_NUMBER)
        .contains(
            "LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY orderId"
                + " ORDER BY itemPriceCents DESC) AS _rn FROM orderItems) orderItems"
                + " ON orders.orderId = orderItems.orderId AND orderItems._rn <= 5")
        .contains("ORDER BY orders.orderTime DESC, orderItems.itemPriceCents DESC");
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private static <R extends Row> RowSet<R> emptyRowSet() {
    RowSet<R> rows = mock(RowSet.class);
    when(rows.iterator()).thenReturn(rowIteratorOf(List.of()));
    return rows;
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  private static <R extends Row> RowSet<R> rowSetOf(R... rows) {
    RowSet<R> rowSet = mock(RowSet.class);
    when(rowSet.iterator()).thenReturn(rowIteratorOf(List.of(rows)));
    return rowSet;
  }

  /** Wraps a plain {@link List} iterator as a Vert.x {@link RowIterator}. */
  private static <R extends Row> RowIterator<R> rowIteratorOf(List<R> rows) {
    Iterator<R> delegate = rows.iterator();
    return new RowIterator<>() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public R next() {
        return delegate.next();
      }
    };
  }

  private static Row mockRow() {
    return mock(Row.class);
  }

  private static void stubLong(Row row, String col, long value) {
    when(row.getLong(col)).thenReturn(value);
  }

  private static void stubString(Row row, String col, String value) {
    when(row.getString(col)).thenReturn(value);
  }

  private static void stubObject(Row row, String col, Object value) {
    when(row.getValue(col)).thenReturn(value);
  }

  /** Builds a row for GetOrdersWithItemsByUserId tests (parent=orders, child=orderItems). */
  private static Row orderRow(
      long userId,
      long orderId,
      long amountCents,
      long orderTime,
      long itemId,
      String itemName,
      long itemPriceCents) {
    Row row = mockRow();
    stubObject(row, "orders_orderId", orderId);
    stubLong(row, "orders_orderId", orderId);
    stubLong(row, "orders_amountCents", amountCents);
    stubLong(row, "orders_orderTime", orderTime);
    stubObject(row, "orderItems_orderItemId", itemId);
    stubLong(row, "orderItems_orderItemId", itemId);
    stubString(row, "orderItems_itemName", itemName);
    stubLong(row, "orderItems_itemPriceCents", itemPriceCents);
    return row;
  }

  /** Builds a fully populated nested-join row for GetUserWithOrdersAndItems tests. */
  private static Row orderItemRow(
      long userId,
      String userName,
      long orderId,
      long amountCents,
      long orderTime,
      long itemId,
      String itemName,
      long itemPriceCents) {
    Row row = mockRow();
    stubObject(row, "users_id", userId);
    stubString(row, "users_name", userName);
    stubObject(row, "orders_orderId", orderId);
    stubLong(row, "orders_orderId", orderId);
    stubLong(row, "orders_amountCents", amountCents);
    stubLong(row, "orders_orderTime", orderTime);
    stubObject(row, "orderItems_orderItemId", itemId);
    stubLong(row, "orderItems_orderItemId", itemId);
    stubString(row, "orderItems_itemName", itemName);
    stubLong(row, "orderItems_itemPriceCents", itemPriceCents);
    return row;
  }
}
