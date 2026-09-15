package com.flipkart.krystal.vajram.graphql.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaLoaderTest {

  @TempDir Path tempDir;

  @Test
  void parsesSchemaAndResolvesRootPackage() throws IOException {
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(
        schemaFile,
        """
        schema @rootPackage(name: "com.example.accounts") { query: Query }
        type Query { account(id: ID!): Account }
        type Account { id: ID! owner: Owner name: String }
        type Owner { id: ID! displayName: String }
        """);

    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());

    assertEquals("com.example.accounts", SchemaLoader.getRootPackageName(registry));
    assertTrue(registry.getType("Account").isPresent());
    assertTrue(registry.getType("Owner").isPresent());
  }

  @Test
  void mergesSiblingGraphqlsFilesUnderRootPackageDirectory() throws IOException {
    Path rootPackageDir = tempDir.resolve("com").resolve("example").resolve("accounts");
    Files.createDirectories(rootPackageDir);
    Files.writeString(
        rootPackageDir.resolve("Extra.graphqls"),
        """
        type Extra { id: ID! }
        """);
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(
        schemaFile,
        """
        schema @rootPackage(name: "com.example.accounts") { query: Query }
        type Query { extra: Extra }
        """);

    TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile.toFile());

    assertTrue(registry.getType("Extra").isPresent());
  }
}
