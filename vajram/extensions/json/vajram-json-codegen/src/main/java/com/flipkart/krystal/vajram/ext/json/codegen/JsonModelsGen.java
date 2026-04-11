package com.flipkart.krystal.vajram.ext.json.codegen;

import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.asBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.buildForBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.builderGettersAndSetters;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.copyCtor;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.getterMethod;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.newCopyForBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.newCopyForImmut;
import static com.flipkart.krystal.vajram.json.Json.JSON;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelListBuilder;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajram.json.SerializableJsonModel;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import org.jspecify.annotations.NonNull;

final class JsonModelsGen implements CodeGenerator {
  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;

  JsonModelsGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }

    TypeElement modelRootType = codeGenContext.modelRootType();

    ClassName immutClassName = util.getImmutInterfaceName(modelRootType);
    String packageName = immutClassName.packageName();

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    TypeSpec immutablePojo = generateJsonModel(modelRootType, modelMethods, immutClassName);

    util.writeJavaFile(packageName, immutablePojo, modelRootType);
  }

  /**
   * Generates the immutable JSON model class that implements the immutable interface.
   *
   * @param modelRootType The model root type
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @return TypeSpec for the immutable POJO
   */
  private TypeSpec generateJsonModel(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName) {
    ClassName immutableJsonModelName = util.getImmutClassName(modelRootType, Json.JSON);
    ClassName builderType = immutableJsonModelName.nestedClass("Builder");
    TypeSpec.Builder classBuilder =
        util.classBuilder(
            immutableJsonModelName.simpleName(), modelRootType.getQualifiedName().toString());

    TypeName byteArrayType = ArrayTypeName.of(TypeName.BYTE);

    jacksonToolFields(classBuilder, immutableJsonModelName);

    classBuilder.addField(
        FieldSpec.builder(byteArrayType, "_serializedPayload", PRIVATE)
            .addAnnotation(JsonIgnore.class)
            .build());
    classBuilder.addField(
        FieldSpec.builder(boolean.class, "_deserializationPending", PRIVATE)
            .addAnnotation(JsonIgnore.class)
            .build());

    // Add _serialize method from Serializable interface with lazy initialization
    classBuilder.addMethod(
        MethodSpec.overriding(util.getMethod(SerializableModel.class, "_serialize", 0))
            .addException(JsonProcessingException.class)
            .addCode(
"""
if (_serializedPayload == null) {
  this._serializedPayload = _WRITER.get().writeValueAsBytes(this);
}
return _serializedPayload;
""")
            .build());

    classBuilder.addMethod(
        MethodSpec.methodBuilder("_deserialize")
            .addModifiers(PRIVATE)
            .addCode(
                """
                try{
                  if (_deserializationPending) {
                    _READER.get().withValueToUpdate(this).readValue(_serializedPayload);
                    this._deserializationPending = false;
                  }
                } catch ($T e) {
                  throw new $T(e);
                }
                """,
                Exception.class,
                RuntimeException.class)
            .build());

    List<MethodSpec> methods = new ArrayList<>();

    for (ExecutableElement method : modelMethods) {
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType());
      MethodSpec pojoGetterMethod =
          getterMethod(method, false, JSON, util).addAnnotation(Override.class).build();
      MethodSpec.Builder getterBuilder =
          MethodSpec.methodBuilder(pojoGetterMethod.name)
              .returns(pojoGetterMethod.returnType)
              .addParameters(pojoGetterMethod.parameters)
              .addModifiers(pojoGetterMethod.modifiers)
              .addAnnotations(pojoGetterMethod.annotations)
              .addStatement("_deserialize()")
              .addCode(pojoGetterMethod.code)
              .addAnnotation(JsonProperty.class);
      if (fieldModelRootInfo.isPresent()) {
        getterBuilder.addAnnotation(
            AnnotationSpec.builder(JsonDeserialize.class)
                .addMember(
                    switch (fieldModelRootInfo.get().containerType()) {
                      case LIST -> "contentAs";
                      default -> "as";
                    },
                    "$T.class",
                    util.getImmutClassName(fieldModelRootInfo.get().element(), JSON))
                .build());
      }
      methods.add(getterBuilder.build());

      String fieldName = method.getSimpleName().toString();
      MethodSpec.Builder setter =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PRIVATE)
              .addParameter(util.getVariableType(method, false), fieldName)
              .addAnnotation(JsonSetter.class);
      setter.addCode(setterCode(method));
      methods.add(setter.build());
    }
    methods.addAll(
        List.of(
            newCopyForImmut(modelMethods, immutableJsonModelName).build(),
            asBuilder(modelMethods, builderType).build(),
            MethodSpec.methodBuilder("_builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderType)
                .addStatement("return new $T()", builderType)
                .build()));

    // Create builder class
    TypeSpec builderClass = generateBuilderClass(modelRootType, modelMethods, immutableModelName);

    util.addImmutableModelObjectMethods(
        immutableModelName,
        modelMethods.stream().map(ExecutableElement::getSimpleName).collect(Collectors.toSet()),
        classBuilder);
    // Create the POJO class
    return classBuilder
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(immutableModelName)
        .addSuperinterface(SerializableJsonModel.class)
        .addFields(fields(modelMethods, false))
        .addMethod(
            // Add constructor for serialized payload
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(byteArrayType, "_serializedPayload")
                .addStatement("this._serializedPayload = _serializedPayload")
                .addStatement("this._deserializationPending = true")
                .build())
        .addMethod(allArgCtor(modelMethods).build())
        .addMethod(copyCtor(modelRootType, util))
        .addMethods(methods)
        .addType(builderClass)
        .build();
  }

  public MethodSpec.Builder allArgCtor(List<ExecutableElement> modelMethods) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      constructorBuilder.addParameter(
          ParameterSpec.builder(util.getVariableType(method, false), fieldName).build());
      constructorBuilder.addStatement("this.$L($L)", fieldName, fieldName);
    }
    return constructorBuilder
        .addAnnotation(JsonCreator.class)
        .addStatement("this._deserializationPending = false");
  }

  private CodeBlock setterCode(ExecutableElement method) {
    String fieldName = method.getSimpleName().toString();
    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType());
    if (fieldModelRootInfo.isPresent()) {
      ClassName immutJsonClassName =
          util.getImmutClassName(fieldModelRootInfo.get().element(), JSON);

      return switch (fieldModelRootInfo.get().containerType()) {
        case NO_CONTAINER ->
            CodeBlock.of(
                "this.$L = $L;",
                fieldName,
                convertToImmutJson(fieldName, fieldModelRootInfo, immutJsonClassName));
        case LIST ->
            CodeBlock.of(
"""
  this.$L = $L == null
    ? null
    : $T.copyOf(
        $T.transform($L, _e -> $L));
""",
                fieldName,
                fieldName,
                ImmutableList.class,
                Lists.class,
                fieldName,
                convertToImmutJson("_e", fieldModelRootInfo, immutJsonClassName));
        case MAP ->
            CodeBlock.of(
"""
  this.$L = $L == null
      ? null
      : $T.copyOf(
          $T.transformValues($L, _e -> $L));
""",
                fieldName,
                fieldName,
                ImmutableMap.class,
                Maps.class,
                fieldName,
                convertToImmutJson("_e", fieldModelRootInfo, immutJsonClassName));
      };
    } else {
      // For other field types, just assign the parameter directly
      return CodeBlock.of("this.$L = $L;", fieldName, fieldName);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static CodeBlock convertToImmutJson(
      String fieldName, Optional<ModelRootInfo> fieldModelRootInfo, ClassName immutJsonClassName) {
    CodeBlock convertToImmutJson =
        CodeBlock.of(
            """
                $L == null
                  ? null
                  $L
                  : $L instanceof $T _immutJson
                    ? _immutJson
                    : new $T($L)
            """,
            fieldName,
            fieldModelRootInfo.get().annotation().builderExtendsModelRoot()
                ? CodeBlock.of(
                    ": $L instanceof $T _jsonBuilder ? _jsonBuilder._build()",
                    fieldName,
                    immutJsonClassName.nestedClass("Builder"))
                : CodeBlock.of(""),
            fieldName,
            immutJsonClassName,
            immutJsonClassName,
            fieldName);
    return convertToImmutJson;
  }

  private @NonNull List<FieldSpec> fields(List<ExecutableElement> modelMethods, boolean isBuilder) {
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(
              util.getModelFieldType(method, isBuilder, Json.JSON).fieldType(),
              method.getSimpleName().toString(),
              PRIVATE);
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType());
      if (isBuilder
          && fieldModelRootInfo.isPresent()
          && ContainerType.LIST.equals(fieldModelRootInfo.get().containerType())) {
        fieldBuilder.initializer("$T.empty()", ModelListBuilder.class);
      }

      fields.add(fieldBuilder.build());
    }
    return fields;
  }

  private static void jacksonToolFields(Builder classBuilder, ClassName immutableJsonModelName) {
    classBuilder.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(Supplier.class, ObjectReader.class),
                "_READER",
                PRIVATE,
                STATIC,
                FINAL)
            .initializer(
                "$T.memoize(() -> $T.OBJECT_READER.forType($T.class))",
                Suppliers.class,
                Json.class,
                immutableJsonModelName)
            .build());
    classBuilder.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(Supplier.class, ObjectWriter.class),
                "_WRITER",
                PRIVATE,
                STATIC,
                FINAL)
            .initializer(
                "$T.memoize(() ->$T.OBJECT_WRITER.forType($T.class))",
                Suppliers.class,
                Json.class,
                immutableJsonModelName)
            .build());
  }

  /**
   * Generates the builder class for the immutable JSON model.
   *
   * @param modelRootType The model Root type
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @return TypeSpec for the builder class
   */
  private TypeSpec generateBuilderClass(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName) {
    ClassName immutableJsonName = util.getImmutClassName(modelRootType, Json.JSON);
    var builderSpec = util.classBuilder("Builder", modelRootType.getQualifiedName().toString());
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));

    // Create no-arg constructor
    builderSpec.addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

    ClassName builderType = immutableJsonName.nestedClass("Builder");
    List<MethodSpec> dataAccessMethods =
        builderGettersAndSetters(modelMethods, builderType, modelRoot, JSON, util);

    // Create the builder class
    return builderSpec
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(immutableModelName.nestedClass("Builder"))
        .addFields(fields(modelMethods, true))
        .addMethods(dataAccessMethods)
        .addMethod(
            buildForBuilder(modelMethods, immutableModelName, immutableJsonName, util).build())
        .addMethod(newCopyForBuilder(modelMethods, builderType, util).build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC)
                .returns(builderType)
                .addStatement("return this")
                .build())
        .build();
  }

  private boolean isApplicable() {
    return CodegenPhase.MODELS.equals(codeGenContext.codegenPhase()) && isJsonSerdeSupported();
  }

  /**
   * Checks if the model root supports JSON serialization.
   *
   * @return true if JSON is supported, false otherwise
   */
  private boolean isJsonSerdeSupported() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return false;
    }
    // Check if JSON is mentioned in the annotation value
    return util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
        .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
        .filter(elem -> elem instanceof QualifiedNameable)
        .map(element -> requireNonNull((QualifiedNameable) element).getQualifiedName().toString())
        .anyMatch(Json.class.getCanonicalName()::equals);
  }
}
