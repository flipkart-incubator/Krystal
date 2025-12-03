package com.flipkart.krystal.vajram.graphql.codegen;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates Java model interfaces for GraphQL input types.
 *
 * <p>GraphQL input types are converted to Krystal {@link Model} interfaces annotated with {@link
 * ModelRoot}. The Krystal model processor then generates the immutable implementations.
 */
public class GraphQLInputTypeGen {

  private final CodeGenUtility util;
  private final SchemaReaderUtil schemaReaderUtil;
  private final TypeDefinitionRegistry typeRegistry;
  private final Set<String> generatedTypes = new LinkedHashSet<>();

  public GraphQLInputTypeGen(CodeGenUtility util) {
    this.util = util;
    GraphQlCodeGenUtil gqlUtil = new GraphQlCodeGenUtil(util);
    this.schemaReaderUtil = gqlUtil.schemaReaderUtil();
    this.typeRegistry = schemaReaderUtil.typeDefinitionRegistry();
  }

  /** Generates Java interfaces for all GraphQL input types in the schema. */
  public void generate() {
    util.note("[GraphQL Input Type Gen] Starting input type generation");

    List<InputObjectTypeDefinition> inputTypes =
        typeRegistry.getTypes(InputObjectTypeDefinition.class);

    if (inputTypes.isEmpty()) {
      util.note("[GraphQL Input Type Gen] No input types found in schema");
      return;
    }

    util.note("[GraphQL Input Type Gen] Found " + inputTypes.size() + " input type(s)");

    for (InputObjectTypeDefinition inputType : inputTypes) {
      try {
        generateInputTypeInterface(inputType);
      } catch (Exception e) {
        util.error(
            "[GraphQL Input Type Gen] Failed to generate: "
                + inputType.getName()
                + " - "
                + e.getMessage());
      }
    }

    util.note("[GraphQL Input Type Gen] Completed");
  }

  private void generateInputTypeInterface(InputObjectTypeDefinition inputTypeDef) {
    String typeName = inputTypeDef.getName();

    if (generatedTypes.contains(typeName)) {
      return;
    }

    util.note("[GraphQL Input Type Gen] Generating: " + typeName);

    String packageName = schemaReaderUtil.rootPackageName() + ".input";

    // Create interface extending Model with @ModelRoot annotation
    TypeSpec.Builder interfaceBuilder =
        TypeSpec.interfaceBuilder(typeName)
            .addModifiers(PUBLIC)
            .addSuperinterface(Model.class)
            .addAnnotation(
                AnnotationSpec.builder(ModelRoot.class)
                    .addMember("type", "$T.$L", ModelType.class, ModelType.REQUEST)
                    .build())
            .addJavadoc("GraphQL Input Type: {@code $L}\n", typeName);

    if (inputTypeDef.getDescription() != null) {
      String desc = inputTypeDef.getDescription().getContent().trim();
      if (!desc.isEmpty()) {
        interfaceBuilder.addJavadoc("\n<p>$L\n", desc);
      }
    }

    // Add getter method for each field
    for (InputValueDefinition field : inputTypeDef.getInputValueDefinitions()) {
      String fieldName = field.getName();
      com.squareup.javapoet.TypeName fieldType = mapGraphQLType(field.getType());
      boolean isNullable = !(field.getType() instanceof NonNullType);

      MethodSpec.Builder methodBuilder =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .returns(fieldType);

      if (field.getDescription() != null) {
        String fieldDesc = field.getDescription().getContent().trim();
        if (!fieldDesc.isEmpty()) {
          methodBuilder.addJavadoc("$L\n", fieldDesc);
        }
      }

      if (isNullable) {
        methodBuilder.addAnnotation(Nullable.class);
      }

      interfaceBuilder.addMethod(methodBuilder.build());
    }

    // Write the file
    try {
      JavaFile javaFile =
          JavaFile.builder(packageName, interfaceBuilder.build()).indent("  ").build();
      String fullName = packageName + "." + typeName;
      util.generateSourceFile(fullName, javaFile.toString(), null);
      generatedTypes.add(typeName);
      util.note("[GraphQL Input Type Gen] Successfully generated: " + typeName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to write file for: " + typeName, e);
    }
  }

  /** Maps GraphQL type to Java/JavaPoet TypeName. */
  private com.squareup.javapoet.TypeName mapGraphQLType(graphql.language.Type<?> graphqlType) {
    if (graphqlType instanceof NonNullType) {
      NonNullType nonNull = (NonNullType) graphqlType;
      return mapGraphQLType(nonNull.getType());
    }

    if (graphqlType instanceof ListType) {
      ListType listType = (ListType) graphqlType;
      com.squareup.javapoet.TypeName elementType = mapGraphQLType(listType.getType());
      return ParameterizedTypeName.get(ClassName.get(List.class), elementType);
    }

    if (graphqlType instanceof graphql.language.TypeName) {
      String typeName = ((graphql.language.TypeName) graphqlType).getName();
      return mapScalarOrInputType(typeName);
    }

    return com.squareup.javapoet.TypeName.OBJECT;
  }

  private com.squareup.javapoet.TypeName mapScalarOrInputType(String typeName) {
    // GraphQL scalar types
    switch (typeName) {
      case "String":
        return com.squareup.javapoet.TypeName.get(String.class);
      case "Int":
        return com.squareup.javapoet.TypeName.get(Integer.class);
      case "Float":
        return com.squareup.javapoet.TypeName.get(Float.class);
      case "Boolean":
        return com.squareup.javapoet.TypeName.get(Boolean.class);
      case "ID":
        return com.squareup.javapoet.TypeName.get(String.class);
      default:
        // Assume it's another input type in the same package
        String packageName = schemaReaderUtil.rootPackageName() + ".input";
        return ClassName.get(packageName, typeName);
    }
  }
}

