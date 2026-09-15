package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_SCHEMA_FILENAME;
import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.GRAPHQL_SRC_DIR;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.graphql.schema.GraphQLTypeName;
import com.flipkart.krystal.vajram.graphql.schema.PlainType;
import com.flipkart.krystal.vajram.graphql.schema.SchemaLocator;
import com.flipkart.krystal.vajram.graphql.schema.SharedTypeNameResolver;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import graphql.language.ObjectTypeDefinition;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GraphQlCodeGenUtil {

  public static final String INPUTS_CLASS_NAME = "_Inputs";
  static final String GRAPHQL_FIELDS_SUFFIX = "_Fields";
  static final String GRAPHQL_ID_SUFFIX = "_Id";

  @Getter private final SchemaReaderUtil schemaReaderUtil;
  private final SharedTypeNameResolver sharedTypeNameResolver;

  public GraphQlCodeGenUtil(CodeGenUtility util) {
    this(getSchemaFilePath(util).toFile());
  }

  public GraphQlCodeGenUtil(File schemaFile) {
    this.schemaReaderUtil = new SchemaReaderUtil(schemaFile);
    this.sharedTypeNameResolver =
        new SharedTypeNameResolver(
            schemaReaderUtil.typeDefinitionRegistry(), schemaReaderUtil.rootPackageName());
  }

  /**
   * Returns the path schema file if found in SOURCE_PATH. If not found, it returns the path in the
   * module path. It is the clients responsibility to check if the file at the given path exists or
   * not.
   */
  public static Path getSchemaFilePath(CodeGenUtility util) {
    return SchemaLocator.locate(util, GRAPHQL_SRC_DIR.resolve(GRAPHQL_SCHEMA_FILENAME).toString());
  }

  TypeName toTypeNameForField(GraphQlFieldSpec fieldSpec) {
    return toTypeNameForField(fieldSpec.fieldType(), fieldSpec);
  }

  TypeName toTypeNameForField(
      com.flipkart.krystal.vajram.graphql.schema.GraphQlTypeDecorator graphQlTypeDecorator,
      GraphQlFieldSpec fieldSpec) {
    return sharedTypeNameResolver.toTypeNameForField(
        graphQlTypeDecorator, plainType -> getTypeNameForField(plainType, fieldSpec));
  }

  /**
   * Returns the raw Java type for a GraphQL field type, ignoring all nullability. Used for facet
   * declarations where {@code Errable} is implicit and must not appear in the type signature.
   *
   * <ul>
   *   <li>{@code T} / {@code T!} → {@code T}
   *   <li>{@code [T]} / {@code [T]!} / {@code [T!]} / {@code [T!]!} → {@code List<T>}
   * </ul>
   */
  TypeName toFacetTypeName(
      com.flipkart.krystal.vajram.graphql.schema.GraphQlTypeDecorator type,
      GraphQlFieldSpec fieldSpec) {
    return sharedTypeNameResolver.toFacetTypeName(
        type, plainType -> getTypeNameForField(plainType, fieldSpec));
  }

  ClassName getTypeNameForField(PlainType fieldType, GraphQlFieldSpec fieldSpec) {
    String graphQlTypeName = fieldType.graphQlType().getName();

    Optional<ObjectTypeDefinition> objectType =
        schemaReaderUtil
            .typeDefinitionRegistry()
            .getType(graphQlTypeName)
            .filter(ObjectTypeDefinition.class::isInstance)
            .map(ObjectTypeDefinition.class::cast);
    if (objectType.filter(schemaReaderUtil::hasObjectId).isPresent()) {
      return schemaReaderUtil.entityIdClassName(new GraphQLTypeName(graphQlTypeName));
    }
    return sharedTypeNameResolver.defaultLeafClassName(fieldType);
  }
}
