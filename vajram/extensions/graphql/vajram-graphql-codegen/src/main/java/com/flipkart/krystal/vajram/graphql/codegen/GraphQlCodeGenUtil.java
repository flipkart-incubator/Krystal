package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_SCHEMA_FILENAME;
import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.GRAPHQL_SRC_DIR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.CLASS_NAME_DIR_ARG;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.JAVA_TYPE_DIRECTIVE;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.PACKAGE_NAME_DIR_ARG;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.StringValue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.tools.StandardLocation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public final class GraphQlCodeGenUtil {

  @Getter private final SchemaReaderUtil schemaReaderUtil;

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
      return schemaFile.toPath();
    }
  }

  TypeName toTypeNameForField(GraphQlFieldSpec fieldSpec) {
    return toTypeNameForField(fieldSpec.fieldType(), fieldSpec);
  }

  TypeName toTypeNameForField(
      GraphQlTypeDecorator graphQlTypeDecorator, GraphQlFieldSpec fieldSpec) {
    if (graphQlTypeDecorator instanceof PlainType plainType) {
      return getTypeNameForField(plainType, fieldSpec)
          .annotated(AnnotationSpec.builder(Nullable.class).build());
    } else if (graphQlTypeDecorator instanceof WrappedType wrappedType) {
      return getTypeNameForField(wrappedType, fieldSpec);
    }
    throw new IllegalArgumentException("Unknown fieldType: " + graphQlTypeDecorator);
  }

  TypeName getTypeNameForField(WrappedType fieldType, GraphQlFieldSpec fieldSpec) {
    GraphQlTypeDecorator innerGraphQlTypeDecorator = fieldType.innerType();
    return switch (fieldType.wrapperType()) {
      case NONNULL ->
          innerGraphQlTypeDecorator instanceof PlainType plainType
              ? getTypeNameForField(plainType, fieldSpec)
              : toTypeNameForField(innerGraphQlTypeDecorator, fieldSpec);
      case LIST ->
          ParameterizedTypeName.get(
              ClassName.get(List.class), toTypeNameForField(innerGraphQlTypeDecorator, fieldSpec));
    };
  }

  ClassName getTypeNameForField(PlainType fieldType, GraphQlFieldSpec fieldSpec) {
    String graphQlTypeName = fieldType.graphQlType().getName();

    ClassName fieldLevelOverride = getFieldLevelJavaType(fieldSpec);
    if (fieldLevelOverride != null) {
      return fieldLevelOverride;
    }

    Optional<ClassName> scalarJavaType = schemaReaderUtil.getJavaTypeForScalar(graphQlTypeName);

    return scalarJavaType.orElseGet(
        () ->
            switch (graphQlTypeName) {
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
                yield ClassName.get(
                    schemaReaderUtil.getPackageNameForType(typeName), typeName.value());
              }
            });
  }

  /**
   * Extracts the Java type from a field-level @javaType directive if present. This allows per-field
   * override of the Java type, taking highest priority over scalar-level mappings.
   */
  private @Nullable ClassName getFieldLevelJavaType(GraphQlFieldSpec fieldSpec) {
    for (Directive directive : fieldSpec.fieldDefinition().getDirectives()) {
      if (!directive.getName().equals(JAVA_TYPE_DIRECTIVE)) {
        continue;
      }

      Argument packageNameArg = directive.getArgument(PACKAGE_NAME_DIR_ARG);
      Argument classNameArg = directive.getArgument(CLASS_NAME_DIR_ARG);

      if (packageNameArg == null || classNameArg == null) {
        throw new IllegalStateException(
            "Field '%s' has @javaType directive without required 'packageName' or 'className' argument"
                .formatted(fieldSpec.fieldName()));
      }

      if (!(packageNameArg.getValue() instanceof StringValue packageNameValue)
          || !(classNameArg.getValue() instanceof StringValue classNameValue)) {
        throw new IllegalStateException(
            "Field '%s' @javaType directive: 'packageName' and 'className' must be String literals, got packageName: %s, className: %s"
                .formatted(
                    fieldSpec.fieldName(),
                    packageNameArg.getValue().getClass().getSimpleName(),
                    classNameArg.getValue().getClass().getSimpleName()));
      }

      return ClassName.get(packageNameValue.getValue(), classNameValue.getValue());
    }
    return null;
  }
}
