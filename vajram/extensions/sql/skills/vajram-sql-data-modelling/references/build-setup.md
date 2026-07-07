# Wiring a Gradle module for vajram-sql

vajram-sql plugs into the same annotation-processing convention used across Krystal (`krystalModelsGenProcessor`),
seen already in Vajram-based modules for REST etc. Add both the runtime deps and the codegen processors:

```kotlin
dependencies {
    implementation(project(":vajram:extensions:sql:vajram-sql"))
    implementation(project(":vajram:extensions:sql:vertx:vajram-sql-vertx"))

    krystalModelsGenProcessor(project(":vajram:vajram-codegen"))
    krystalModelsGenProcessor(project(":vajram:extensions:sql:vertx:vajram-sql-vertx-codegen"))
}
```

If the target repo consumes Krystal as published artifacts rather than as included builds/subprojects (check
`settings.gradle.kts` and existing `build.gradle.kts` files in the repo for the pattern already in use — most
Krystal-based repos pull in `libs.lattice.*` / `libs.vajram.*` via a version catalog rather than `project(...)`),
mirror whatever form the rest of the module's Krystal dependencies already take instead of introducing a second,
inconsistent pattern. E.g. if other modules do:

```kotlin
implementation(libs.vajram.java.sdk)
krystalModelsGenProcessor(libs.vajram.codegen)
```

...then vajram-sql's deps should be added the same way (`libs.vajram.sql`, `libs.vajram.sql.vertx`,
`libs.vajram.sql.vertx.codegen`, or whatever the version catalog calls them) rather than as raw `project(...)`
references. Check `gradle/libs.versions.toml` for the actual alias names before assuming.

## Connection pool binding

The Vert.x SQL pool is injected by Krystal's DI, not passed as a `_Inputs` field — it's bound under the fixed
qualifier `vertxSql_pool`:

```java
injectionProvider.bind("vertxSql_pool", myVertxPool);
```

This is an app-startup wiring concern, not something that goes in a `@Table` model. Mention it if you're setting up
vajram-sql in a module for the first time, since the generated query vajrams won't wire up without it, but it isn't
part of schema modeling itself.

## Generated output

Codegen only produces query-execution Java classes (`build/generated/sources/.../<TraitName>_VertxSql.java`) for
`@SQL` trait interfaces — **not** for `@Table` interfaces themselves, and never any SQL/DDL files. A `@Table`
interface alone triggers no codegen; it only becomes relevant once referenced from a `@Selection`/`@WHERE`/`@SQL`
trait. Don't expect `./gradlew build` to produce anything visible right after adding a bare table model — that's
expected, not a sign something's broken.
