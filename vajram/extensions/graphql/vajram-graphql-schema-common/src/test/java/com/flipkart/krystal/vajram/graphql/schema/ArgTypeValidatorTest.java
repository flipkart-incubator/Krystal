package com.flipkart.krystal.vajram.graphql.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squareup.javapoet.ClassName;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArgTypeValidatorTest {

  @TempDir Path tempDir;

  private ArgTypeValidator validator(String schema) throws IOException {
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(schemaFile, schema);
    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());
    String rootPackage = SchemaLoader.getRootPackageName(registry);
    return new ArgTypeValidator(new SharedTypeNameResolver(registry, rootPackage));
  }

  private FieldDefinition queryField(TypeDefinitionRegistry registry, String fieldName) {
    return registry
        .getType("Query")
        .filter(graphql.language.ObjectTypeDefinition.class::isInstance)
        .map(graphql.language.ObjectTypeDefinition.class::cast)
        .orElseThrow()
        .getFieldDefinitions()
        .stream()
        .filter(f -> f.getName().equals(fieldName))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void resolvesExistingArgAndValidatesMatchingMandatoryType() throws IOException {
    String schema =
        """
        schema @rootPackage(name: "test.schema") { query: Query }
        type Query { account(id: ID!): Account }
        type Account { id: ID! }
        """;
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(schemaFile, schema);
    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());
    ArgTypeValidator validator =
        new ArgTypeValidator(
            new SharedTypeNameResolver(registry, SchemaLoader.getRootPackageName(registry)));

    FieldDefinition accountField = queryField(registry, "account");
    Optional<InputValueDefinition> idArg = validator.resolveArg(accountField, "id");
    assertTrue(idArg.isPresent());
    assertTrue(validator.isMandatory(idArg.get()));
    assertEquals(
        ClassName.get(String.class).toString(), validator.expectedJavaType(idArg.get()).toString());

    assertTrue(validator.validate(idArg.get(), ClassName.get(String.class), true).isEmpty());
  }

  @Test
  void missingArgIsNotResolved() throws IOException {
    ArgTypeValidator validator =
        validator(
            """
            schema @rootPackage(name: "test.schema") { query: Query }
            type Query { account(id: ID!): Account }
            type Account { id: ID! }
            """);
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());
    FieldDefinition accountField = queryField(registry, "account");
    assertTrue(validator.resolveArg(accountField, "doesNotExist").isEmpty());
  }

  @Test
  void mandatoryArgBoundToOptionalFieldFailsValidation() throws IOException {
    String schema =
        """
        schema @rootPackage(name: "test.schema") { query: Query }
        type Query { account(id: ID!): Account }
        type Account { id: ID! }
        """;
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(schemaFile, schema);
    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());
    ArgTypeValidator validator =
        new ArgTypeValidator(
            new SharedTypeNameResolver(registry, SchemaLoader.getRootPackageName(registry)));
    FieldDefinition accountField = queryField(registry, "account");
    InputValueDefinition idArg = validator.resolveArg(accountField, "id").orElseThrow();

    Optional<String> error = validator.validate(idArg, ClassName.get(String.class), false);
    assertTrue(error.isPresent());
    assertTrue(error.get().contains("non-null"));
  }

  @Test
  void typeMismatchFailsValidation() throws IOException {
    String schema =
        """
        schema @rootPackage(name: "test.schema") { query: Query }
        type Query { account(id: ID!): Account }
        type Account { id: ID! }
        """;
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(schemaFile, schema);
    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());
    ArgTypeValidator validator =
        new ArgTypeValidator(
            new SharedTypeNameResolver(registry, SchemaLoader.getRootPackageName(registry)));
    FieldDefinition accountField = queryField(registry, "account");
    InputValueDefinition idArg = validator.resolveArg(accountField, "id").orElseThrow();

    Optional<String> error = validator.validate(idArg, ClassName.get(Integer.class), true);
    assertFalse(error.isEmpty());
  }
}
