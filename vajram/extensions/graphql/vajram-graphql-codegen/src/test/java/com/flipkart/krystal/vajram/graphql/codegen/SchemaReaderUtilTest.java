package com.flipkart.krystal.vajram.graphql.codegen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaReaderUtilTest {

  @TempDir Path tempDir;

  @Test
  void allowsUnreferencedOperationAndNestedComposedOnlyTypes() throws IOException {
    SchemaReaderUtil schemaReaderUtil =
        schemaReader(
            """
            schema @rootPackage(name: "test.schema") { query: Query }
            type Query @operation(type: QUERY) { item(id: ID!): Item @inferIdFromArgs }
            type UnusedMutation @operation(type: MUTATION) { noop: String }
            type Item { id: ID! @idField details: Details alternateDetails: Details }
            type Details @composedOnly(inRootType: "Item") { nested: Nested }
            type Nested @composedOnly(inRootType: "Item") { value: String }
            """);

    assertTrue(
        schemaReaderUtil.hasOwnIdentity(
            schemaReaderUtil.graphQLObjectTypes().get(GraphQLTypeName.of("Item"))));
    assertDoesNotThrow(() -> schemaReaderUtil.entityIdClassName(GraphQLTypeName.of("Details")));
  }

  @Test
  void rejectsSchemaOperationWithMismatchedDirective() throws IOException {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                schemaReader(
                    """
                    schema @rootPackage(name: "test.schema") { query: Query }
                    type Query @operation(type: MUTATION) { value: String }
                    """));

    assertTrue(exception.getMessage().contains("@operation(type: QUERY)"));
  }

  @Test
  void rejectsListComposedOnlyField() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                schemaReader(
                    """
                    schema @rootPackage(name: "test.schema") { query: Query }
                    type Query @operation(type: QUERY) { value: String }
                    type Item { id: ID! @idField details: [Details] }
                    type Details @composedOnly(inRootType: "Item") { value: String }
                    """));

    assertTrue(exception.getMessage().contains("cannot be a list"));
  }

  @Test
  void rejectsDuplicateIdFetcherVajramIdOnSameType() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                schemaReader(
                    """
                    schema @rootPackage(name: "test.schema") { query: Query }
                    type Query @operation(type: QUERY) { value: String }
                    type Widget { id: ID! @idField name: String }
                    type Item {
                      id: ID! @idField
                      primaryWidget: Widget @idFetcher(vajramId: "GetWidget", subPackage: "w")
                      secondaryWidget: Widget @idFetcher(vajramId: "GetWidget", subPackage: "w")
                    }
                    """));

    assertTrue(exception.getMessage().contains("can only be used by one field per type"));
  }

  @Test
  void allowsDifferentIdFetcherVajramIdsOnSameType() {
    assertDoesNotThrow(
        () ->
            schemaReader(
                """
                schema @rootPackage(name: "test.schema") { query: Query }
                type Query @operation(type: QUERY) { value: String }
                type Widget { id: ID! @idField name: String }
                type Item {
                  id: ID! @idField
                  primaryWidget: Widget @idFetcher(vajramId: "GetWidget", subPackage: "w")
                  secondaryWidget: Widget @idFetcher(vajramId: "GetAnotherWidget", subPackage: "w")
                }
                """));
  }

  private SchemaReaderUtil schemaReader(String schema) throws IOException {
    Path schemaFile = tempDir.resolve("Schema.graphqls");
    Files.writeString(schemaFile, schema);
    return new SchemaReaderUtil(schemaFile.toFile());
  }
}
