package com.flipkart.krystal.vajram.graphql.codegen;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlInputJson;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import graphql.language.InputObjectTypeDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates a registry class that maps GraphQL input type names to their corresponding Java classes
 * at compile time, eliminating the need for reflection.
 *
 * <p>This generator creates a class like {@code GraphQlInputTypeRegistry} that contains a static
 * map of all input types, allowing {@link GraphQlInputTypeWiringFactory} to look up classes without
 * using {@code Class.forName()}.
 */
public class GraphQlInputTypeRegistryGen {

  private final CodeGenUtility util;
  private final SchemaReaderUtil schemaReaderUtil;
  private final TypeDefinitionRegistry typeRegistry;

  public GraphQlInputTypeRegistryGen(CodeGenUtility util) {
    this.util = util;
    GraphQlCodeGenUtil gqlUtil =
        new GraphQlCodeGenUtil(GraphQlCodeGenUtil.getSchemaFilePath(util).toFile());
    this.schemaReaderUtil = gqlUtil.schemaReaderUtil();
    this.typeRegistry = schemaReaderUtil.typeDefinitionRegistry();
  }

  /** Generates the registry class for all GraphQL input types. */
  public void generate() {
    util.note("[GraphQL Input Type Registry Gen] Starting registry generation");

    List<InputObjectTypeDefinition> inputTypes =
        typeRegistry.getTypes(InputObjectTypeDefinition.class);

    if (inputTypes.isEmpty()) {
      util.note(
          "[GraphQL Input Type Registry Gen] No input types found - generating empty registry");
      generateEmptyRegistry();
      return;
    }

    util.note("[GraphQL Input Type Registry Gen] Found " + inputTypes.size() + " input type(s)");

    String rootPackageName = schemaReaderUtil.rootPackageName();
    String packageName = rootPackageName;
    String registryClassName = "GraphQlInputTypeRegistry";
    String inputPackageName = rootPackageName + ".input";

    // Build the registry class
    TypeSpec.Builder registryBuilder =
        TypeSpec.classBuilder(registryClassName)
            .addModifiers(PUBLIC, FINAL)
            .addJavadoc(
                "Generated registry mapping GraphQL input type names to their corresponding Java classes.\n")
            .addJavadoc(
                "\n<p>This class is generated at compile time to eliminate reflection usage.\n")
            .addJavadoc("\n<p>All input types from the GraphQL schema are registered here.\n");

    // Create the static map field
    ParameterizedTypeName mapType =
        ParameterizedTypeName.get(
            ClassName.get(Map.class),
            ClassName.get(String.class),
            ParameterizedTypeName.get(
                ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

    // Build the static initializer block
    CodeBlock.Builder staticInit = CodeBlock.builder();
    staticInit.add("INPUT_TYPE_CLASSES = new $T<>();\n", HashMap.class);

    for (InputObjectTypeDefinition inputType : inputTypes) {
      String typeName = inputType.getName();
      String className = typeName + "_Immut" + GraphQlInputJson.INSTANCE.modelClassesSuffix();
      ClassName inputTypeClass = ClassName.get(inputPackageName, className);

      staticInit.add("INPUT_TYPE_CLASSES.put($S, $T.class);\n", typeName, inputTypeClass);
    }

    registryBuilder
        .addField(FieldSpec.builder(mapType, "INPUT_TYPE_CLASSES", PRIVATE, STATIC).build())
        .addStaticBlock(staticInit.build());

    // Add static getter method
    registryBuilder.addMethod(
        MethodSpec.methodBuilder("getInputTypeClass")
            .addModifiers(PUBLIC, STATIC)
            .addParameter(String.class, "graphQlTypeName")
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
            .addAnnotation(Nullable.class)
            .addJavadoc("Gets the Java class for a GraphQL input type name.\n")
            .addJavadoc(
                "\n@param graphQlTypeName The GraphQL input type name (e.g., \"SellerInput\")\n")
            .addJavadoc("\n@return The Java class, or null if not found\n")
            .addStatement("return INPUT_TYPE_CLASSES.get(graphQlTypeName)")
            .build());

    // Write the registry file
    try {
      JavaFile javaFile =
          JavaFile.builder(packageName, registryBuilder.build()).indent("  ").build();
      String fullName = packageName + "." + registryClassName;
      util.generateSourceFile(fullName, javaFile.toString(), null);
      util.note("[GraphQL Input Type Registry Gen] Successfully generated: " + registryClassName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to write registry file", e);
    }

    // Generate a factory implementation that directly calls the registry without reflection
    generateFactoryImplementation(packageName, registryClassName);
  }

  /**
   * Generates a factory implementation that directly imports and calls the registry's static
   * method, eliminating all reflection usage.
   */
  private void generateFactoryImplementation(String packageName, String registryClassName) {
    String factoryClassName = "GraphQlInputTypeWiringFactoryImpl";
    ClassName registryClass = ClassName.get(packageName, registryClassName);
    ClassName factoryClass = ClassName.get(packageName, factoryClassName);

    TypeSpec factory =
        TypeSpec.classBuilder(factoryClassName)
            .addModifiers(PUBLIC, FINAL)
            .superclass(
                ClassName.get(
                    "com.flipkart.krystal.vajram.graphql.api.schema",
                    "GraphQlInputTypeWiringFactory"))
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(
                        ClassName.get("graphql.schema.idl", "TypeDefinitionRegistry"),
                        "typeDefinitionRegistry")
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("getInputTypeClass")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(String.class, "graphQlTypeName")
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                    .addAnnotation(
                        ClassName.get("org.checkerframework.checker.nullness.qual", "Nullable"))
                    .addStatement("return $T.getInputTypeClass(graphQlTypeName)", registryClass)
                    .build())
            .build();

    try {
      JavaFile javaFile = JavaFile.builder(packageName, factory).indent("  ").build();
      String fullName = packageName + "." + factoryClassName;
      util.generateSourceFile(fullName, javaFile.toString(), null);
      util.note(
          "[GraphQL Input Type Registry Gen] Successfully generated factory: " + factoryClassName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to write factory file", e);
    }
  }

  private void generateEmptyRegistry() {
    String rootPackageName = schemaReaderUtil.rootPackageName();
    String packageName = rootPackageName;
    String registryClassName = "GraphQlInputTypeRegistry";

    ParameterizedTypeName mapType =
        ParameterizedTypeName.get(
            ClassName.get(Map.class),
            ClassName.get(String.class),
            ParameterizedTypeName.get(
                ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

    TypeSpec registry =
        TypeSpec.classBuilder(registryClassName)
            .addModifiers(PUBLIC, FINAL)
            .addJavadoc(
                "Generated registry mapping GraphQL input type names to their corresponding Java classes.\n")
            .addJavadoc(
                "\n<p>This class is generated at compile time to eliminate reflection usage.\n")
            .addJavadoc("\n<p>No input types found in the schema, so this registry is empty.\n")
            .addField(
                FieldSpec.builder(mapType, "INPUT_TYPE_CLASSES", PRIVATE, STATIC)
                    .initializer("new $T<>()", HashMap.class)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("getInputTypeClass")
                    .addModifiers(PUBLIC, STATIC)
                    .addParameter(String.class, "graphQlTypeName")
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                    .addAnnotation(Nullable.class)
                    .addJavadoc("Gets the Java class for a GraphQL input type name.\n")
                    .addJavadoc(
                        "\n@param graphQlTypeName The GraphQL input type name (e.g., \"SellerInput\")\n")
                    .addJavadoc("\n@return The Java class, or null if not found\n")
                    .addStatement("return INPUT_TYPE_CLASSES.get(graphQlTypeName)")
                    .build())
            .build();

    try {
      JavaFile javaFile = JavaFile.builder(packageName, registry).indent("  ").build();
      String fullName = packageName + "." + registryClassName;
      util.generateSourceFile(fullName, javaFile.toString(), null);
      util.note("[GraphQL Input Type Registry Gen] Successfully generated empty registry");
    } catch (Exception e) {
      throw new RuntimeException("Failed to write empty registry file", e);
    }
  }
}
