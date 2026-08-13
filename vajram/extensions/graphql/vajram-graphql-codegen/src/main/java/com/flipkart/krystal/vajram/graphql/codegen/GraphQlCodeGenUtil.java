package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_SCHEMA_FILENAME;
import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.GRAPHQL_SRC_DIR;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.tools.StandardLocation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GraphQlCodeGenUtil {

  @Getter private final SchemaReaderUtil schemaReaderUtil;

  public GraphQlCodeGenUtil(CodeGenUtility util) {
    this(getSchemaFilePath(util).toFile());
  }

  public GraphQlCodeGenUtil(File schemaFile) {
    this.schemaReaderUtil = new SchemaReaderUtil(schemaFile);
  }

  /**
   * Returns the path schema file if found in SOURCE_PATH. If not found, it returns the path in the
   * module path. It is the clients responsibility to check if the file at the given path exists or
   * not.
   */
  public static Path getSchemaFilePath(CodeGenUtility util) {
    try {
      return new File(
              util.processingEnv()
                  .getFiler()
                  .getResource(StandardLocation.SOURCE_PATH, "", GRAPHQL_SCHEMA_FILENAME)
                  .toUri())
          .toPath();
    } catch (IOException e) {
      util.note(
          """
              Failed to get schema file in SOURCE_PATH. This can happen in projects which have not configured a JPMS named moduled. \
              Trying to look for 'moduleRootPath' annotation processor option""");
      Path moduleRootPath = util.moduleRootPath();
      if (moduleRootPath == null) {
        throw new RuntimeException(
            "Schema.graphqls was not present in SOURCE_PATH, nor was the "
                + MODULE_ROOT_PATH_KEY
                + " passed");
      }
      File schemaFile =
          moduleRootPath.resolve(GRAPHQL_SRC_DIR).resolve(GRAPHQL_SCHEMA_FILENAME).toFile();
      if (!schemaFile.exists()) {
        util.note(
            "Schema.graphqls was not present in SOURCE_PATH, nor was it found in the module path: "
                + schemaFile.getAbsolutePath());
      }
      if (!schemaFile.exists()) {
        util.note("Schema.graphqls not found. GraphQl Code Generation May be skipped");
      }
      return schemaFile.toPath();
    }
  }

  TypeName toTypeNameForField(GraphQlFieldSpec fieldSpec) {
    return toTypeNameForField(fieldSpec.fieldType(), fieldSpec);
  }

  TypeName toTypeNameForField(
      GraphQlTypeDecorator graphQlTypeDecorator, GraphQlFieldSpec fieldSpec) {
    ClassName errable = ClassName.get("com.flipkart.krystal.data", "Errable");
    if (graphQlTypeDecorator instanceof PlainType plainType) {
      // T → Errable<T>
      return ParameterizedTypeName.get(errable, getTypeNameForField(plainType, fieldSpec));
    } else if (graphQlTypeDecorator instanceof WrappedType wrappedType) {
      return switch (wrappedType.wrapperType()) {
        // T! → T  (or [T]! → List<resolve(inner)>)
        case NONNULL -> toNonNullTypeName(wrappedType.innerType(), fieldSpec);
        // [T] → Errable<List<resolve(inner)>>
        case LIST ->
            ParameterizedTypeName.get(
                errable,
                ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    toTypeNameForField(wrappedType.innerType(), fieldSpec)));
      };
    }
    throw new IllegalArgumentException("Unknown fieldType: " + graphQlTypeDecorator);
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
  TypeName toFacetTypeName(GraphQlTypeDecorator type, GraphQlFieldSpec fieldSpec) {
    if (type instanceof PlainType plainType) {
      return getTypeNameForField(plainType, fieldSpec);
    } else if (type instanceof WrappedType wrappedType) {
      return switch (wrappedType.wrapperType()) {
        case NONNULL -> toFacetTypeName(wrappedType.innerType(), fieldSpec);
        case LIST ->
            ParameterizedTypeName.get(
                ClassName.get(List.class), toFacetTypeName(wrappedType.innerType(), fieldSpec));
      };
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Resolves a type that is known to be non-null (inside a {@code !} wrapper).
   *
   * <ul>
   *   <li>{@code T!} → {@code T}
   *   <li>{@code [T]!} → {@code List<resolve(T)>}
   *   <li>{@code [T!]!} → {@code List<T>}
   * </ul>
   */
  private TypeName toNonNullTypeName(GraphQlTypeDecorator type, GraphQlFieldSpec fieldSpec) {
    if (type instanceof PlainType plainType) {
      return getTypeNameForField(plainType, fieldSpec);
    } else if (type instanceof WrappedType wrappedType) {
      return switch (wrappedType.wrapperType()) {
        case NONNULL -> toNonNullTypeName(wrappedType.innerType(), fieldSpec); // flatten !!
        case LIST -> // [inner]! → List<resolve(inner)>
            ParameterizedTypeName.get(
                ClassName.get(List.class), toTypeNameForField(wrappedType.innerType(), fieldSpec));
      };
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  ClassName getTypeNameForField(PlainType fieldType, GraphQlFieldSpec fieldSpec) {
    String graphQlTypeName = fieldType.graphQlType().getName();

    ClassName scalarJavaType = schemaReaderUtil.getJavaTypeForScalar(graphQlTypeName);
    if (scalarJavaType != null) {
      return scalarJavaType;
    }

    return switch (graphQlTypeName) {
      case "String" -> ClassName.get(String.class);
      case "Int" -> ClassName.get(Integer.class);
      case "Boolean" -> ClassName.get(Boolean.class);
      case "Float" -> ClassName.get(Float.class);
      case "ID" -> {
        GraphQLTypeName enclosingType = fieldSpec.enclosingType();
        yield enclosingType != null
            ? schemaReaderUtil.entityIdClassName(enclosingType)
            : ClassName.get(Object.class);
      }
      default -> {
        GraphQLTypeName typeName = new GraphQLTypeName(graphQlTypeName);
        yield ClassName.get(schemaReaderUtil.getPackageNameForType(typeName), typeName.value());
      }
    };
  }
}
