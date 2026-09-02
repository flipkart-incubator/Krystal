package com.flipkart.krystal.vajram.graphql.schema;

import static java.util.Objects.requireNonNull;

import graphql.language.Directive;
import graphql.language.SchemaDefinition;
import graphql.language.StringValue;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/** Parses a GraphQL SDL schema file (and any sibling {@code .graphqls} files) into a registry. */
@UtilityClass
@Slf4j
public class SchemaLoader {

  private static final String GRAPHQL_SCHEMA_EXTENSION = ".graphqls";
  private static final String ROOT_PACKAGE_DIRECTIVE = "rootPackage";
  private static final String NAME_DIR_ARG = "name";

  public static TypeDefinitionRegistry parse(File schemaFile) {
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
    List<File> files = new ArrayList<>();

    typeDefinitionRegistry.merge(schemaParser.parse(schemaFile));

    String rootPackageName = getRootPackageName(typeDefinitionRegistry);
    String typesPath = rootPackageName.replace('.', File.separatorChar);

    File graphqlsDir =
        requireNonNull(schemaFile.getParentFile()).toPath().resolve(typesPath).toFile();

    String[] graphqlSchemaFileNames =
        graphqlsDir.list((dir, name) -> name.endsWith(GRAPHQL_SCHEMA_EXTENSION));
    if (graphqlSchemaFileNames != null) {
      for (String graphqlSchemaFileName : graphqlSchemaFileNames) {
        File graphqlSchemaFile = new File(graphqlsDir, graphqlSchemaFileName);
        log.info("Found graphql schema file {} ", graphqlSchemaFile);
        if (!graphqlSchemaFile.exists()) {
          break;
        }
        files.add(graphqlSchemaFile);
      }
    }
    files.forEach(file -> typeDefinitionRegistry.merge(schemaParser.parse(file)));
    return typeDefinitionRegistry;
  }

  public static String getRootPackageName(TypeDefinitionRegistry typeDefinitionRegistry) {
    SchemaDefinition schemaExtensionDefinition =
        typeDefinitionRegistry.schemaDefinition().orElseThrow();
    List<Directive> rootPackages = schemaExtensionDefinition.getDirectives(ROOT_PACKAGE_DIRECTIVE);
    if (rootPackages.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly 1 @rootPackage directive on schema definition. Found :"
              + rootPackages.size());
    }
    Directive rootPackage = rootPackages.get(0);
    return ((StringValue) rootPackage.getArgument(NAME_DIR_ARG).getValue()).getValue();
  }
}
