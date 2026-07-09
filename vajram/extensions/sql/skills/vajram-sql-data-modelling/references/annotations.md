# vajram-sql table-model annotation reference

Source of truth: `com.flipkart.krystal.vajram.ext.sql.model` and `com.flipkart.krystal.vajram.ext.sql.lang` packages in
[flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal), under `vajram/extensions/sql/vajram-sql`.

**Read this before trusting the plugin's own READMEs.** `vajram-sql-vertx-codegen/README.md` in particular still
disagrees with current source in places (see "Doc/source drift" at the bottom); `vajram-sql/README.md` and
`vajram-sql-codegen/README.md` have been reconciled with source as of this writing, but re-verify before trusting an
example blindly. Every annotation below was checked against the actual `.java` source, not the prose docs. If
something looks off, `grep`/fetch the real annotation source rather than trusting a README example.

## The shape of a table model

A table is a plain Java interface, not a class:

```java
@ModelRoot
@Table(name = "UserEntity")          // name optional — defaults to the interface's simple name
public interface User extends TableModel {
  @PrimaryKey
  long id();

  String name();
  // ...
}
```

Two structural rules are enforced at compile time by the annotation processor:
- The interface must carry **both** `@ModelRoot` and `@Table` — one without the other is a compile error.
- It must extend `TableModel` (`com.flipkart.krystal.vajram.ext.sql.model.TableModel`).
- Exactly **one** method must carry `@PrimaryKey` — zero or more than one is a compile error.

Every abstract method on the interface is one column. The column name is the method name unless overridden with
`@Column("dbName")`. The column's SQL type is inferred from the Java return type by the Vert.x codegen (`long` →
`getLong`, `String` → `getString`, etc.) — there is no explicit `@ColumnType` annotation; whatever Java type you
declare is what gets bound/read via the corresponding Vert.x `Row` accessor.

## `@Table`

```java
@Target(TYPE)
public @interface Table {
  String name() default "";          // physical table name; empty = use interface's simple name
  UniqueKey[] uniqueKeys() default {}; // composite unique constraints spanning multiple columns
}
```

Use the type-level `uniqueKeys()` array (not the method-level `@UniqueKey`, see below) when a unique constraint
spans more than one column.

## `@Column("dbName")`

Overrides the derived column name for a method — use when the DB column name can't be a legal Java identifier, or
when you want the Java accessor name to diverge from the physical column (e.g. `orderId()` mapping to a `fk_order_id`
column). Also usable at the type level for aliasing in `@Selection`/`@WHERE` interfaces (out of scope for this
skill — that's query modeling, not schema modeling).

**Don't add `@Column` on a `@Table`/`@Selection` method whose name already matches the column name.** The column
name defaults to the method name, so restating it with `@Column("sameName")` is redundant noise — reserve the
annotation for the actual divergence case above. (`@WHERE` predicate methods are the one place this default doesn't
help — see `references/queries.md`, since predicate method names are conventionally named for the operator, not the
column, so `@Column` is doing real work there on every method, not just the exceptions.)

## `@PrimaryKey`

Method-level, no attributes. Marks the single PK column. There must be exactly one per table.

## `@UniqueKey`

Method-level for a single-column unique constraint:

```java
@UniqueKey
String email();
```

For a **composite** unique key spanning multiple columns, don't repeat `@UniqueKey` on each method — use the
type-level `@Table(uniqueKeys = {...})` form instead, referencing the participating column names.

## `@ForeignKey` / `@IncomingForeignKey` — must be added in pairs

```java
public @interface ForeignKey {
  Class<? extends TableModel> toTable();   // required — the parent table's model interface
  String[] toColumns() default {};         // optional — explicit column mapping if not the parent's PK
}
```

`@ForeignKey` goes on the **child** table's method pointing at the parent:

```java
public interface Order extends TableModel {
  @PrimaryKey
  long id();

  @ForeignKey(toTable = User.class)
  @Column("userId")
  long userId();
}
```

`@IncomingForeignKey` goes on the **parent** table's method, and is the reverse side — it is purely a modeling
construct for join codegen, **it does not correspond to a real database column**:

```java
public interface User extends TableModel {
  // ...
  @IncomingForeignKey
  List<Order> orders();
}
```

The annotation processor requires **both sides** to be present for the relationship to be usable in joined
selections — a `@ForeignKey` with no matching `@IncomingForeignKey` on the parent (or vice versa) is a compile-time
error. When you add a FK, always add its `@IncomingForeignKey` counterpart on the other table in the same change.

The child's `@ForeignKey` field type must match the parent's `@PrimaryKey` return type (or, if `toColumns()` is set,
whatever those columns' types are) — the processor checks this at compile time.

## Nullability: `Optional<T>` (or `@Nullable T`)

Columns that can be SQL `NULL` should be typed as `Optional<T>`:

```java
Optional<String> phoneNumber();
```

The checker-framework `@Nullable` annotation on the return type is an accepted alternative you'll see in some
samples. Either way — **this is a runtime distinction, not a compile-time one.** If you declare a column as a plain
non-nullable type but the underlying value is actually `NULL`, it compiles fine and throws a `NullPointerException`
at query time. Get nullability right the first time; the compiler won't catch a mismatch for you.

## Defaults: `@DefaultValueStrategy` / `@DefaultValue`

```java
public @interface DefaultValueStrategy {
  ValueComputation value();                    // required
  Trigger trigger() default Trigger.ON_INSERT; // ON_INSERT | ON_UPDATE
}
```

`ValueComputation` has three values:
- `AUTO_ASSIGN_ID` — DB-assigned (e.g. auto-increment/serial). Applicable to `int`-typed columns.
- `CURRENT_TIMESTAMP` — for timestamp columns like `createdAt`/`updatedAt`. Applicable to `java.time.Instant`
  columns only (sets the value to the current UTC timestamp); combine with `trigger = Trigger.ON_UPDATE` for an
  "updated at" column that refreshes on every update.
- `CUSTOM_STATIC_VALUE` — pair with `@DefaultValue("literal")` for a fixed default (e.g. `@DefaultValue("false")` on
  a boolean flag column).

**Getting excluded from generated INSERTs is about the method shape, not the `ValueComputation` value — this is
easy to get wrong.** Verified directly against `InsertModelParser`/`InsertQueryModel` in `vajram-sql-codegen`: a
column is only left out of the generated `INSERT INTO ... (...)` column list if its accessor is declared as a Java
`default` method (with no `@IfAbsent(ASSUME_DEFAULT_VALUE)` override) — the codegen doesn't actually branch on
`AUTO_ASSIGN_ID` vs. `CURRENT_TIMESTAMP` vs. anything else. The samples' `default int internalId() { throw ... }`
idiom happens to work for `AUTO_ASSIGN_ID` for exactly this reason. **If you declare a `CURRENT_TIMESTAMP` column as
a plain abstract method** (as it's easy to assume, since "defaults to now" sounds DB-managed), **it will NOT be
excluded from the generated INSERT** — the caller still has to supply a real value, and only `Objects.requireNonNullElse`-style substitution via `@DefaultValue` (for `CUSTOM_STATIC_VALUE`) gets any help at insert time. To
actually get "auto-populated at insert, caller doesn't supply it" behavior for a `CURRENT_TIMESTAMP` column, give it
the same `default`-method-with-throwing-body treatment as an `AUTO_ASSIGN_ID` column:

```java
@DefaultValueStrategy(AUTO_ASSIGN_ID)
@UniqueKey
default int internalId() {
  throw new UnsupportedOperationException("'internalId' is auto-assigned and cannot be inserted via this model.");
}

@DefaultValueStrategy(CURRENT_TIMESTAMP)
default Instant publishedAt() {
  throw new UnsupportedOperationException("'publishedAt' is DB-assigned and cannot be set when inserting.");
}
```

If a `CURRENT_TIMESTAMP`/`AUTO_ASSIGN_ID` column is declared as a plain abstract method instead, it isn't wrong for
*schema modeling* purposes (the annotation still documents intent, and `references/queries.md`'s Step for writing
the INSERT trait is where this actually bites) — but flag it, since whoever writes the INSERT trait against this
table will otherwise be surprised that the "auto" column isn't actually optional at insert time.

**MySQL-specific gotcha:** an `AUTO_ASSIGN_ID` column must be typed `int` — the MySQL codegen path
(`MySqlCodeGenerator`/`SqlInsertVajramGen`) specifically checks for `TypeKind.INT`. Using `long` (or any other type)
on an `AUTO_ASSIGN_ID` column will silently fail to wire up correctly rather than producing a clear compile error.
The "only one `AUTO_ASSIGN_ID` column per table" constraint some older docs mention isn't actually enforced anywhere
in the codegen — it's just a MySQL/`LAST_INSERT_ID` practical limitation, not a checked invariant.

## Nested/JSON columns: `@SerdeWith(Json.class)` + `@JsonConfig`

For a column that stores a serialized nested model (or a list of them) rather than a native SQL type:

```java
@SerdeWith(Json.class)
@JsonConfig(serializeAs = STRING)
Address address();

@SerdeWith(Json.class)
@JsonConfig(serializeAs = STRING)
List<Address> secondaryAddresses();
```

`Address` here is itself a `Model` (a Krystal model, not a `@Table`). This maps to a `TEXT`/`VARCHAR` column
containing JSON — vajram-sql serializes/deserializes it for you, but the physical column is just text as far as the
DB is concerned.

## What schema modeling in vajram-sql does *not* do

This is the single most important thing to get right, and it's easy to miss because most ORMs work differently:

**vajram-sql generates zero DDL.** The `@Table` interface is a compile-time query contract, not a source of truth
for the physical schema. There is no migration system, no schema versioning, no "apply schema" task. The plugin
assumes a physical table already matching the model exists, and if it doesn't, you get a runtime failure when a
generated query runs — never a compile-time or "migration" error. Confirmed directly in the plugin's own integration
test, which stands up the schema with a hand-written raw `CREATE TABLE`:

```sql
CREATE TABLE UserEntity (
  internalId SERIAL UNIQUE,
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  phoneNumber VARCHAR(255),
  address TEXT,
  secondaryAddresses TEXT
)
```

**Implication for this skill:** whenever you define or change a `@Table` interface, also produce/update the matching
raw SQL (`CREATE TABLE` for new tables, `ALTER TABLE` for changes) so the physical schema doesn't drift silently out
of sync with the Java model. See the main SKILL.md for where that DDL should live.

## Build wiring

Codegen is an annotation processor, following the same `krystalModelsGenProcessor` convention as other Krystal
plugins. See `references/build-setup.md`.

## Out of scope for this skill

`@Selection`, `@WHERE`/`ColumnPredicate`, `@SQL` traits, `@LIMIT`, `@ORDER` — these define **queries** against a
table model, not the table's schema. They're a separate concern (query modeling) even though they live in the same
plugin. Don't reach for them when the task is "model/update a table" — only when asked to define an actual query.

## Doc/source drift — known discrepancies

If you consult the plugin's own READMEs directly, watch for these (verified against source, not prose):
- The vertx-codegen README (`vajram-sql-vertx-codegen/README.md`) still uses stale `@Projection`/`@ORDER_BY`
  naming; the real, current annotations are `@Selection` and `@ORDER`. Unlike the other two READMEs, this one has
  not yet been corrected.
- `ReturnOnInsert` lives in package `lang`, not `model`.

None of these affect table-schema modeling directly (they're all query-side), but they're a good reminder to check
source over prose anywhere in this plugin.

## Full worked example: User → Order → OrderItem

Three tables, showing every annotation above in one coherent schema, plus the hand-written DDL that must exist
alongside them.

```java
public interface User extends TableModel {

  @DefaultValueStrategy(AUTO_ASSIGN_ID)
  @UniqueKey
  default int internalId() {
    throw new UnsupportedOperationException(
        "'internalId' value is auto-assigned and cannot be inserted via this model.");
  }

  @PrimaryKey
  long id();

  String name();

  @UniqueKey
  String email();

  Optional<String> phoneNumber();

  @SerdeWith(Json.class)
  @JsonConfig(serializeAs = STRING)
  Address address();

  @SerdeWith(Json.class)
  @JsonConfig(serializeAs = STRING)
  List<Address> secondaryAddresses();

  @IncomingForeignKey
  List<Order> orders();
}

public interface Order extends TableModel {
  @PrimaryKey
  long id();

  @ForeignKey(toTable = User.class)
  @Column("userId")
  long userId();

  @IncomingForeignKey
  List<OrderItem> items();
}

public interface OrderItem extends TableModel {
  @PrimaryKey
  long id();

  @ForeignKey(toTable = Order.class)
  @Column("orderId")
  long orderId();

  String sku();
  int quantity();
}
```

Matching DDL (Postgres flavor — adapt types per dialect):

```sql
CREATE TABLE UserEntity (
  internalId SERIAL UNIQUE,
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  phoneNumber VARCHAR(255),
  address TEXT,
  secondaryAddresses TEXT
);

CREATE TABLE OrderEntity (
  id BIGINT PRIMARY KEY,
  userId BIGINT NOT NULL REFERENCES UserEntity(id)
);

CREATE TABLE OrderItemEntity (
  id BIGINT PRIMARY KEY,
  orderId BIGINT NOT NULL REFERENCES OrderEntity(id),
  sku VARCHAR(255) NOT NULL,
  quantity INT NOT NULL
);
```

Notice the DDL has no column for `User.orders()` or `Order.items()` — `@IncomingForeignKey` is a Java-side modeling
construct only, never a physical column.
