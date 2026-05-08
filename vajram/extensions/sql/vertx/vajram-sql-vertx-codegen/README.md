# vajram-sql-vertx-codegen

Annotation processor that reads `@SQL @SELECT @Trait` interfaces and generates a complete Vert.x-backed Compute Vajram for each one. The generated vajram delegates SQL execution to [`ExecuteVertxSql`](../vajram-sql-vertx/src/main/java/com/flipkart/krystal/vajram/ext/sql/vertx/ExecuteVertxSql.java) (the IO Vajram), keeping IO isolated from business logic and enabling future batching support.

## What Gets Generated

For each `@SQL @SELECT @Trait` interface the processor generates a single `<TraitName>_VertxSql` class — a `ComputeVajramDef<R>` that implements the trait. The result type `R` is derived from the trait's `TraitDef<R>` signature.

### Generated class structure

| Member | Description |
|---|---|
| `_Inputs` | Mirrors the trait's `_Inputs` interface — the caller-provided WHERE-clause parameters |
| `_InternalFacets` | Declares the injected Vert.x pool (`vertxSql_pool`) and the `ExecuteVertxSql` dependency (`sqlResult`) |
| `@Resolve resolveSql()` | Returns the SQL string built at code-generation time |
| `@Resolve resolveParams(...)` | Builds a `Tuple` of positional parameter values from the WHERE-clause inputs |
| `@Resolve resolvePool(Pool)` | Passes the injected pool to `ExecuteVertxSql` |
| `@Output mapResult(RowSet<Row>)` | Maps the result rows to `R`; behaviour depends on `R` (see below) |

### Why ComputeVajramDef + ExecuteVertxSql?

The generated vajram is a `ComputeVajramDef`, not an `IOVajramDef`. It depends on `ExecuteVertxSql` for the actual I/O. This separation:

- **Isolates IO**: all blocking JDBC/SQL work happens in `ExecuteVertxSql` (an `IOVajramDef`), while the generated class handles only result mapping and input wiring.
- **Enables future batching**: `ExecuteVertxSql` can be enhanced to batch multiple queries without touching any generated code.

### Pool Injection Convention

The connection pool is **not** a caller input — it is injected via Krystal's injection mechanism under the facet name `vertxSql_pool`. Applications bind the pool in their `VajramInjectionProvider`:

```java
injectionProvider.bind("vertxSql_pool", myVertxPool);
```

The `@SQL @SELECT @Trait` interface therefore contains **only functional inputs** — it is free of all infrastructure concerns.

---

## Mandatory `@LIMIT` Rules

Fetching unbounded result sets is a common source of production incidents (OOM, slow queries, accidental full-table scans). The codegen enforces explicit limits at every multi-row boundary:

| Location | Requirement |
|---|---|
| `TraitDef<T>` (single-result, no annotation) | No `@LIMIT` required. The developer asserts the WHERE clause uniquely identifies exactly one row. A runtime error is thrown if the database returns more than one row. |
| `TraitDef<@LIMIT(1) T>` (single-result, first-match) | `@LIMIT(1)` declares that the WHERE clause may match multiple rows, but only the first result is wanted. The SQL wraps the parent in a `LIMIT 1` subquery; no runtime error is thrown when multiple rows match. |
| `TraitDef<List<T>>` (list-result) | Must declare `@LIMIT(N)` on the type argument — use `@LIMIT(LIMIT.NO_LIMIT)` to explicitly opt out |
| `List<Projection> joinField()` method on a projection | Must declare `@LIMIT(N)` — use `@LIMIT(LIMIT.NO_LIMIT)` to explicitly opt out |

`LIMIT.NO_LIMIT` is the constant `int NO_LIMIT = -1` defined on the `@LIMIT` annotation interface. Prefer it over the bare literal for readability:

```java
// Explicit unbounded list
@LIMIT(LIMIT.NO_LIMIT)
List<OrderItemInfo> orderItems();
```

---

## SQL Generation

The SQL string is assembled at code-generation time. Four patterns are recognised depending on the structure of the projection (the `T` in `TraitDef<T>` or `TraitDef<List<T>>`):

### Pattern 1 — Single row (no JOIN)

The projection has only scalar columns. There are two options for a single-row trait:

**`TraitDef<T>` — assert uniqueness (no `@LIMIT` on type arg)**

The developer asserts that the WHERE clause uniquely identifies exactly one row (e.g. querying by primary key or a unique-constraint column). No `LIMIT` is added to the SQL. If the database returns more than one row, `mapResult` throws a runtime error.

**`TraitDef<@LIMIT(1) T>` — first-match semantics**

The developer acknowledges that the WHERE clause may match multiple rows but only wants the first. The SQL includes `LIMIT 1`. No runtime error is thrown when multiple rows match in the database.

**Example trait (`@LIMIT(1)`):** `GetUserInfoById extends TraitDef<@LIMIT(1) UserInfo>`

**Generated SQL:**
```sql
SELECT id, name, email AS contactEmail, phoneNumber
FROM users
WHERE id = $1
LIMIT 1
```

- Columns come from the projection's methods; `@Column("email")` on `contactEmail()` produces `email AS contactEmail`.
- The table name comes from `@Table(name = "users")` on the class referenced by `@Projection(over = User.class)` on the result type.
- `WHERE` predicates come from the `@WHERE`-annotated input types; `UserIdEquals.id()` becomes `id = $1`.

### Pattern 2 — Multiple rows (no JOIN)

`TraitDef<List<T>>` — the projection has only scalar columns but the query may return any number of rows. The generated vajram returns a `List<T>` (never `null`; empty list when there are no results).

**Example trait:** `GetOrderInfoByUserId extends TraitDef<List<OrderInfo>>`

**Generated SQL:**
```sql
SELECT orderId, userId, amountCents
FROM orders
WHERE userId = $1
```

### Pattern 2b — Multiple rows with ORDER BY and LIMIT

Add `@ORDER_BY` and/or `@LIMIT(N)` as type-use annotations on the `List<T>` type argument for list traits that need ordering or a row cap.

**Example trait:**
```java
extends TraitDef<@ORDER_BY(column = "orderTime", direction = DESC) @LIMIT(5) List<OrderInfo>>
```

**Generated SQL:**
```sql
SELECT orderId, userId, amountCents
FROM orders
WHERE userId = $1
ORDER BY orderTime DESC
LIMIT 5
```

`@ORDER_BY` is repeatable — use it multiple times for multi-column ordering. The order in which the annotations are declared is preserved in the `ORDER BY` clause.

### Pattern 3 — Single parent with joined children (LEFT JOIN)

The projection has at least one `List<AnotherProjection>` method. The codegen detects this, looks up the `@ForeignKey` relationship between the two tables, and emits a `LEFT JOIN`. All column aliases are prefixed with `tableName_` to prevent name clashes.

Every `List<Projection>` method must carry `@LIMIT`. How the limit is enforced in SQL depends on whether the parent entity is single-row and on whether the trait carries a type-arg annotation:

- **`TraitDef<T>` (no type-arg annotation)**: the developer asserts the WHERE clause uniquely identifies one parent row. A plain `LEFT JOIN` is emitted; child join limits use `LIMIT N` at the end of the outer query (safe because there is exactly one parent). A runtime error is thrown if the database returns more than one parent row.
- **`TraitDef<@LIMIT(1) T>`**: the developer acknowledges multiple parent rows may match but only wants the first. The parent table is wrapped in a `LIMIT 1` subquery before the join expands children; all child joins use `ROW_NUMBER() OVER (PARTITION BY fk …)` to enforce per-parent caps. No runtime error is thrown when multiple parents match.
- **`TraitDef<List<T>>`** (multi-row parent): a `ROW_NUMBER() OVER (PARTITION BY fk …)` subquery is always used for child limits. Plain `LIMIT N` at the outer level would cap total result rows, not rows per parent.

**Example — assert-unique parent (`TraitDef<T>`, no annotation):** `GetUserWithOrdersAndItems extends TraitDef<UserWithOrdersAndItems>`

```sql
SELECT users.id AS users_id, users.name AS users_name,
       orders.orderId AS orders_orderId, ...
FROM users
LEFT JOIN orders ON users.id = orders.userId
LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY orderId ORDER BY itemPriceCents DESC) AS _rn
           FROM orderItems) orderItems
  ON orders.orderId = orderItems.orderId AND orderItems._rn <= 5
WHERE users.id = $1
ORDER BY orderItems.itemPriceCents DESC
```

**Example — first-match parent (`TraitDef<@LIMIT(1) T>`):** `GetUserOrdersByUserName extends TraitDef<@LIMIT(1) UserNameAndOrders>`

Where `UserNameAndOrders` is:

```java
@Projection(over = User.class)
public interface UserNameAndOrders extends Model {
    String name();

    @ORDER_BY(column = "orderTime", direction = DESC)
    @LIMIT(10)
    List<OrderInfo> orders();    // triggers LEFT JOIN
}
```

**Generated SQL** (`@LIMIT(1)` on type arg → parent subquery, child joins use ROW_NUMBER):
```sql
SELECT users.id AS users_id, users.name AS users_name,
       orders.orderId AS orders_orderId, orders.userId AS orders_userId,
       orders.amountCents AS orders_amountCents
FROM (SELECT * FROM users WHERE name = $1 LIMIT 1) users
LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY userId ORDER BY orderTime DESC) AS _rn
           FROM orders) orders
  ON users.id = orders.userId AND orders._rn <= 10
ORDER BY orders.orderTime DESC
```

The join condition (`users.id = orders.userId`) is discovered automatically:
1. The codegen finds the `@ForeignKey User userId()` method on `Order` — the method name (`userId`) is the FK column; the return type (`User`) identifies the parent table.
2. The `@PrimaryKey` column of `User` (`id`) becomes the right-hand side of the `ON` clause.

WHERE column names are qualified with the table name (e.g. `users.id`) when JOINs are present — the `inTable` attribute of `@WHERE(inTable = User.class)` determines the qualifier.

### Pattern 4 — Multiple parents with joined children (multi-row LEFT JOIN)

`TraitDef<List<T>>` where `T` itself contains a `List<AnotherProjection>` method. The codegen generates a LEFT JOIN query just like Pattern 3, but returns a `List<T>` instead of a single `T`. Use `@ORDER_BY` and/or `@LIMIT` as type-use annotations on the `List<T>` type argument to order or cap the **parent** rows.

#### Why `@LIMIT` on the type argument uses a subquery

Appending `LIMIT N` directly at the end of a `LEFT JOIN` query limits the **total number of result rows** (parents × children combined), not the number of parent rows. For example, with `LIMIT 10` and an average of 3 items per order, you would get only ~3–4 parent rows instead of 10.

The codegen solves this correctly: when `@LIMIT(N)` (with N > 0) is present on the type argument, the parent table is wrapped in a subquery that applies the `LIMIT` and `ORDER BY` before the join expands child rows:

```sql
FROM (SELECT * FROM parent_table WHERE ... ORDER BY ... LIMIT N) parent_table
LEFT JOIN child_table ON parent_table.pk = child_table.fk
```

This guarantees exactly N parent rows and all their children.

When `@LIMIT(LIMIT.NO_LIMIT)` is used, no subquery is added and the join runs without a limit.

**Example trait:**

```java
@SQL @SELECT @Trait @CallGraphDelegationMode(SYNC)
public interface GetOrdersWithItemsByUserId
    extends TraitDef<@ORDER_BY(column = "orderTime", direction = DESC) @LIMIT(10) List<OrderWithItems>> {
    interface _Inputs {
        @IfAbsent(FAIL) OrderUserIdEquals where();
    }
}
```

Where `OrderWithItems` is:

```java
@Projection(over = Order.class)
public interface OrderWithItems extends Model {
    long orderId();
    long amountCents();
    long orderTime();

    @ORDER_BY(column = "itemPriceCents", direction = DESC)
    @LIMIT(5)                    // at most 5 items per order, sorted by price
    List<OrderItemInfo> orderItems();   // triggers LEFT JOIN to orderItems
}
```

**Generated SQL:**
```sql
SELECT orders.orderId      AS orders_orderId,
       orders.amountCents  AS orders_amountCents,
       orders.orderTime    AS orders_orderTime,
       orderItems.orderItemId    AS orderItems_orderItemId,
       orderItems.itemName       AS orderItems_itemName,
       orderItems.itemPriceCents AS orderItems_itemPriceCents
FROM (
  SELECT * FROM orders WHERE userId = $1 ORDER BY orderTime DESC LIMIT 10
) orders
LEFT JOIN (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY orderId ORDER BY itemPriceCents DESC) AS _rn
  FROM orderItems
) orderItems ON orders.orderId = orderItems.orderId AND orderItems._rn <= 5
ORDER BY orders.orderTime DESC, orderItems.itemPriceCents DESC
```

The `WHERE` and parent `LIMIT` are inside the outer subquery. The `ROW_NUMBER` subquery limits each order to at most 5 items, ranked by `itemPriceCents DESC`. The outer `ORDER BY` preserves parent order and sorts items within each parent consistently.

---

## Row Mapping

The `@Output mapResult(RowSet<Row>)` method is generated with one of four strategies matching the four SQL patterns above:

**Pattern 1 — single row:**
```java
@Output @Nullable
static UserInfo mapResult(RowSet<Row> sqlResult) {
    Iterator<Row> _it = sqlResult.iterator();
    if (!_it.hasNext()) return null;
    Row _row = _it.next();
    return UserInfo_ImmutPojo._builder()
        .id(_row.getLong("id"))
        .name(_row.getString("name"))
        .contactEmail(_row.getString("contactEmail"))
        .phoneNumber(_row.getString("phoneNumber"))
        ._build();
}
```

Row getter keys always use the **alias** (the method name in the projection), not the raw DB column name.

**Pattern 2 — list of rows:**
```java
@Output
static List<OrderInfo> mapResult(RowSet<Row> sqlResult) {
    List<OrderInfo> _result = new ArrayList<>();
    for (Row _row : sqlResult) {
        _result.add(OrderInfo_ImmutPojo._builder()
            .orderId(_row.getLong("orderId"))
            .userId(_row.getLong("userId"))
            .amountCents(_row.getLong("amountCents"))
            ._build());
    }
    return _result;
}
```

**Pattern 3 — parent + joined children:**
```java
@Output @Nullable
static UserNameAndOrders mapResult(RowSet<Row> sqlResult) {
    UserNameAndOrders_ImmutPojo.Builder _parent = null;
    List<OrderInfo> _orders = new ArrayList<>();
    for (Row _row : sqlResult) {
        if (_parent == null) {
            _parent = UserNameAndOrders_ImmutPojo._builder()
                .name(_row.getString("users_name"));
        }
        if (_row.getValue("orders_orderId") != null) {
            _orders.add(OrderInfo_ImmutPojo._builder()
                .orderId(_row.getLong("orders_orderId"))
                .userId(_row.getLong("orders_userId"))
                .amountCents(_row.getLong("orders_amountCents"))
                ._build());
        }
    }
    if (_parent == null) return null;
    return _parent.orders(_orders)._build();
}
```

**Pattern 4 — list of parents + joined children:**
```java
@Output
static List<OrderWithItems> mapResult(RowSet<Row> sqlResult) {
    LinkedHashMap<Object, OrderWithItems_ImmutPojo.Builder> _parentBuilders = new LinkedHashMap<>();
    LinkedHashMap<Object, List<OrderItemInfo>> _orderItems = new LinkedHashMap<>();
    for (Row _row : sqlResult) {
        Object _parentKey = _row.getValue("orders_orderId");
        if (_parentKey != null && !_parentBuilders.containsKey(_parentKey)) {
            _parentBuilders.put(_parentKey, OrderWithItems_ImmutPojo._builder()
                .orderId(_row.getLong("orders_orderId"))
                .amountCents(_row.getLong("orders_amountCents"))
                .orderTime(_row.getLong("orders_orderTime")));
            _orderItems.put(_parentKey, new ArrayList<>());
        }
        if (_parentKey != null && _row.getValue("orderItems_orderItemId") != null) {
            _orderItems.get(_parentKey).add(OrderItemInfo_ImmutPojo._builder()
                .orderItemId(_row.getLong("orderItems_orderItemId"))
                .itemName(_row.getString("orderItems_itemName"))
                .itemPriceCents(_row.getLong("orderItems_itemPriceCents"))
                ._build());
        }
    }
    List<OrderWithItems> _result = new ArrayList<>();
    for (Object _key : _parentBuilders.keySet()) {
        _result.add(_parentBuilders.get(_key).orderItems(_orderItems.get(_key))._build());
    }
    return _result;
}
```

Parent rows are accumulated into a `LinkedHashMap` keyed by the parent table's `@PrimaryKey` column alias (e.g. `orders_orderId`). Insertion order is preserved so the result list matches the SQL `ORDER BY` order. Child rows are collected per-parent and attached when building the final list.

The `_ImmutPojo` classes are generated by Krystal's `ModelGenProcessor`. Each projection model must declare `@SupportedModelProtocols(PlainJavaObject.class)` to trigger their generation. This applies to all projections — including nested ones used in JOINs (e.g. `OrderInfo` above).

---

## Adding to Your Module

```groovy
dependencies {
    implementation project(':vajram:extensions:sql:vajram-sql')
    implementation project(':vajram:extensions:sql:vertx:vajram-sql-vertx')

    // Runs during the MODELS codegen phase
    krystalModelsGenProcessor project(':vajram:vajram-codegen')
    krystalModelsGenProcessor project(':vajram:extensions:sql:vertx:vajram-sql-vertx-codegen')
}
```

---

## End-to-End Examples

### Example 1 — single-row query (`GetUserInfoById`)

#### Table model

```java
@ModelRoot
@Table(name = "users")
public interface User extends TableModel {
    @PrimaryKey long id();
    String name();
    @UniqueKey(name = "uk_users_email") String email();
    Optional<String> phoneNumber();
    @IncomingForeignKey List<Order> orders();
}
```

#### Projection

```java
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserInfo extends Model {
    long id();
    String name();
    @Column("email") String contactEmail();
    Optional<String> phoneNumber();
}
```

#### WHERE parameter group

```java
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@WHERE(inTable = User.class)
public interface UserIdEquals extends WhereClause {
    long id();
}
```

#### Query trait

```java
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserInfoById extends TraitDef<@LIMIT(1) UserInfo> {
    interface _Inputs {
        @IfAbsent(FAIL) UserIdEquals where();
    }
}
```

#### Generated vajram (abridged)

```java
@Vajram
public abstract class GetUserInfoById_VertxSql
    extends ComputeVajramDef<UserInfo> implements GetUserInfoById {

  @Resolve(dep = "...:sqlResult", depInputs = ExecuteVertxSql_Req.sql_n)
  static String resolveSql() {
    return "SELECT id, name, email AS contactEmail, phoneNumber FROM users WHERE id = $1 LIMIT 1";
  }

  @Resolve(dep = "...:sqlResult", depInputs = ExecuteVertxSql_Req.params_n)
  static Tuple resolveParams(UserIdEquals where) {
    return Tuple.from(List.of(where.id()));
  }

  @Output @Nullable
  static UserInfo mapResult(RowSet<Row> sqlResult) { /* single-row mapping */ }
}
```

---

### Example 2 — multi-row query (`GetOrderInfoByUserId`)

#### Query trait

```java
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetOrderInfoByUserId extends TraitDef<List<OrderInfo>> {
    interface _Inputs {
        @IfAbsent(FAIL) OrderUserIdEquals where();
    }
}
```

Generated SQL: `SELECT orderId, userId, amountCents FROM orders WHERE userId = $1`

---

### Example 4 — multi-row query with ORDER BY + LIMIT (`GetRecentOrdersByUserId`)

#### Query trait

```java
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetRecentOrdersByUserId
    extends TraitDef<@ORDER_BY(column = "orderTime", direction = DESC) @LIMIT(5) List<OrderInfo>> {
    interface _Inputs {
        @IfAbsent(FAIL) OrderUserIdEquals where();
    }
}
```

Generated SQL: `SELECT orderId, userId, amountCents FROM orders WHERE userId = $1 ORDER BY orderTime DESC LIMIT 5`

---

### Example 3 — JOIN query, first-match parent (`GetUserOrdersByUserName`)

Demonstrates `TraitDef<@LIMIT(1) T>`: the WHERE clause is on a non-unique column (`name`), so multiple users could match. `@LIMIT(1)` tells the framework to take the first match with no runtime error.

#### Projection with nested list

```java
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserNameAndOrders extends Model {
    String name();

    @ORDER_BY(column = "orderTime", direction = DESC)
    @LIMIT(10)
    List<OrderInfo> orders();
}
```

#### Query trait

```java
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserOrdersByUserName extends TraitDef<@LIMIT(1) UserNameAndOrders> {
    interface _Inputs {
        @IfAbsent(FAIL) UserNameEquals where();
    }
}
```

Generated SQL (`@LIMIT(1)` on type arg → parent subquery, child joins use ROW_NUMBER):
```sql
SELECT users.id AS users_id, users.name AS users_name,
       orders.orderId AS orders_orderId, orders.userId AS orders_userId,
       orders.amountCents AS orders_amountCents
FROM (SELECT * FROM users WHERE name = $1 LIMIT 1) users
LEFT JOIN (SELECT *, ROW_NUMBER() OVER (PARTITION BY userId ORDER BY orderTime DESC) AS _rn
           FROM orders) orders
  ON users.id = orders.userId AND orders._rn <= 10
ORDER BY orders.orderTime DESC
```

---

### Example 5 — multi-row LEFT JOIN with parent ORDER BY + LIMIT (`GetOrdersWithItemsByUserId`)

#### Projection with nested list

```java
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = Order.class)
public interface OrderWithItems extends Model {
    long orderId();
    long amountCents();
    long orderTime();

    @ORDER_BY(column = "itemPriceCents", direction = DESC)
    @LIMIT(5)          // at most 5 items per order, sorted by price descending
    List<OrderItemInfo> orderItems();   // triggers LEFT JOIN to orderItems
}
```

#### Query trait

```java
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetOrdersWithItemsByUserId
    extends TraitDef<@ORDER_BY(column = "orderTime", direction = DESC) @LIMIT(10) List<OrderWithItems>> {
    interface _Inputs {
        @IfAbsent(FAIL) OrderUserIdEquals where();
    }
}
```

Generated SQL — the parent table is wrapped in a subquery so `LIMIT 10` applies to parent rows; a `ROW_NUMBER` subquery limits each order to at most 5 items:
```sql
SELECT orders.orderId      AS orders_orderId,
       orders.amountCents  AS orders_amountCents,
       orders.orderTime    AS orders_orderTime,
       orderItems.orderItemId    AS orderItems_orderItemId,
       orderItems.itemName       AS orderItems_itemName,
       orderItems.itemPriceCents AS orderItems_itemPriceCents
FROM (
  SELECT * FROM orders WHERE userId = $1 ORDER BY orderTime DESC LIMIT 10
) orders
LEFT JOIN (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY orderId ORDER BY itemPriceCents DESC) AS _rn
  FROM orderItems
) orderItems ON orders.orderId = orderItems.orderId AND orderItems._rn <= 5
ORDER BY orders.orderTime DESC, orderItems.itemPriceCents DESC
```

Result: a `List<OrderWithItems>` with at most 10 parent orders (sorted by `orderTime DESC`), each with at most 5 line items sorted by `itemPriceCents DESC`.
