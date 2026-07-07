---
name: vajram-sql-data-modelling
description: Models SQL table schemas AND writes SELECT/INSERT queries using the vajram-sql plugin of the Krystal framework (flipkart-incubator/Krystal). Covers @Table/@ModelRoot model interfaces (columns, @PrimaryKey, @UniqueKey, @ForeignKey/@IncomingForeignKey, nullability, defaults, JSON columns, matching raw DDL) and query traits — @Selection projections, @WHERE/ColumnPredicate/SqlOrPredicate filters, @SQL @SELECT/@INSERT @Trait definitions with @LIMIT/@ORDER/@ReturnOnInsert. Use whenever the user wants to define/change a SQL table, or fetch/insert rows, look up a record, add a query, or write a repository-style method against a Krystal/Vajram data layer — even if they just say "add a table for X" or "write a query to fetch Z" without naming vajram-sql, as long as the repo uses Krystal/Vajram. Not for generic (non-Krystal) DB work, or UPDATE/DELETE (no codegen support yet).
---

# Modeling SQL schemas and queries with vajram-sql

vajram-sql is a compile-time query-contract codegen layer inside the Krystal/Vajram framework, not a JPA-style ORM.
A table is declared once as a plain Java interface (`@Table`), and everything else — SELECT/INSERT queries — is
built as a **Krystal `Trait`** checked against that declaration. This skill covers both halves: defining/evolving
`@Table` model interfaces (Steps 1-5) and writing queries against them (Steps 6-8).

The one thing to internalize before touching any schema code: **vajram-sql never generates DDL.** The `@Table`
interface and the physical database table are two independent things that you are responsible for keeping in sync
by hand. Every schema step below reflects that — whenever the Java model changes, the matching SQL changes with it,
in the same edit. Queries don't have this problem — they compile down to a generated Vajram that executes real SQL,
so a query that compiles is already checked against the table shape.

## Step 1 — Find how the repo already does this

Before writing anything, check for precedent:
- Search the repo for existing `@Table`/`@ModelRoot` interfaces (any module) to match its column-naming, package
  layout (`.../model/<Table>.java` is the common convention seen in Krystal samples), and default-value idioms.
- Search for where raw schema DDL already lives — a `schema.sql`, a `migrations/` directory, a Flyway/Liquibase
  setup, or (least ideally) DDL only inside test setup. vajram-sql has no opinion here; the surrounding repo's
  existing convention wins. If there's no existing convention and none is obvious, ask the user where the DDL for
  this table should live rather than inventing a new location.
- Check `gradle/libs.versions.toml` and a sibling module's `build.gradle.kts` for how Krystal/vajram deps are
  referenced (version-catalog aliases vs. `project(...)`) — see `references/build-setup.md` for why this matters.

## Step 2 — Define or update the `@Table` interface

Read `references/annotations.md` for the full, source-verified annotation reference (it also documents where the
plugin's own READMEs disagree with the actual source — trust that file over the upstream docs). In short:

- New table → new interface, `@ModelRoot` + `@Table(name = "...")`, extends `TableModel`, exactly one
  `@PrimaryKey` method, one method per column.
- New column → new abstract method on the interface. Nullable columns are `Optional<T>`. Pick the narrowest
  correct Java type since it drives how the value gets bound/read at query time — get this right the first time,
  since a nullable/type mismatch fails at runtime, not compile time.
- Constraint or relationship → the matching annotation from `references/annotations.md`. Foreign keys are the one
  place that touches **two** files: adding `@ForeignKey` to the child table also requires adding
  `@IncomingForeignKey` to the parent, in the same change — the codegen will reject one without the other, but only
  once something actually references the relationship in a query.
- Renaming a column in the DB without renaming the Java accessor → use `@Column("dbName")` rather than renaming the
  method, if the desired Java name and DB name genuinely need to differ; otherwise just rename the method plus the
  DDL together.
- Auto-incrementing/DB-assigned columns and timestamp defaults → `@DefaultValueStrategy` (see reference for the
  `AUTO_ASSIGN_ID` / `CURRENT_TIMESTAMP` / `CUSTOM_STATIC_VALUE` options and the MySQL-only-one-auto-id-column
  caveat).
- A column holding a nested object or list of objects → `@SerdeWith(Json.class)` + `@JsonConfig(serializeAs =
  STRING)`, with the nested type itself being a plain Krystal `Model`.

## Step 3 — Write or update the matching raw SQL in the same change

Since there's no DDL codegen, treat this as non-optional, not an afterthought:

- **New table**: write the `CREATE TABLE` statement column-for-column matching the interface — same names (post
  `@Column` overrides), same nullability (`NOT NULL` wherever the Java type isn't `Optional`/`@Nullable`), same PK/
  unique/FK constraints.
- **Adding a column**: `ALTER TABLE ... ADD COLUMN ...`, matching nullability and any default.
- **Adding a constraint or FK**: `ALTER TABLE ... ADD CONSTRAINT ...` alongside the new annotation(s).
- **Renaming/dropping**: the corresponding `ALTER TABLE ... RENAME COLUMN` / `DROP COLUMN`, checking first whether
  the column participates in any existing FK/unique constraint elsewhere that also needs updating.
- Put this SQL wherever Step 1 found the repo already keeps its schema DDL. If the repo has a real migration tool
  (Flyway/Liquibase/etc.), add it as a new migration rather than editing a prior one in place — vajram-sql has no
  opinion on this, but the migration tool almost certainly does.

## Step 4 — Build wiring (new module/table only)

If this is the first table model in a module, it needs the vajram-sql dependencies and annotation processors wired
into `build.gradle.kts` — see `references/build-setup.md` for the exact dependency set and how to match the repo's
existing dependency-declaration style (version catalog vs. `project(...)`). If the module already has other
`@Table` interfaces, this is already done — skip it.

Note that a bare `@Table` interface with no `@Selection`/`@WHERE`/`@SQL` trait referencing it triggers no codegen at
all. Don't be surprised that `./gradlew build` produces nothing new immediately after adding just the model —
that's expected, not a sign the wiring is broken.

## Step 5 — Sanity-check before calling it done

- Exactly one `@PrimaryKey` on the interface.
- Every `@ForeignKey` has a matching `@IncomingForeignKey` on the other side.
- Every nullable-in-the-DDL column is `Optional<T>` (or `@Nullable`) in Java, and vice versa — a mismatch here is
  silent until a query actually hits the null case.
- The DDL you wrote/updated in Step 3 actually matches the interface, column by column.
- If you touched an existing table, check for other `@Table`/`@Selection` interfaces elsewhere in the repo that
  reference it (via `@ForeignKey(toTable = ...)` or `@Selection(from = ...)`) and might need a matching update.

For the fully worked example this is all drawn from (a `User`/`Order`/`OrderItem` schema with every annotation in
play, plus its hand-written DDL), see the end of `references/annotations.md`.

If the task is only about the schema (no query being asked for), stop here — Steps 6-8 below are for writing
SELECT/INSERT queries against a table that's already modeled.

## Step 6 — Find how the repo already organizes queries

Same instinct as Step 1, applied to queries: check for an existing `clause/`/`statement/` split (the convention
used in the plugin's own samples — projections and predicates in `clause/`, the actual `@Trait` interfaces in
`statement/`) or whatever pattern the repo already has for its query-side Vajram traits, and match it rather than
inventing a new one.

## Step 7 — Write the query

Read `references/queries.md` for the full, source-verified reference (projections, predicates, operators, and
worked SELECT/INSERT examples pulled directly from the plugin's real samples module). The shape of the work:

- **What columns come back** → a `@Selection(from = Table.class)` projection interface, one method per column/
  alias. A method returning `List<OtherSelection>` (or a single one) becomes a join, riding on the `@ForeignKey`/
  `@IncomingForeignKey` pair already declared on the tables (Step 2) — if that pair doesn't exist yet on the tables
  involved, add it first.
- **Which rows** (SELECT only) → a `ColumnPredicate` (`@WHERE(inTable = Table.class)`) with one comparison-operator
  annotation per filtered column (`@IsEqualTo`, `@IsGreaterThan`, `@IsInRange`, etc.), AND'ed if there are several.
  Wrap multiple predicates in a `SqlOrPredicate` for OR. Give every predicate interface the `static _builder()`
  method the samples use — calling code needs it to actually construct one.
- **The trait itself** → `@SQL(dialect = ...)` + `@SELECT` or `@INSERT` + `@Trait`, extending `TraitDef<ResultType>`.
  For SELECT, `_Inputs` holds the predicate (`where()`); for INSERT, `_Inputs` holds one or more same-table
  `@Table`-typed values to insert, and the result type is either `Integer` (rows inserted) or a `@ReturnOnInsert`
  projection if you need generated columns (like an auto-assigned id) back.
- **Always set `@SQL(dialect = ...)` explicitly** to match whichever Vert.x SQL client the module actually depends
  on — don't rely on the `SQL_2023` default, especially for INSERT traits (see the dialect gotcha in
  `references/queries.md`).
- **`@LIMIT` is mandatory on every `List<T>` result** (top-level or a nested join field) — it's a compile error
  otherwise, by design. For a non-list result, choose deliberately between no `@LIMIT` (asserts exactly one row,
  runtime error if that's violated — use when the WHERE clause is provably unique) and `@LIMIT(1)` (silently takes
  the first row — use only when "any one match" is genuinely the intent).

## Step 8 — Sanity-check the query before calling it done

- Every `List<T>` result has an explicit `@LIMIT`; every non-list result has a deliberate choice between no
  `@LIMIT` and `@LIMIT(1)`, not just whichever one happened to compile.
- `@SQL(dialect = ...)` is set and matches the module's actual DB driver dependency.
- A join field's underlying tables actually have the `@ForeignKey`/`@IncomingForeignKey` pair — add it to the
  `@Table` models first if not (this loops back to Step 2).
- An INSERT trait's `_Inputs` only references one table type across all its inputs.
- A `@ReturnOnInsert` model's methods match real column names/types on the target table exactly.
