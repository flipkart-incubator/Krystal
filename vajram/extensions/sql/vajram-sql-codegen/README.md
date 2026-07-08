# vajram-sql-codegen

Framework-agnostic annotation model parsing and SQL query building for the Krystal SQL extension.
This module sits between the developer-facing SDK (`vajram-sql`) and driver-specific code generators
(e.g. `vajram-sql-vertx-codegen`), providing shared infrastructure that any SQL codegen backend can
reuse.

---

## Architecture

```text
vajram-sql  (annotations + marker interfaces — developer-facing)
    ↓
vajram-sql-codegen  ← this module (parsing + SQL building)
    ↓
vajram-sql-vertx-codegen  (Vert.x-specific JavaPoet code generation)
```

The module exports a single package — `com.flipkart.krystal.vajram.ext.sql.codegen` — containing
these classes:

### SELECT support

| Class                  | Responsibility                                                                     |
|------------------------|------------------------------------------------------------------------------------|
| `SqlQueryModel`        | Immutable data records that represent a parsed SQL SELECT query                    |
| `SqlModelParser`       | Reads annotations from source elements and produces `SqlQueryModel` records         |
| `SqlQueryBuilder`      | Accepts `SqlQueryModel` records and produces SQL SELECT query strings               |
| `WhereOperator`        | Interface for WHERE clause operators — `toSql` for SQL generation, `getJavaParamAccessors` for runtime parameter extraction |
| `SimpleWhereOperator`  | Implements `WhereOperator` for simple comparisons (`=`, `>`, `>=`, `<`, `<=`)      |
| `RangeWhereOperator`   | Implements `WhereOperator` for `@IsInRange` — generates dual-bound SQL with runtime-adaptive open/closed comparison |

### INSERT support

| Class                  | Responsibility                                                                     |
|------------------------|------------------------------------------------------------------------------------|
| `InsertQueryModel`     | Immutable data records that represent a parsed SQL INSERT trait                     |
| `InsertModelParser`    | Reads `@INSERT` trait inputs, validates `@Table` types, extracts columns (respects `@Column` for DB column names) |
| `InsertQueryBuilder`   | Accepts `InsertQueryModel` records and produces parameterized SQL INSERT strings    |

---

## SqlQueryModel

All data types are nested inside the `SqlQueryModel` utility class. They are plain Java `record`s
with no behaviour — purely structural.

### Core records

| Record            | Purpose                                                                              |
|-------------------|--------------------------------------------------------------------------------------|
| `ScalarColumn`    | A single scalar column in a selection (method name, DB column name, Java type, Optional flag) |
| `OrderByClause`   | A single `ORDER BY col [ASC\|DESC]` term                                              |
| `JoinRelation`    | A LEFT JOIN discovered from a `List<AnotherSelection>` method; supports nesting       |
| `SelectionInfo`   | Fully parsed `@Selection` interface: table, PK, scalars, joins                        |
| `TraitResultType` | The `T` in `TraitDef<T>` — selection element + whether the trait returns `List<T>`    |

### WHERE records

| Record        | Purpose                                                                                          |
|---------------|--------------------------------------------------------------------------------------------------|
| `WhereColumn` | A single column comparison: accessor method name, DB column name, SQL operator (e.g. `=`)        |
| `WhereLeaf`   | An AND group of `WhereColumn`s from one `ColumnPredicate`; carries the accessor prefix and table name |
| `WhereInput`  | Complete WHERE spec for one `_Inputs` method — `isOr` flag + list of `WhereLeaf`s                |

For a simple `ColumnPredicate`, `WhereInput.isOr()` is `false` and `leaves` has one entry.
For an `SqlOrPredicate`, `isOr()` is `true` and `leaves` has one entry per OR branch.

### Other records

| Record          | Purpose                                                              |
|-----------------|----------------------------------------------------------------------|
| `JoinSqlResult` | Result of `SqlQueryBuilder.buildJoinSql` — SQL string + parent PK alias |

---

## SqlModelParser

Reads `@Table`, `@Selection`, `@WHERE`, `@ForeignKey`, `@Column`, `@IsEqualTo`, `@SqlOrPredicate`,
and related annotations from `javax.lang.model` elements and builds `SqlQueryModel` records.
This class is **framework-agnostic** — it does not reference Vert.x or any specific SQL driver.

### Key public methods

| Method                          | Input                     | Output             | Description                                                                                                                                                                                                                                                                     |
|---------------------------------|---------------------------|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `collectWhereInputs`            | `VajramInfo`              | `List<WhereInput>` | Scans `_Inputs` facets for `@WHERE`-annotated or `SqlOrPredicate` types; returns parsed WHERE specs                                                                                                                                                                             |
| `parseScalarColumns`            | `TypeElement` (selection), `TypeElement` (table) | `List<ScalarColumn>` | Extracts scalar columns from a `@Selection` interface                                                                                                                                                                                                                           |
| `resolveColumnName`             | `ExecutableElement`       | `String`           | Returns `@Column.value()` or falls back to the method name                                                                                                                                                                                                                      |
| `resolveComparisonOperator`     | `ExecutableElement`       | `WhereOperator`    | Returns the SQL operator from annotations (`@IsEqualTo`→`=`, `@IsGreaterThan`→`>`, `@IsGreaterThanOrEqual`→`>=`, `@IsLessThan`→`<`, `@IsLessThanOrEqual`→`<=`, `@IsInRange`→range comparison); validates comparable type for ordering operators and `Range<T>` for `@IsInRange` |
| `getTableName`                  | `TypeElement` (table)     | `String`           | Returns `@Table(name = "...")` value                                                                                                                                                                                                                                            |
| `findPkColumn`                  | `TypeElement` (table)     | `String`           | Finds the `@PrimaryKey` column name                                                                                                                                                                                                                                             |
| `findFkColumnInChildForParent`  | child table, parent table | `String`           | Finds the `@ForeignKey(toTable = ...)` column in the child that references the parent; respects `@Column` for DB name                                                                                                                                                           |
| `hasIncomingFkForChild`         | parent table, child table | `boolean`          | Checks for `@IncomingForeignKey` on the parent side whose return type (unwrapped from `List<>`) matches the child table                                                                                                                                                         |
| `whereClauseCoversSingleRow`    | table, `List<WhereInput>` | `boolean`          | Returns `true` if the WHERE columns match a PK or unique key (OR predicates always return `false`)                                                                                                                                                                              |
| `findInvalidSelectionColumns`   | selection, table          | `List<String>`     | Lists selection methods that don't correspond to real table columns                                                                                                                                                                                                             |
| `validateTableAndWhereElements` | `RoundEnvironment`        | void               | Compile-time validation of `@Table` and `@WHERE` structural invariants                                                                                                                                                                                                          |
| `parseOrderBys`                 | `TypeMirror`              | `List<ORDER>`      | Extracts `@ORDER` annotations from a type mirror                                                                                                                                                                                                                                |

### WHERE parsing flow

```
_Inputs method (e.g. UserOrPredicate where())
    │
    ├─ type extends SqlOrPredicate?
    │   YES → for each method in the interface:
    │         └─ return type is a ColumnPredicate → parseWhereLeaf()
    │             └─ for each method in the predicate:
    │                 ├─ @Column("col") → DB column name
    │                 └─ @IsEqualTo     → operator "="
    │         → WhereInput(isOr=true, leaves=[...])
    │
    └─ type extends ColumnPredicate?
        YES → parseWhereLeaf() directly
            → WhereInput(isOr=false, leaves=[single leaf])
```

---

## SqlQueryBuilder

Stateless utility that accepts `SqlQueryModel` records and produces SQL query strings.
All methods are `public static`. Two query patterns are supported:

### `buildSimpleSql`

Builds a flat `SELECT ... FROM ... WHERE ... ORDER BY ... LIMIT ...` statement for selections
that contain only scalar columns (no `List<AnotherSelection>` joins).

```
buildSimpleSql(SelectionInfo, List<WhereInput>, List<ORDER>, int limit)
→ "SELECT id, name, email AS contactEmail FROM users WHERE id = $1 LIMIT 1"
```

### `buildJoinSql`

Builds a LEFT JOIN `SELECT` for selections that contain `List<AnotherSelection>` methods.
Returns a `JoinSqlResult` (SQL string + parent PK alias for per-row identity validation).

Features:
- **Column aliasing** — all columns are prefixed with their table name to avoid ambiguity
  (e.g. `users.id AS users_id`)
- **Per-parent-row limits** — when the trait returns `List<T>` and a join has `@LIMIT(N)`,
  a `ROW_NUMBER() OVER (PARTITION BY fk ...)` subquery enforces the limit per parent row
- **Multi-level joins** — supports parent → child → grandchild via nested `JoinRelation`s
- **Trait-level LIMIT for list traits** — wraps the parent table in a subquery
  `(SELECT * FROM parent WHERE ... LIMIT N) parent` so that LIMIT applies to parent rows,
  not to the total joined result

### WHERE clause generation (`appendWhere`)

The private `appendWhere` method handles both simple and OR predicates:

| Input structure                          | Generated SQL                                      |
|------------------------------------------|-----------------------------------------------------|
| Simple predicate: `id = $1`              | `WHERE id = $1`                                     |
| Two AND predicates: `id = $1, name = $2` | `WHERE id = $1 AND name = $2`                       |
| OR predicate with single-column branches | `WHERE (id = $1 OR name = $2)`                      |
| OR predicate with multi-column branches  | `WHERE (col1 = $1 AND col2 = $2) OR (col3 = $3)`  |
| Mixed: simple + OR                       | `WHERE simple_col = $1 AND (id = $2 OR name = $3)` |

When `qualified` is `true` (JOIN queries), column names are prefixed with the table name from
`WhereLeaf.inTableName()` (e.g. `users.id = $1`).

---

## InsertQueryModel

Data records for parsed `@SQL @INSERT @Trait` interfaces.

| Field            | Purpose                                                                                   |
|------------------|-------------------------------------------------------------------------------------------|
| `tableElement`   | The `@Table`-annotated type element                                                       |
| `tableName`      | The SQL table name from `@Table(name = ...)`                                              |
| `columns`        | List of `TableColumn` records — the table's insertable columns (auto-assigned columns are filtered out by the `columns()` accessor) |
| `inputParamName` | The method name of the single input in `_Inputs`                                          |
| `isList`         | `true` if the input type is `List<@Table>`, `false` for a single `@Table`                 |

### TableColumn

Nested inside `InsertQueryModel`.

| Field                   | Purpose                                                                 |
|-------------------------|---------------------------------------------------------------------------|
| `declaringMethod`       | The `ExecutableElement` on the Table model; `methodName()` derives the method name from it |
| `columnName`            | DB column name (from `@Column` or the method name)                      |
| `javaType`              | Java type of the column (unwrapped if `Optional`)                       |
| `isOptional`            | `true` if the column's return type is `Optional<T>`                     |
| `serdeInfo`             | Serialization info if the column's type has `@SupportedModelProtocol`, else `null` |
| `isEligibleForInsertion` | `false` for columns whose values are auto-assigned (e.g. auto-increment IDs, current timestamp) |

---

## InsertModelParser

Reads `@INSERT` trait inputs and validates their structure.

### Key public methods

| Method              | Input        | Output             | Description                                                                                    |
|---------------------|--------------|--------------------|------------------------------------------------------------------------------------------------|
| `parseInsertInputs` | `VajramInfo` | `InsertQueryModel` | Validates the trait has exactly one input of type `@Table` or `List<@Table>`; extracts columns (FK columns are plain scalars) |

### Validation rules

- The trait must have **exactly one** input
- That input must be a `@Table`-annotated type or `List<@Table>`
- `@IncomingForeignKey` methods are excluded (not real DB columns)
- `@ForeignKey` columns are treated as plain scalars, using whatever type they declare (typically the same type as the referenced `@PrimaryKey`)
- DB column names are resolved via `@Column` annotation (falling back to method name)

---

## InsertQueryBuilder

Builds parameterized INSERT SQL strings.

```
InsertQueryBuilder.buildInsertSql(model, config, syntax, returningColumnNames)
→ "INSERT INTO users (id, name, email, phoneNumber) VALUES ($1, $2, $3, $4)"
```

The `SqlDriverConfig` provides driver-specific placeholder syntax (e.g. `$1` for PostgreSQL/Vert.x).
`syntax` is a dialect-specific `SqlSyntax` (see below); when `returningColumnNames` is
non-empty, a `RETURNING` clause is appended via `syntax.returningClause(...)`.

---

## SqlSyntax

A sealed interface (`syntax` package) that encapsulates dialect-specific SQL differences needed at
code-generation time — currently the `RETURNING` clause syntax for `@ReturnOnInsert` and how column
names get transformed in dialect-specific result sets.

| Implementation      | Dialect          | `returningClause`                          |
|----------------------|------------------|---------------------------------------------|
| `Sql2023Syntax`      | `SQL_2023`       | Always returns `""` — the standard doesn't define `RETURNING`      |
| `PostgreSqlSyntax`   | `POSTGRESQL_18`  | `RETURNING col1, col2, ...`; also lowercases unquoted identifiers via `columnNameInResult` |
| `SqlLite3_35Syntax`  | `SQL_LITE_3_35`  | `RETURNING col1, col2, ...`                  |
| `MySql8Syntax`       | `MYSQL_8`        | Returns `""` for a single returning column (relies on the driver's `LAST_INSERT_ID`); throws `UnsupportedOperationException` if more than one column is requested |

Obtain the right implementation via `SqlSyntax.forDialect(SqlDialect)`.

---

## Dependencies

```groovy
dependencies {
    api project(':vajram:extensions:sql:vajram-sql')
    api project(':krystal-codegen-common')
    api project(':vajram:vajram-codegen-common')
    implementation project(':vajram:vajram-java-sdk')
    implementation 'com.squareup:javapoet'
}
```

This module depends on:
- **vajram-sql** — annotation types (`@Table`, `@Selection`, `@WHERE`, `@Column`, `@IsEqualTo`,
  `SqlOrPredicate`, `ColumnPredicate`, etc.)
- **krystal-codegen-common** — `CodeGenUtility` for annotation processing helpers
- **vajram-codegen-common** — `VajramInfo`, `VajramCodeGenUtility` for facet introspection
- **vajram-java-sdk** — facet type constants
- **javapoet** — used by `WhereOperator` implementations to build `CodeBlock`s

---

## Usage by downstream codegen modules

A driver-specific codegen module (like `vajram-sql-vertx-codegen`) typically:

1. Creates a `SqlModelParser` from its `VajramCodeGenUtility`
2. Calls `collectWhereInputs(vajramInfo)` to get WHERE specs
3. Calls `parseScalarColumns(selectionElement, tableElement)` and related methods to get the selection model
4. Passes the parsed `SelectionInfo` + `List<WhereInput>` to `SqlQueryBuilder.buildSimpleSql`
   or `buildJoinSql` to get the SQL string
5. Uses the parsed model records to generate driver-specific Java code (e.g. JavaPoet classes
   for Vert.x parameter binding and row mapping)

```java
SqlModelParser parser = new SqlModelParser(vajramUtil);

// Parse WHERE inputs
List<WhereInput> whereInputs = parser.collectWhereInputs(vajramInfo);

// Parse the selection
SelectionInfo selection = /* build from parser methods */;

// Generate SQL
String sql = SqlQueryBuilder.buildSimpleSql(selection, whereInputs, orderBys, limit);
// or
JoinSqlResult result = SqlQueryBuilder.buildJoinSql(selection, whereInputs, orderBys, limit, isList);
```
