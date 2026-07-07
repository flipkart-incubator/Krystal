# vajram-sql SELECT/INSERT query reference

Source of truth: `com.flipkart.krystal.vajram.ext.sql.lang` (query annotations) and
`com.flipkart.krystal.vajram.ext.sql.model.Selection` in
[flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal), plus the real, compiling samples under
`vajram/extensions/sql/vertx/vajram-sql-vertx-samples`. Every snippet below is drawn from that samples module or
verified directly against annotation source — not from the plugin's prose docs (see the doc/source drift warning in
`references/annotations.md`, which applies here too).

A query in vajram-sql is a **Krystal `Trait`** — the same `@Trait`/`TraitDef<T>` mechanism used for any Vajram trait,
specialized with SQL annotations. Writing one means composing three pieces: a **projection** (what columns come
back), an optional **predicate** (which rows), and the **trait** itself (glues them together and is what gets
codegen'd into a real Vajram that executes the SQL).

## The three pieces

### 1. Projection — `@Selection(from = ...)`

A plain `Model` interface where each method is a column (or alias) you want back, for SELECT — or the shape you
want returned after an INSERT (see `@ReturnOnInsert` below).

```java
@ModelRoot(type = ModelRoot.ModelType.RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@Selection(from = User.class)
public interface UserInfo extends Model {
  long id();
  String name();

  @Column("email")       // DB column is `email`; this method aliases it
  String contactEmail();

  Optional<String> phoneNumber();
}
```

A projection can also pull in a **related table's rows** — declare a method returning `List<OtherSelection>` (or a
single `OtherSelection`) and it becomes a join:

```java
@Selection(from = Order.class)
public interface OrderWithItems extends Model {
  long orderId();
  long amountCents();

  @ORDER(by = "itemPriceCents", direction = DESC)
  @LIMIT(5)
  List<OrderItemInfo> orderItems();   // generates a LEFT JOIN orders -> orderItems
}
```

This only works because `Order`/`OrderItem` already have the `@ForeignKey`/`@IncomingForeignKey` pair declared on
their `@Table` models (see `references/annotations.md`) — the join follows that relationship. `@ORDER`/`@LIMIT` on a
nested `List<...>` method scope to that join, independent of any `@ORDER`/`@LIMIT` on the outer trait result.

### 2. Predicate (WHERE clause) — `@WHERE(inTable = ...)` + `ColumnPredicate`

A `ColumnPredicate` is a `Model` interface where each method is one column condition, tagged with a comparison
operator annotation from `com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison`:

```java
@ModelRoot
@WHERE(inTable = User.class)
public interface UserIdPredicate extends ColumnPredicate {
  @Column("id")
  @IsEqualTo
  long idIs();

  static UserIdPredicate_ImmutPojo.Builder _builder() {
    return UserIdPredicate_ImmutPojo._builder();
  }
}
```

If a `ColumnPredicate` declares **multiple** methods, they're implicitly AND'ed. Available comparison operators
(each `@Target(METHOD)`, applied above the column method, method name itself is just a label — semantics come from
the annotation):

| Operator | SQL | Notes |
|---|---|---|
| `@IsEqualTo` | `=` | any type |
| `@IsGreaterThan` / `@IsLessThan` | `>` / `<` | numeric or temporal (`LocalDate`, `LocalDateTime`, `OffsetDateTime`) |
| `@IsGreaterThanOrEqual` / `@IsLessThanOrEqual` | `>=` / `<=` | same type constraints |
| `@IsInRange` | dynamic, based on the `Range<T>` passed at runtime | method must return `com.google.common.collect.Range<T>`; `Range.closed/open/closedOpen/openClosed` map to the corresponding `>=`/`>`/`<=`/`<` combination. Use this only when the caller should control the range shape — otherwise prefer the fixed comparison operators above. |

For **OR** across predicates, wrap them in a `SqlOrPredicate`:

```java
@ModelRoot
public interface UserOrPredicate extends SqlOrPredicate {
  UserIdPredicate orWithUserId();
  UserNamePredicate orWithUserName();

  static UserOrPredicate_ImmutPojo.Builder _builder() {
    return UserOrPredicate_ImmutPojo._builder();
  }
}
```

Note the `static _builder()` method on every predicate interface — it returns the annotation-processor-generated
immutable builder (`<Interface>_ImmutPojo` in current samples; the exact generated class name is whatever the
codegen produces, so this method won't resolve until you've compiled once). Calling code constructs predicate
instances through this builder to pass into a trait's `_Inputs`.

A `@WHERE`-annotated interface is scoped to one table (`inTable = ...`) — if a query needs to filter across a join,
put the predicate on whichever table the filtered column actually belongs to; the join itself is driven by the
`@Selection`'s nested field, not by the predicate.

### 3. The trait itself — `@SQL` + `@SELECT`/`@INSERT` + `@Trait`

This is what actually gets compiled into a real, executable Vajram.

**SELECT, single row:**

```java
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserInfoById extends TraitDef<@LIMIT(1) UserInfo> {
  interface _Inputs {
    @IfAbsent(FAIL)
    UserIdPredicate where();
  }
}
```

**SELECT, list, with ordering:**

```java
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetOrdersByTimeInRange
    extends TraitDef<@ORDER(by = "orderTime", direction = ASC) @LIMIT(LIMIT.NO_LIMIT) List<OrderInfo>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    OrderTimeIsInRange where();
  }
}
```

**INSERT, plain (returns row count):**

```java
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@INSERT
@Trait
@CallGraphDelegationMode(SYNC)
public interface InsertUser extends TraitDef<Integer> {
  interface _Inputs {
    @IfAbsent(FAIL)
    User user();
  }
}
```

**INSERT, returning generated columns** — pair with a `@ReturnOnInsert` projection:

```java
@ModelRoot(type = ModelRoot.ModelType.RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@ReturnOnInsert(inTable = User.class)
public interface UserInsertResult extends Model {
  long id();     // must match a real column name+type on User; typically the AUTO_ASSIGN_ID column
}

@SQL(dialect = SqlDialect.POSTGRESQL_18)
@INSERT
@Trait
@CallGraphDelegationMode(SYNC)
public interface InsertUserReturning extends TraitDef<UserInsertResult> {
  interface _Inputs {
    @IfAbsent(FAIL)
    User user();
  }
}
```

Rules for `INSERT` traits (from source, `INSERT.java`'s own javadoc):
- `_Inputs` holds one or more `@Table`-typed inputs, all pointing at the **same** table. A `List<@Table X>` input
  inserts multiple rows in one statement.
- The trait must return `int`/`Integer` (rows inserted) or a `@ReturnOnInsert` model.

**Before writing an INSERT trait against an existing table, check how its "auto" columns are declared** (see the
insert-exclusion note in `references/annotations.md`'s `@DefaultValueStrategy` section). A column is only left out
of the generated INSERT statement if it's a Java `default` method — an `AUTO_ASSIGN_ID`/`CURRENT_TIMESTAMP` column
declared as a plain abstract method is *not* excluded, and the code you write to construct the `@Table` input for
this trait will need to supply a real value for it (verified directly against `InsertModelParser`/`InsertQueryModel`
source, not assumed). If the table isn't set up that way yet and it should be, fix the `@Table` model first (that's
a schema change — Step 2/Step 3 in SKILL.md, including a DDL update if the DB column itself needs `DEFAULT
CURRENT_TIMESTAMP`/auto-increment to actually take over once the app stops sending a value).

## `@LIMIT` — required on every list result, otherwise a compile error

If a trait's result type (or a nested join field inside a `@Selection`) is a `List<T>`, it must carry `@LIMIT(n)` —
omitting it is a compile-time error, by design, so nobody accidentally ships an unbounded query. Use
`@LIMIT(LIMIT.NO_LIMIT)` (`-1`) to explicitly opt into no limit rather than picking an arbitrary number.

For a **non-list** result (`TraitDef<SomeSelection>`, no `List`), there's a real behavioral choice:
- No `@LIMIT` at all → the generated query asserts exactly one row matched; a runtime `IllegalStateException` if the
  WHERE clause actually matches more than one row. Prefer this when the WHERE clause is provably unique (e.g.
  filtering on a `@UniqueKey` or the `@PrimaryKey`).
- `@LIMIT(1)` → takes the first row via SQL `LIMIT 1`, silently, even if multiple rows match. This can silently
  mask a WHERE clause that's less selective than you think — only use it when "just give me any one match" is
  actually the intent.

## `@ORDER` — type-use, repeatable, scoped to whatever it's attached to

`@ORDER(by = "columnName", direction = ASC|DESC)` goes on the same type-use position as `@LIMIT` — either the
trait's top-level result type, or a nested `List<...>` join field inside a `@Selection`. It's `@Repeatable`, so
stack multiple `@ORDER` annotations for a multi-column sort; they apply in the order written.

## Dialect — always set it explicitly, don't rely on the default

`@SQL(dialect = ...)` defaults to `SqlDialect.SQL_2023` if omitted. Every real sample explicitly sets
`POSTGRESQL_18` (or `MYSQL_8`) instead of relying on that default, and there's a known rough edge here: omitting an
explicit dialect on an `INSERT` trait has been observed to NPE in the current INSERT codegen rather than falling
back cleanly. Always set `dialect` to match whichever Vert.x SQL client the module actually depends on (see
`references/build-setup.md` — `vajram-sql-vertx` pulling in `vertx-mysql-client` vs. a Postgres client determines
which dialect is actually correct, not just which one compiles).

## Where things live (package convention from the samples)

The samples module organizes query-related files as:
```
model/     — @Table interfaces (see references/annotations.md) and @ReturnOnInsert result models
clause/    — @Selection projections and @WHERE/ColumnPredicate/SqlOrPredicate predicates
statement/ — the actual @SQL @SELECT/@INSERT @Trait interfaces
```
This isn't enforced by the framework, but it's a reasonable default to follow if the target repo has no existing
convention of its own (check first, per SKILL.md Step 1).

## Sanity checklist for a new query

- Every `List<T>` result (top-level or nested join field) has an explicit `@LIMIT`.
- A non-list result either has no `@LIMIT` (relying on the runtime uniqueness check) or `@LIMIT(1)` deliberately —
  pick based on whether the WHERE clause is provably unique.
- `@SQL(dialect = ...)` is set explicitly and matches the actual DB/driver the module depends on.
- Every `ColumnPredicate`/`SqlOrPredicate` interface has the `static _builder()` method so calling code can actually
  construct one.
- An `INSERT` trait's `_Inputs` only references one table type across all its inputs.
- A `@ReturnOnInsert` model's methods match real column names/types on the target table exactly.
