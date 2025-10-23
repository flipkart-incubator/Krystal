# SQL Trait Quick Start Guide

Get started with Krystal SQL traits in 5 minutes!

## Step 1: Define Your Entity

```java
public record User(int id, String name, String email) {}
```

## Step 2: Create a SQL Trait

```java
@Sql("SELECT id, name, email FROM users WHERE id = ?")
public interface GetUserByIdTrait extends TraitRoot<User> {
  class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
```

## Step 3: Build to Generate Code

```bash
./gradlew :vajram:vajram-samples:compileJava
```

This automatically generates:
- `GetUserByIdTrait_sql.java` - The vajram implementation
- `GetUserByIdTrait_sql_Req.java` - Request interface
- `GetUserByIdTrait_sql_ReqImmutPojo.java` - Request implementation

## Step 4: Implement RowMapper

```java
public class UserRowMapper implements RowMapper {
  @Override
  public <T> Mono<List<T>> map(Result result, Class<T> type) {
    return Flux.from(result.map((row, metadata) -> 
      new User(
        row.get("id", Integer.class),
        row.get("name", String.class),
        row.get("email", String.class)
      )
    ))
    .collectList()
    .map(list -> (List<T>) list);
  }
}
```

## Step 5: Configure Dependencies

```java
class DatabaseModule extends AbstractModule {
  @Provides
  @Singleton
  public ConnectionFactory provideConnectionFactory() {
    return MySqlConnectionFactory.from(
      MySqlConnectionConfiguration.builder()
        .host("localhost")
        .username("root")
        .database("mydb")
        .build()
    );
  }
  
  @Provides
  @Singleton
  public RowMapper provideRowMapper() {
    return new UserRowMapper();
  }
}
```

## Step 6: Execute the Vajram

```java
// Create vajram graph
VajramKryonGraph graph = VajramKryonGraph.builder()
    .loadFromPackage(GetUserByIdTrait_sql.class.getPackageName())
    .loadFromPackage(SQLRead.class.getPackageName())
    .build();

// Register dependency injection
Injector injector = createInjector(new DatabaseModule());
graph.registerInputInjector(new VajramGuiceInputInjector(injector));

// Create executor
KrystexVajramExecutorConfig config = KrystexVajramExecutorConfig.builder()
    .kryonExecutorConfigBuilder(
        KryonExecutorConfig.builder()
            .executorId("myExecutor")
            .executorService(executorService))
    .build();

// Execute!
try (KrystexVajramExecutor executor = graph.createExecutor(config)) {
    CompletableFuture<List<User>> future = executor.execute(
        GetUserByIdTrait_sql_ReqImmutPojo._builder()
            .query("SELECT id, name, email FROM users WHERE id = ?")
            .parameters(List.of(1))
            ._build(),
        KryonExecutionConfig.builder()
            .executionId("get-user-1")
            .build()
    );
    
    List<User> users = future.join();
    System.out.println("Found users: " + users);
}
```

## That's It! 🎉

You now have a fully functional, type-safe, database-backed vajram with:
- ✅ Automatic code generation
- ✅ Dependency injection
- ✅ Parallel execution support
- ✅ Type safety
- ✅ Minimal boilerplate

## Next Steps

- **Add more queries**: Create additional traits for different queries
- **Use parameterized queries**: Add multiple `?` placeholders and parameters
- **Compose vajrams**: Use SQL vajrams as dependencies in other vajrams
- **Add error handling**: Implement proper exception handling in your RowMapper
- **Performance tuning**: Configure connection pools and executor services

## Common Patterns

### Query with Multiple Parameters

```java
@Sql("SELECT * FROM users WHERE status = ? AND created_at > ?")
public interface GetActiveUsersTrait extends TraitRoot<User> {
  class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters; // List.of("active", timestamp)
  }
}
```

### Query Without Parameters

```java
@Sql("SELECT * FROM users")
public interface GetAllUsersTrait extends TraitRoot<User> {
  class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters; // Collections.emptyList()
  }
}
```

### Complex Join Query

```java
@Sql("""
    SELECT u.id, u.name, u.email, p.phone
    FROM users u
    LEFT JOIN phones p ON u.id = p.user_id
    WHERE u.id = ?
    """)
public interface GetUserWithPhoneTrait extends TraitRoot<UserProfile> {
  class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
```

## Troubleshooting

### Code Not Generated?

1. Clean build: `./gradlew clean`
2. Check annotation processor is configured in `build.gradle`
3. Verify trait extends `TraitRoot<T>`
4. Ensure `_Inputs` class has `parameters` field

### Compilation Errors?

1. Check module-info.java includes SQL extension modules
2. Verify R2DBC dependencies are added
3. Ensure module mappings are configured in root build.gradle

### Runtime Errors?

1. Verify ConnectionFactory is provided via Guice
2. Check RowMapper is implemented correctly
3. Ensure vajram packages are loaded in graph
4. Verify query syntax is correct for your database

## Learn More

- [Full README](./README.md) - Detailed documentation
- [Test Examples](../../../../../../test/java/com/flipkart/krystal/vajram/samples/sql/) - Working test cases
- [SQL Codegen](../../../../../../../vajram/extensions/sql/vajram-sql-codegen/README.md) - Code generator details
