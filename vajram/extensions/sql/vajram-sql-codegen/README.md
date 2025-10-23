# SQL Trait Code Generation for Krystal

This module provides automatic code generation for SQL database operations in Krystal using the `@Sql` annotation.

## Overview

The SQL trait code generator automatically creates Vajram implementations from trait interfaces annotated with `@Sql`. This eliminates boilerplate code for database operations while maintaining type safety and integration with Krystal's execution framework.

## Usage

### 1. Define a SQL Trait

Create an interface extending `TraitRoot` with the `@Sql` annotation:

```java
package com.example;

import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.sql.SqlQuery;
import com.flipkart.krystal.model.IfAbsent;
import java.util.List;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

@Sql("select * from users where id = ?")
public interface GetUserTrait<User> extends TraitRoot<User> {
  static class _Inputs {
    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
```

### 2. Code Generation

The annotation processor will automatically generate a Vajram implementation class named `GetUserTrait_sql` with:

- **_Inputs inner class**: Contains all fields from the trait's `_Inputs` plus a `query` field
- **_InternalFacets inner class**: Manages dependencies on `SQLRead` vajram and optional `RowMapper`
- **getSimpleInputResolvers()**: Configures input resolution for the SQL query
- **@Output method**: Executes the query and maps results to the generic type

### 3. Generated Code Structure

```java
@Vajram
@InvocableOutsideGraph
public abstract class GetUserTrait_sql extends IOVajramDef<List<User>> {
  
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
  
  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            queryResult_s,
            depInput(SQLRead_Req.selectQuery_s).usingAsIs(query_s).asResolver(),
            depInput(SQLRead_Req.parameters_s).usingAsIs(parameters_s).asResolver()));
  }
  
  @Output
  public static CompletableFuture<List<User>> getUser(
      Result queryResult, @Nullable RowMapper resultMapper) {
    if (resultMapper != null) {
      return resultMapper.map(queryResult, User.class).toFuture();
    }
    return null;
  }
}
```

## Requirements

### Trait Requirements

1. **Must be an interface** extending `TraitRoot<T>`
2. **Must have @Sql annotation** with a SQL query string
3. **Must have _Inputs inner class** containing:
   - A `parameters` field of type `List<Object>` for query parameters
   - Optional additional input fields

### Dependencies

Add to your `build.gradle`:

```gradle
dependencies {
    implementation project(':vajram:extensions:sql:vajram-sql')
    annotationProcessor project(':vajram:extensions:sql:vajram-sql-codegen')
}
```

## Validation

The code generator performs the following validations:

1. **Trait Structure**: Ensures the trait is an interface with `@Sql` annotation
2. **_Inputs Class**: Verifies presence of static `_Inputs` inner class
3. **Parameters Field**: Validates existence of `parameters` field of type `List<Object>`
4. **Generic Type**: Extracts and validates the generic type parameter from `TraitRoot<T>`

## Architecture

### Components

- **SqlTraitCodeGeneratorProvider**: Service provider implementing `VajramCodeGeneratorProvider`
- **SqlTraitCodeGenerator**: Main generator implementing `CodeGenerator`
  - Validates trait structure
  - Extracts SQL query and generic type
  - Generates Vajram implementation with proper annotations
  - Adds static imports for facets and resolvers

### Integration

The generator integrates with Krystal's annotation processing pipeline:

1. Registered via `@AutoService(VajramCodeGeneratorProvider.class)`
2. Executed during `CodegenPhase.VAJRAMS`
3. Generates code only for interfaces with `@Sql` annotation
4. Outputs properly formatted Java files with static imports

## Example

See the test example in `GetUserTrait.java` for a complete working example of:
- Trait definition
- Query parameterization
- Integration with Krystal execution framework
- Custom row mapping with `RowMapper`

## Limitations

- Currently supports SELECT queries through `SQLRead` vajram
- For UPDATE/INSERT/DELETE operations, use `SQLWrite` vajram directly
- The `RowMapper` is optional but recommended for custom type mapping
