# Krystal SQL Extension

The Krystal SQL Extension provides declarative SQL database operations through annotations and automatic code generation. It enables type-safe, reactive database access using R2DBC with minimal boilerplate.

## Table of Contents

- [Setup](#setup)
- [Core Concepts](#core-concepts)
- [Row Mapping](#row-mapping)
- [Examples](#examples)

---
## Setup

### 1. Add Dependencies

**In your `build.gradle`:**

```gradle
dependencies {
    // SQL extension runtime
    implementation project(':vajram:extensions:sql:vajram-sql')
    
    // R2DBC driver (choose based on your database)
    implementation 'io.asyncer:r2dbc-mysql:1.4.1'  // For MySQL
    // OR
    // implementation 'io.r2dbc:r2dbc-postgresql:1.0.2.RELEASE'  // For PostgreSQL
    
    // Code generation (annotation processor)
    annotationProcessor project(':vajram:extensions:sql:vajram-sql-codegen')
}
```

### 2. Configure Connection Pool

Create a Guice module to provide the `ConnectionPool`:

```java
public class DatabaseModule extends AbstractModule {
    
    @Provides
    @Singleton
    public ConnectionPool provideConnectionPool() {
        MySqlConnectionConfiguration configuration = 
            MySqlConnectionConfiguration.builder()
                .host("localhost")
                .port(3306)
                .username("your_username")
                .password("your_password")
                .database("your_database")
                .build();
        
        ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);
        
        return new ConnectionPool(
            ConnectionPoolConfiguration.builder(connectionFactory)
                .initialSize(5)
                .maxSize(20)
                .build());
    }
    
    @Provides
    @Singleton
    public RowMapper provideRowMapper() {
        return new DefaultRowMapper();
    }
}
```

### 3. Define Your Entity

**Option 1: Using a Record (Java 16+)**

```java
public record User(Integer id, String name, String emailId) {}
```

**Option 2: Using a Class**

```java
@Getter
@Setter
public class User {
    private Integer id;
    private String name;
    private String emailId;
    
    public User() {}
}
```

### 4. Create a SQL Trait

```java
@Trait
@SqlQuery("SELECT id, name, email_id FROM user_profile WHERE id = ?")
public interface GetUserByIdTrait extends TraitRoot<User> {
    
    class _Inputs {
        @IfAbsent(FAIL)
        List<Object> parameters;
    }
}
```

### 5. Use the Trait in a Vajram

```java
@Vajram
public abstract class GetUserById extends ComputeVajramDef<User> {
  ...
    
    static class _InternalFacets {
        @Dependency(onVajram = GetUserByIdTrait.class)
        User user;
    }
  ....  
}
```

---

## Core Concepts

### SQL Traits

SQL Traits are interfaces that declare SQL operations using annotations:

- **`@SqlQuery`**: For SELECT operations (returns data)
- **`@SqlUpdate`**: For INSERT, UPDATE, DELETE operations (returns affected row count)

**Key Requirements:**
1. Must be annotated with `@Trait`
2. Must extend `TraitRoot<T>` where `T` is the return type
3. Must have a `_Inputs` inner class with a `parameters` field of type `List<Object>`

### Return Types

**For `@SqlQuery`:**
- `TraitRoot<List<T>>` - Returns multiple rows as a list
- `TraitRoot<T>` - Returns a single row (or null if not found)
- `T` can be: Custom POJO, primitive wrapper (String, Long, Integer, etc.)

**For `@SqlUpdate`:**
- `TraitRoot<Long>` - Returns the number of affected rows

---

## Row Mapping

### Automatic Field Mapping

The `DefaultRowMapper` automatically maps database columns to Java fields:

- **Column naming**: `snake_case` (e.g., `email_id`) → `camelCase` (e.g., `emailId`)
- **Type conversion**: Automatic conversion for supported types

---

## Examples

### Complete CRUD Example

See the sample implementations in `vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/sql/`:
