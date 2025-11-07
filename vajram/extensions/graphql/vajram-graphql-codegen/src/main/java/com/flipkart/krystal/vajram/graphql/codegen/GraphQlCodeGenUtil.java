package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.CLASS_NAME_DIR_ARG;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.CUSTOM_TYPE_DIRECTIVE;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.PACKAGE_NAME_DIR_ARG;
import static java.util.Objects.requireNonNullElse;

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
import java.util.List;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GraphQlCodeGenUtil {

  private final CodeGenUtility util;
  @Getter private final SchemaReaderUtil schemaReaderUtil;

  public GraphQlCodeGenUtil(CodeGenUtility util) {
    this.util = util;
    this.schemaReaderUtil = new SchemaReaderUtil(getSchemaFile(util));
  }

  private static File getSchemaFile(CodeGenUtility util) {
    FileObject schemaFileObject;
    try {
      schemaFileObject =
          util.processingEnv()
              .getFiler()
              .getResource(StandardLocation.SOURCE_PATH, "", "Schema.graphqls");
      return new File(schemaFileObject.toUri());
    } catch (IOException e) {
      throw new RuntimeException(e);
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
      case NONNULL -> innerGraphQlTypeDecorator instanceof PlainType plainType
          ? getTypeNameForField(plainType, fieldSpec)
          : toTypeNameForField(innerGraphQlTypeDecorator, fieldSpec);
      case LIST -> ParameterizedTypeName.get(
          ClassName.get(List.class), toTypeNameForField(innerGraphQlTypeDecorator, fieldSpec));
    };
  }

  ClassName getTypeNameForField(PlainType fieldType, GraphQlFieldSpec fieldSpec) {
    GraphQLTypeName typeName = new GraphQLTypeName(fieldType.graphQlType().getName());
    String packageName = null;
    for (Directive directive : fieldSpec.fieldDefinition().getDirectives()) {
      if (directive.getName().equals(CUSTOM_TYPE_DIRECTIVE)) {
        for (Argument argument : directive.getArguments()) {
          if (argument.getName().equals(PACKAGE_NAME_DIR_ARG)
              && argument.getValue() instanceof StringValue stringValue) {
            packageName = stringValue.getValue();
          }
          if (argument.getName().equals(CLASS_NAME_DIR_ARG)
              && argument.getValue() instanceof StringValue stringValue) {
            typeName = new GraphQLTypeName(stringValue.getValue());
          }
        }
      }
    }
    return switch (fieldType.graphQlType().getName()) {
      case "String" -> ClassName.get(String.class);
      case "Int" -> ClassName.get(Integer.class);
      case "Boolean" -> ClassName.get(Boolean.class);
      case "Float" -> ClassName.get(Float.class);
      case "ID" -> {
        GraphQLTypeName enclosingType = fieldSpec.enclosingType();
        yield enclosingType != null
            ? schemaReaderUtil.entityIdClassName(
                ClassName.get(
                    schemaReaderUtil.getPackageNameForType(enclosingType), enclosingType.value()))
            : ClassName.get(Object.class);
      }
      default -> ClassName.get(
          requireNonNullElse(packageName, schemaReaderUtil.getPackageNameForType(typeName)),
          typeName.value());
    };
  }
}
