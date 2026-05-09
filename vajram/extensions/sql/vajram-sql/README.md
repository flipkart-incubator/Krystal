# vajram-sql

Developer SDK for declaratively modeling SQL tables and query contracts in the Krystal framework.
You define your schema and query shapes as annotated Java interfaces; the companion codegen module (
`vajram-sql-vertx-codegen`) generates all the SQL execution infrastructure.

---

## Design Philosophy

### Projection reuse through WHERE clause separation

Projections (what columns to fetch) and WHERE clauses (which rows to fetch) are modeled as distinct
types by design. The same `@Projection` can be reused across multiple `@SQL @SELECT @Trait`
definitions that apply different WHERE predicates. For example, a `UserInfo` projection can serve
both `GetUserById` and `GetUserByEmail` without duplication — only the WHERE input type changes.

### Performance and transparency through code generation

vajram-sql generates SQL at compile time rather than at runtime via reflection. This means:

- The exact SQL that will be executed is visible as a literal string in the generated source file —
  it is inspectable, diffable, and reviewable.
- No reflection overhead at runtime.
- Type errors and schema mismatches are caught at compile time, not in production.

### Declarative definitions, imperative execution

You write declarative annotations (`@Table`, `@Projection`, `@WHERE`, `@ORDER`). The codegen
converts these into the imperative Java boilerplate — the `SELECT` string, the `Tuple.from(...)`
parameter binding, the `RowSet<Row>` mapping loop. This keeps your code declarative and easy to
review while keeping the execution path explicit and traceable.

### How vajram-sql differs from Hibernate-style ORMs

In Hibernate and similar ORMs, the same entity class is reused everywhere. A `User` entity carries
every column and every relationship. Queries return `User` objects that may have some fields loaded
and others null or lazy — the caller must know which fields were requested to avoid
`LazyInitializationException` or silent nulls.

vajram-sql takes the opposite approach: **each query has its own projection tightly coupled to its
business use case**. A `UserSummary` projection fetches just `id` and `name`; a `UserProfile`
projection fetches `id`, `name`, `email`, and `phoneNumber`. Neither projection can be confused with
the other, and both are always fully populated — there are no nullable fields from under-fetching
and no wasted data from over-fetching.

This also eliminates N+1 problems by construction: nested `List<Projection>` methods generate a
single LEFT JOIN query, not a loop of separate queries.

---

## Concepts

### Tables

A table is a [
`@ModelRoot`](../../../../../../../../krystal-common/src/main/java/com/flipkart/krystal/model/ModelRoot.java)
interface that extends [
`TableModel`](src/main/java/com/flipkart/krystal/vajram/ext/sql/model/TableModel.java) and is
annotated with [`@Table`](src/main/java/com/flipkart/krystal/vajram/ext/sql/model/Table.java). Each
abstract method corresponds to a column — the method name is the column name and the return type is
the column's Java type.

**Structural invariants (enforced at compile time):**

- A `@Table` interface **must** also carry `@ModelRoot`; omitting it produces:
  `[vajram-sql] @Table interface '...' must also be annotated with @ModelRoot.`
- A `@Table` interface **must** extend `TableModel`; omitting it produces:
  `[vajram-sql] @Table interface '...' must extend TableModel.`

```java

@ModelRoot
@Table(name = "users")
public interface User extends TableModel {

  @PrimaryKey
  long id();

  String name();

  @UniqueKey(name = "uk_users_email")
  String email();

  Optional<String> phoneNumber();  // nullable column

  @IncomingForeignKey
  List<Order> orders();            // reverse FK — not a real DB column
}
```

#### Column Constraints

| Annotation                          | Target    | Meaning                                                                                                        |
|-------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------|
| `@PrimaryKey`                       | method    | Marks the column as the primary key                                                                            |
| `@UniqueKey(name)`                  | method    | Single-column unique constraint                                                                                |
| `@UniqueKey(name, columns = {...})` | interface | Composite unique key across multiple columns                                                                   |
| `@ForeignKey`                       | method    | Outgoing FK — the method name is the FK column name; the return type must be the referenced table's model type |
| `@IncomingForeignKey`               | method    | Models the reverse side of a FK that lives on another table; **not** a real DB column                          |

#### Foreign Keys

`@ForeignKey` is declared on the owning side (the table that holds the FK column). The method return
type is the referenced table model — this is what lets the codegen discover join conditions
automatically.

```java

@ModelRoot
@Table(name = "orders")
public interface Order extends TableModel {

  @PrimaryKey
  long orderId();

  @ForeignKey
  User userId();          // FK column "userId" → users.id

  long amountCents();

  long orderTime();
}
```

The referenced table **must** also declare the reverse side with `@IncomingForeignKey` for any JOIN
to be valid:

```java

@IncomingForeignKey
List<Order> orders();       // reverse of Order.userId — not a real DB column
```

**Bidirectional-FK invariant (enforced at compile time):** a `List<@Projection>` join in a
`@Projection` interface is only valid when both directions of the FK relationship are declared:

- The **child** table must have a `@ForeignKey`-annotated method whose return type is the **parent**
  table model.
- The **parent** table must have an `@IncomingForeignKey`-annotated method whose return type is
  `List<ChildTable>` or `ChildTable`.

If either annotation is absent the code generator reports a compile-time error pointing to the
`List<@Projection>` method that triggered the join.

**Additional invariants:**

- A `@ForeignKey` method's return type **must** be a `@Table`-annotated model interface. Referencing
  a non-table type will prevent the codegen from discovering the join condition.
- An `@IncomingForeignKey` method must **not** represent a real DB column. It models the reverse
  side of a relationship that is physically stored on another table.

#### Composite Unique Key

```java

@ModelRoot
@UniqueKey(name = "uk_user_email_type", columns = {"userId", "emailType"})
@Table(name = "user_emails")
public interface UserEmail extends TableModel {

  @PrimaryKey
  long id();

  long userId();

  String emailType();

  String email();
}
```

---

### Projections

A projection is a [
`@ModelRoot`](../../../../../../../../krystal-common/src/main/java/com/flipkart/krystal/model/ModelRoot.java)
interface annotated with [
`@Projection(over = TableClass.class)`](src/main/java/com/flipkart/krystal/vajram/ext/sql/statement/Projection.java).
It defines the shape of data returned by a SELECT query — which columns to include and how to name
them. Each method in the projection corresponds to a column in the underlying table.

To be instantiable at runtime the projection must also carry
`@SupportedModelProtocols(PlainJavaObject.class)` so that Krystal generates a concrete `_ImmutPojo`
implementation.

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserInfo extends Model {

  long id();

  String name();

  @Column("email")
  String contactEmail();   // DB column "email", aliased to "contactEmail"

  Optional<String> phoneNumber();
}
```

**Invariants:**

- A `@Projection` **must** be backed by a `@Table` model via the `over` attribute. The referenced
  table is the source of truth for column names, types, and nullability.
- Every method in a projection must correspond to a real column in the underlying table (by the
  method name, or by the `@Column("name")` override). Methods referencing non-existent columns will
  produce a compile-time error during codegen.
- A nullable column (i.e., one that allows SQL `NULL`) may be typed as `Optional<T>` **or** as
  `@org.checkerframework.checker.nullness.qual.Nullable T`. Both signal to the codegen and to
  readers that the value may be absent. Using a plain non-nullable type for a nullable column will
  cause a `NullPointerException` at runtime when the column value is `NULL`.
- A projection is **never** a table model. Do not annotate a projection with `@Table`. The SELECT
  trait's `TraitDef<T>` result type must be a `@Projection`, not a `@Table` model. Returning a table
  model directly couples query results to the full schema and defeats the purpose of
  projection-per-use-case.

#### Column Aliasing

When a method name differs from the actual column name, annotate the method with [
`@Column("columnName")`](src/main/java/com/flipkart/krystal/vajram/ext/sql/statement/Column.java).
The method name becomes the alias in the generated `SELECT` clause.

```java

@Column("email")
String contactEmail();
// → generates "email AS contactEmail" in the SELECT clause
```

#### Nested Projections and LEFT JOINs

A projection method whose return type is `List<AnotherProjection>` — where `AnotherProjection` is
itself a `@Projection` — signals a **LEFT JOIN**. The codegen automatically discovers the join
condition from the `@ForeignKey` relationship between the two tables.

**Single-level LEFT JOIN:**

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserNameAndOrders extends Model {

  String name();

  @ORDER(by = "orderTime", direction = DESC)
  @LIMIT(10)
  List<OrderInfo> orders();    // LEFT JOIN orders ON users.id = orders.userId
}
```

**Two-level nested LEFT JOIN** (parent → child → grandchild):

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = Order.class)
public interface OrderWithItems extends Model {

  long orderId();

  long amountCents();

  long orderTime();

  @LIMIT(LIMIT.NO_LIMIT)
  List<OrderItemInfo> orderItems();    // nested join: orders → orderItems
}

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserWithOrdersAndItems extends Model {

  String name();

  @ORDER(by = "orderTime", direction = DESC)
  @LIMIT(3)
  List<OrderWithItems> orders();       // joins: users → orders → orderItems
}
```

The codegen generates a single SQL query with two LEFT JOINs and uses a `LinkedHashMap`-based
deduplication strategy to reconstruct the nested object graph from the flat result set.

| Annotation              | Target                    | Meaning                                                                                  |
|-------------------------|---------------------------|------------------------------------------------------------------------------------------|
| `@ORDER(by, direction)` | `List<Projection>` method | Adds an `ORDER BY` clause on the joined table's column                                   |
| `@LIMIT(n)`             | `List<Projection>` method | **Required.** Adds a `LIMIT` clause. Use `@LIMIT(LIMIT.NO_LIMIT)` to explicitly opt out. |

`@ORDER` is repeatable — apply it multiple times for multi-column ordering.

`LIMIT.NO_LIMIT` is the constant `int NO_LIMIT = -1` on the `@LIMIT` annotation interface, used to
declare an explicit "fetch all" intent without magic numbers:

```java

@LIMIT(LIMIT.NO_LIMIT)
List<OrderInfo> orders();  // all orders, explicitly unbounded
```

**Invariants:**

- A `List<AnotherProjection>` method in a parent projection signals a LEFT JOIN. The child
  projection must also be annotated with `@Projection(over = ChildTable.class)`.
- **`@LIMIT` is mandatory** on every `List<Projection>` join method. Omitting it produces a
  compile-time error. The requirement exists because unbounded JOINs can return excessive data. Use
  `@LIMIT(LIMIT.NO_LIMIT)` to explicitly opt out.
- **Bidirectional FK required:** the child table must have `@ForeignKey` pointing to the parent
  table AND the parent table must have `@IncomingForeignKey` pointing back to the child table. If
  either is absent, codegen reports a compile-time error on the `List<@Projection>` method that
  caused the join.
- For multi-level joins, the intermediate table's projection must include the primary key column so
  that the codegen can use it as the deduplication key when grouping grandchild rows.
- The generated LEFT JOIN returns `NULL` in child columns when there is no matching child row. This
  means child rows with all `NULL` columns are silently dropped, and an empty `List` is returned —
  correct behaviour for a LEFT JOIN with no matches.

---

### SQL Query Traits

The code generation trigger is a `@SQL @SELECT @Trait` interface — a Krystal trait that declares the
WHERE-clause inputs and the result type. The codegen module generates a complete Vert.x-backed
Compute Vajram for each such trait.

`@LIMIT` and `@ORDER` are placed as **type-use annotations on the `TraitDef<T>` type argument** —
not on the trait interface itself. This makes the query contract self-contained: the type signature
declares both _what_ is returned and _how many_ rows are expected.

**Single-row result** (`TraitDef<@LIMIT(1) T>`):

```java

@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserInfoById extends TraitDef<@LIMIT(1) UserInfo> {

  interface _Inputs {

    @IfAbsent(FAIL)
    UserIdEquals where();
  }
}
```

**Multi-row result with ORDER BY and LIMIT** (`TraitDef<@ORDER @LIMIT List<T>>`):

```java

@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetRecentOrdersByUserId
    extends TraitDef<@ORDER(by = "orderTime", direction = DESC) @LIMIT(5) List<OrderInfo>> {

  interface _Inputs {

    @IfAbsent(FAIL)
    OrderUserIdEquals where();
  }
}
```

Key points:

- `@SQL` — marks the trait as SQL-backed.
- `@SELECT` — declares this is a SELECT statement; required alongside `@SQL` for codegen to fire.
- `TraitDef<T>` — `T` is the result type. Use `TraitDef<List<T>>` for queries that return multiple
  rows.
- `@LIMIT(1)` on the type arg — required for single-result traits; added as `LIMIT 1` to the
  generated SQL for simple queries. For JOIN queries (projections with `List<ChildProjection>`
  methods), `@LIMIT(1)` is still required but is *not* added to the SQL — applying LIMIT at the
  outer query level would truncate joined child rows.
- `@LIMIT(N)` with N > 1 or `@LIMIT(LIMIT.NO_LIMIT)` — **required** on `List<T>` result traits. A
  plain `TraitDef<List<T>>` without `@LIMIT` produces a compile-time error. `LIMIT.NO_LIMIT` = -1
  means no limit.
- For `TraitDef<@LIMIT(N) List<T>>` where `T` contains a join: the LIMIT applies to the number of *
  *parent rows** returned, implemented via a subquery (not a plain `LIMIT N` at the end). See the
  codegen README for details.
- `@ORDER(by, direction)` on the type arg — appends `ORDER BY col [ASC|DESC]` to the generated SQL
  for simple queries. Repeatable for multi-column ordering.
- `_Inputs` should contain **only functional/business inputs** (WHERE-clause values). Infrastructure
  like the connection pool is injected into the generated vajram automatically.
- `@CallGraphDelegationMode(SYNC)` is required by Krystal's trait system.

**Invariants:**

- `T` in `TraitDef<T>` or `TraitDef<List<T>>` **must** be a `@Projection`-annotated interface. If
  `T` is a `@Table` model, codegen will report an error. This enforces the separation between table
  schema and query result shape.
- A **single-result** trait (`TraitDef<T>`) **must** declare `@LIMIT(1)` on the type argument.
  Omitting it produces a compile-time error: `[SqlTraitVajramGen] ... does not declare @LIMIT(1)`.
  This makes the single-row expectation explicit.
- A **list-result** trait (`TraitDef<List<T>>`) **must** declare `@LIMIT` on the type argument.
  Omitting it produces a compile-time error:
  `[SqlTraitVajramGen] ... returns a List but has no @LIMIT`. Use `@LIMIT(LIMIT.NO_LIMIT)` to
  explicitly fetch all rows.
- `@LIMIT(1)` is invalid on a `List<T>` result trait — use `TraitDef<@LIMIT(1) T>` instead.
- For a **single-parent LEFT JOIN** result (`TraitDef<@LIMIT(1) T>` where `T` has join methods), the
  WHERE clause is implicitly expected to identify a **single parent entity**. If the WHERE clause
  returns rows belonging to multiple parent entities, the generated `mapResult` will throw an
  `IllegalStateException` at runtime.

#### Single vs. Multiple Results

| Trait signature                                    | Generated SQL shape                                                   | `mapResult` behaviour                                         |
|----------------------------------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------|
| `TraitDef<@LIMIT(1) UserInfo>`                     | `... WHERE ... LIMIT 1`                                               | Maps the first row; returns `null` if empty                   |
| `TraitDef<@LIMIT(1) UserNameAndOrders>` (JOIN)     | `... WHERE ...` (no outer LIMIT uses sub query LIMIT for correctness) | Maps single parent + joined children; returns `null` if empty |
| `TraitDef<@LIMIT(LIMIT.NO_LIMIT) List<OrderInfo>>` | `... WHERE ...` (no LIMIT)                                            | Maps all rows; returns empty list if none                     |
| `TraitDef<@LIMIT(5) List<OrderInfo>>`              | `... WHERE ... LIMIT 5`                                               | Maps up to 5 rows; returns empty list if none                 |
| `TraitDef<@LIMIT(10) List<OrderWithItems>>` (JOIN) | `FROM (SELECT * FROM … LIMIT 10) parent LEFT JOIN child`              | Maps up to 10 parent rows, each with all their children       |

#### WHERE Clause Inputs

WHERE clause parameters are interfaces annotated with `@WHERE(inTable = TableClass.class)`. Each
method in the interface maps to a `col = $N` positional placeholder in the generated `WHERE` clause.

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@WHERE(inTable = User.class)
public interface UserIdEquals extends WhereClause {

  long id();   // → "id = $1" in the WHERE clause
}
```

The `inTable` attribute tells the codegen which table the WHERE column belongs to — this is required
for queries involving JOINs so that column names are qualified correctly (`users.id = $1` rather
than just `id = $1`).

**Structural invariants (enforced at compile time):**

- A `@WHERE` interface **must** also carry `@ModelRoot`; omitting it produces:
  `[vajram-sql] @WHERE interface '...' must also be annotated with @ModelRoot.`
- A `@WHERE` interface **must** extend `WhereClause`; omitting it produces:
  `[vajram-sql] @WHERE interface '...' must extend WhereClause.`

---

### INSERT / UPDATE / DELETE (planned)

[`@INSERT`](src/main/java/com/flipkart/krystal/vajram/ext/sql/statement/INSERT.java), [
`@UPDATE`](src/main/java/com/flipkart/krystal/vajram/ext/sql/statement/UPDATE.java), and [
`@DELETE`](src/main/java/com/flipkart/krystal/vajram/ext/sql/statement/DELETE.java) annotations
exist and follow the same design principle but codegen support is not yet implemented.

---

## How to execute an SQL statement using vajram-sql

This section walks through the end-to-end workflow for adding a new SQL query to your application.

### Step 1 — Model the table (once per table)

Create a `@ModelRoot @Table` interface for each database table. This is done once and shared across
all queries that touch the table.

```java

@ModelRoot
@Table(name = "users")
public interface User extends TableModel {

  @PrimaryKey
  long id();

  String name();

  @UniqueKey(name = "uk_users_email")
  String email();

  Optional<String> phoneNumber();

  @IncomingForeignKey
  List<Order> orders();   // required for any JOIN that starts from users
}
```

### Step 2 — Define the Projection

Create a `@Projection` interface for the specific data shape your query needs. Name it after the use
case, not the table.

**Simple projection (single or multiple rows):**

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserSummary extends Model {

  long id();

  String name();
}
```

**Projection with a LEFT JOIN:**

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserWithRecentOrders extends Model {

  String name();

  @ORDER(by = "orderTime", direction = DESC)
  @LIMIT(5)
  List<OrderInfo> orders();
}
```

### Step 3 — Define the WHERE clause input(s)

Create a `@WHERE` interface for each group of WHERE predicates. One interface per equality condition
group is the norm.

```java

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@WHERE(inTable = User.class)
public interface UserIdEquals extends WhereClause {

  long id();
}
```

### Step 4 — Declare the SELECT Trait

Create a `@SQL @SELECT @Trait` interface that wires the projection to the WHERE inputs. Place
`@LIMIT` and `@ORDER` as type-use annotations on the `TraitDef<T>` type argument.

```java
// Single-row result — @LIMIT(1) required on the type argument
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserSummaryById extends TraitDef<@LIMIT(1) UserSummary> {

  interface _Inputs {

    @IfAbsent(FAIL)
    UserIdEquals userIdEquals();
  }
}

// Multi-row result — explicitly unbounded (LIMIT.NO_LIMIT required)
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetAllUsers extends TraitDef<@LIMIT(LIMIT.NO_LIMIT) List<UserSummary>> {

  interface _Inputs {

  }   // no WHERE clause — fetch all rows
}

// Multi-row result with ORDER BY + LIMIT
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetRecentOrdersByUserId
    extends TraitDef<@ORDER(by = "orderTime", direction = DESC) @LIMIT(5) List<OrderInfo>> {

  interface _Inputs {

    @IfAbsent(FAIL)
    OrderUserIdEquals where();
  }
}

// LEFT JOIN result — @LIMIT(1) required; NOT added to the SQL (would truncate joined rows)
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserWithRecentOrders extends TraitDef<@LIMIT(1) UserWithRecentOrders> {

  interface _Inputs {

    @IfAbsent(FAIL)
    UserIdEquals userIdEquals();
  }
}
```

### Step 5 — Run the annotation processor

The codegen module (`vajram-sql-vertx-codegen`) is an annotation processor. Add it to your module's
`build.gradle`:

```groovy
dependencies {
    krystalModelsGenProcessor project(':vajram:extensions:sql:vertx:vajram-sql-vertx-codegen')
}
```

Build the module (or run the `krystalModelsGen` task). The processor generates a `*_VertxSql`
Compute Vajram class for each `@SQL @SELECT @Trait`. For `GetUserSummaryById` it produces
`GetUserSummaryById_VertxSql` in the same package, under `build/generated/sources/`.

### Step 6 — Configure the connection pool

The generated vajram expects a `io.vertx.sqlclient.Pool` to be available under the qualifier name
`vertxSql_pool`. Configure it in the krystal injection config.

### Step 7 — Invoke the generated vajram

Call the generated `*_VertxSql` vajram from any upstream vajram in your call graph. The vajram
accepts the WHERE clause inputs and returns the projection result asynchronously.

```java
// As a dependency in another vajram:
@Dependency(onVajram = GetUserSummaryById_VertxSql.class)
UserSummary userSummary;
```

---

### Query pattern summary

| Use case                                      | Projection                         | Trait signature                           | `@LIMIT` requirement                                                                                                              |
|-----------------------------------------------|------------------------------------|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Fetch a single row by PK or UK                | Scalar methods only                | `TraitDef<T>`                             | Not Required — enforces single-row intent at runtime if more than one rwo is returned                                             |
| Fetch a single row by non unique where clause | Scalar methods only                | `TraitDef<@LIMIT(1) T>`                   | Required — enforces single-row in the query by applying limit                                                                     |
| Fetch multiple rows (bounded)                 | Scalar methods only                | `TraitDef<@LIMIT(N) List<T>>`             | **Required** — `SqlTraitVajramGen` emits a compile error if `TraitDef<List<T>>` has no `@LIMIT`; use `@LIMIT(N)` to cap at N rows |
| Fetch N most recent rows                      | Scalar methods only                | `TraitDef<@ORDER(...) @LIMIT(N) List<T>>` | **Required** — `@LIMIT(N)` with N > 1                                                                                             |
| Fetch one parent + its children               | One `List<ChildProjection>` method | `TraitDef<@LIMIT(1) T>`                   | Required — Interpretation: pick first parent.                                                                                     |
| Fetch parent → child → grandchild             | Nested `List<ChildProjection>`     | `TraitDef<@LIMIT(1) T>`                   | Required — Interpretation: pick first parent.                                                                                     |
| Fetch all rows (unbounded)                    | Scalar methods only                | `TraitDef<@LIMIT(NO_LIMIT) List<T>>`      | **Required** — `@LIMIT(NO_LIMIT)` is the explicit opt-out; omitting `@LIMIT` entirely is a compile error                          |

---

## Dependencies

This module depends only on `krystal-common` and carries no runtime overhead — it contains pure
annotations and interfaces consumed at compile time by the codegen processor.
