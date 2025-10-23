# SQL Trait Example

This example demonstrates how to use Krystal's SQL trait code generation feature to create database-backed vajrams with minimal boilerplate.

## Overview

The SQL trait code generator automatically creates vajram implementations from simple trait interfaces annotated with `@Sql`. This provides:

- **Type-safe database queries** with compile-time validation
- **Automatic dependency management** on the `SQLRead` vajram
- **Integration with Krystex** execution framework
- **Custom result mapping** via `RowMapper`

## Files

### Core Files

- **`User.java`** - Simple record representing a database entity
- **`GetUserByIdTrait.java`** - SQL trait for fetching users by ID
- **`GetAllUsersTrait.java`** - SQL trait for fetching all users
- **`UserRowMapper.java`** - Custom RowMapper for mapping R2DBC results to User objects

### Generated Files (after compilation)

- **`GetUserByIdTrait_sql.java`** - Generated vajram implementation
- **`GetUserByIdTrait_sql_Req.java`** - Generated request interface
- **`GetUserByIdTrait_sql_ReqImmutPojo.java`** - Generated immutable request implementation
- **`GetUserByIdTrait_sql_Fac.java`** - Generated facets helper

## How It Works

### 1. Define a SQL Trait

```java
@Sql("SELECT id, name, email FROM users WHERE id = ?")
public interface GetUserByIdTrait extends TraitRoot<User> {
  class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
```

**Key Components:**
- `@Sql` annotation contains the SQL query
- Extends `TraitRoot<User>` where `User` is the entity type
- `_Inputs` class defines query parameters

### 2. Code Generation

During compilation, the annotation processor:

1. Detects the `@Sql` annotation
2. Validates trait structure (interface, `_Inputs` with `parameters` field)
3. Extracts the SQL query and entity type
4. Generates a complete vajram implementation

### 3. Generated Vajram Structure

```java
@Vajram
@InvocableOutsideGraph
public abstract class GetUserByIdTrait_sql extends IOVajramDef<List<User>> {
  
  static class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters;
    
    @IfAbsent(FAIL)
    String query;
  }
  
  static class _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = SQLRead.class)
    Result queryResult;
    
    @Inject
    RowMapper resultMapper;
  }
  
  // Input resolvers and output methods...
}
```

### 4. Usage in Code

```java
// Create request
GetUserByIdTrait_sql_Req request = GetUserByIdTrait_sql_ReqImmutPojo._builder()
    .query("SELECT id, name, email FROM users WHERE id = ?")
    .parameters(List.of(1))
    ._build();

// Execute
CompletableFuture<List<User>> future = executor.execute(request, config);
List<User> users = future.join();
```

## Building and Running

### Compile and Generate Code

```bash
# From the project root
./gradlew :vajram:vajram-samples:clean :vajram:vajram-samples:compileJava

# View generated files
find vajram/vajram-samples/build/generated -name "*_sql.java"
```

### Run Tests

```bash
./gradlew :vajram:vajram-samples:test --tests "*GetUserByIdSqlTest"
```

## Dependencies

The SQL extension requires:

```gradle
dependencies {
    implementation project(':vajram:extensions:sql:vajram-sql')
    annotationProcessor project(':vajram:extensions:sql:vajram-sql-codegen')
    
    // R2DBC for database connectivity
    implementation 'io.r2dbc:r2dbc-spi:1.0.0.RELEASE'
    implementation 'io.asyncer:r2dbc-mysql:1.4.1' // or another R2DBC driver
}
```

## Configuration

### Provide Dependencies via Guice

```java
class DatabaseModule extends AbstractModule {
    @Provides
    @Singleton
    public ConnectionFactory provideConnectionFactory() {
        MySqlConnectionConfiguration config = MySqlConnectionConfiguration.builder()
            .host("localhost")
            .port(3306)
            .username("root")
            .database("mydb")
            .build();
        return MySqlConnectionFactory.from(config);
    }
    
    @Provides
    @Singleton
    public RowMapper provideRowMapper() {
        return new UserRowMapper();
    }
}
```

### Register with Vajram Graph

```java
VajramKryonGraph graph = VajramKryonGraph.builder()
    .loadFromPackage(GetUserByIdTrait_sql.class.getPackageName())
    .loadFromPackage(SQLRead.class.getPackageName())
    .build();

Injector injector = createInjector(new DatabaseModule());
graph.registerInputInjector(new VajramGuiceInputInjector(injector));
```

## Advanced Features

### Custom RowMapper

Implement `RowMapper` for custom result mapping:

```java
public class UserRowMapper implements RowMapper {
    @Override
    public <T> Mono<List<T>> map(Result result, Class<T> type) {
        return Flux.from(result.map((row, metadata) -> {
            int id = row.get("id", Integer.class);
            String name = row.get("name", String.class);
            String email = row.get("email", String.class);
            return new User(id, name, email);
        }))
        .collectList()
        .map(list -> (List<T>) list);
    }
}
```

### Parameterized Queries

Use `?` placeholders in SQL and provide values in `parameters`:

```java
@Sql("SELECT * FROM users WHERE status = ? AND created_at > ?")
public interface GetActiveUsersTrait extends TraitRoot<User> {
  class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters; // List.of("active", timestamp)
  }
}
```

## Benefits

1. **Minimal Boilerplate** - Write only the trait interface, get full vajram implementation
2. **Type Safety** - Compile-time validation of trait structure
3. **Krystex Integration** - Automatic dependency resolution and parallel execution
4. **Testability** - Easy to mock database connections for unit tests
5. **Maintainability** - SQL queries are visible in the trait definition

## See Also

- [SQL Codegen README](../../../../../../../vajram/extensions/sql/vajram-sql-codegen/README.md) - Detailed codegen documentation
- [Vajram Documentation](../../../../../../../docs) - Core Vajram concepts
- [R2DBC Documentation](https://r2dbc.io/) - Reactive database connectivity
