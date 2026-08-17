package com.flipkart.krystal.vajram.ext.json.codegen;

import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.buildForBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.builderGettersAndSetters;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.copyCtor;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.getterMethod;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.isIfAbsentFail;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.newCopyForBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.stripNullableAnnotation;
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
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.list.ModelsListBuilder;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.vajram.codegen.common.generators.SerdeModelValidator;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajram.json.JsonModelHint;
import com.flipkart.krystal.vajram.json.ModelErrableDeserializer;
import com.flipkart.krystal.vajram.json.SerializableJsonModel;
import com.flipkart.krystal.vajram.json.serialized.BytesJson;
import com.flipkart.krystal.vajram.json.serialized.JsonRepresentation;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

final class JsonModelsGen implements CodeGenerator {
  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;
  private final ModelRoot modelRoot;

  JsonModelsGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
    this.modelRoot = requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
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
    List<ExecutableElement> modelMethods = util.getModelFieldsForCodegen(modelRootType);

    // Validate serde compatibility (nested Models must support JSON; purity not required for JSON)
    new SerdeModelValidator(util, modelRootType, Json.JSON).validate(modelMethods);

    jsonSpecificValidations(modelMethods);

    TypeSpec immutablePojo = generateJsonModel(modelRootType, modelMethods, immutClassName);

    util.writeJavaFile(packageName, immutablePojo, modelRootType);
  }

  private void jsonSpecificValidations(List<ExecutableElement> modelMethods) {
    // Validate that map keys are only String or Integer (JSON object keys constraint)
    validateMapKeyTypes(modelMethods);
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
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));

    ClassName immutableJsonModelName = util.getImmutClassName(modelRootType, Json.JSON);
    ClassName builderType = immutableJsonModelName.nestedClass("Builder");
    TypeSpec.Builder classBuilder =
        util.classBuilder(
            immutableJsonModelName.simpleName(), modelRootType.getQualifiedName().toString());

    TypeName byteArrayType = ArrayTypeName.of(TypeName.BYTE);

    jacksonToolFields(classBuilder, immutableJsonModelName);

    classBuilder.addField(
        FieldSpec.builder(JsonRepresentation.class, "_serializedPayload", PRIVATE)
            .addAnnotation(JsonIgnore.class)
            .build());
    classBuilder.addField(
        FieldSpec.builder(boolean.class, "_deserializationPending", PRIVATE)
            .addAnnotation(JsonIgnore.class)
            .build());

    // Add _serialize method from Serializable interface with lazy initialization
    classBuilder.addMethod(
        MethodSpec.overriding(
                util.getMethod(() -> SerializableJsonModel.class.getMethod("_serializedJson")))
            .addCode(
                """
                if (_serializedPayload == null) {
                  this._serializedPayload = new $T(_WRITER.get().writeValueAsBytes(this));
                }
                return _serializedPayload;
                """,
                BytesJson.class)
            .build());

    classBuilder.addMethod(
        MethodSpec.methodBuilder("_deserialize")
            .addModifiers(PRIVATE)
            .addCode(
                """
                if (_deserializationPending) {
                  try{
                    _serializedPayload.deserialize(_READER.get().withValueToUpdate(this));
                    this._deserializationPending = false;
                  } catch ($T e) {
                    throw new $T(e);
                  }
                }
                """,
                Exception.class,
                RuntimeException.class)
            .build());

    classBuilder.addMethod(
        MethodSpec.overriding(
                util.getMethod(() -> SerializableJsonModel.class.getMethod("_reader")))
            .addStatement("return _READER.get()")
            .build());

    classBuilder.addMethod(
        MethodSpec.overriding(
                util.getMethod(() -> SerializableJsonModel.class.getMethod("_writer")))
            .addStatement("return _WRITER.get()")
            .build());

    List<MethodSpec> methods = new ArrayList<>();

    for (ExecutableElement method : modelMethods) {
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      MethodSpec pojoGetterMethod =
          getterMethod(method, false, JSON, util, immutableJsonModelName, modelRoot)
              .addAnnotation(Override.class)
              .build();
      MethodSpec.Builder getterBuilder =
          MethodSpec.methodBuilder(pojoGetterMethod.name)
              .returns(pojoGetterMethod.returnType)
              .addParameters(pojoGetterMethod.parameters)
              .addModifiers(pojoGetterMethod.modifiers)
              .addAnnotations(pojoGetterMethod.annotations)
              .addStatement("_deserialize()")
              .addCode(pojoGetterMethod.code)
              .addAnnotation(JsonProperty.class);
      if (fieldModelRootInfo.isPresent() && !util.isEnumModel(fieldModelRootInfo.get().element())) {
        ClassName modelImmutClass =
            util.getImmutClassName(fieldModelRootInfo.get().element(), JSON);
        boolean isWholeErrable = util.isErrable(method.getReturnType());
        boolean isContentErrable = util.isContentErrable(method.getReturnType());
        if (isWholeErrable || isContentErrable) {
          // Errable<Model>/Errable<List<Model>>/Errable<Map<K,Model>>/List<Errable<Model>>/
          // Map<K,Errable<Model>> (and combinations thereof): contentAs/as can't narrow
          // Errable<Model> (or its content) to ImmutJson since ImmutJson isn't an Errable
          // subtype - use a hint-driven custom deserializer instead. `using` targets the whole
          // Errable-wrapped getter type (resolves any nested List/Map/Errable structure at
          // runtime via generics); `contentUsing` targets each List/Map element directly when
          // only the content - not the whole field - is Errable-wrapped.
          getterBuilder
              .addAnnotation(
                  AnnotationSpec.builder(JsonDeserialize.class)
                      .addMember(
                          isWholeErrable ? "using" : "contentUsing",
                          "$T.class",
                          ModelErrableDeserializer.class)
                      .build())
              .addAnnotation(
                  AnnotationSpec.builder(JsonModelHint.class)
                      .addMember("value", "$T.class", modelImmutClass)
                      .build());
        } else {
          getterBuilder.addAnnotation(
              AnnotationSpec.builder(JsonDeserialize.class)
                  .addMember(
                      switch (fieldModelRootInfo.get().containerType()) {
                        case LIST, MAP -> "contentAs";
                        default -> "as";
                      },
                      "$T.class",
                      modelImmutClass)
                  .build());
        }
      }
      methods.add(getterBuilder.build());

      String fieldName = method.getSimpleName().toString();
      // @JsonSetter for Errable<T> fields uses Errable<T> as parameter type.
      // Jackson uses ErrableDeserializer (registered via ErrableModule) to produce an Errable<T>
      // from the JSON value, so the setter must accept Errable<T>.
      // All other field types use the standard non-builder type.
      TypeName setterParamType = util.getVariableType(method, false);
      MethodSpec.Builder setter =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PRIVATE)
              .addParameter(setterParamType, fieldName)
              .addAnnotation(JsonSetter.class);
      setter.addCode(setterCode(method));
      methods.add(setter.build());
    }
    methods.addAll(
        List.of(
            asBuilderUsingGetters(modelMethods, builderType, util).build(),
            MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
                .addModifiers(Modifier.PUBLIC)
                .returns(immutableJsonModelName)
                .addStatement("return this")
                .build(),
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
            // Primary constructor accepting SerializedJson
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(JsonRepresentation.class, "_serializedPayload")
                .addStatement("this._serializedPayload = _serializedPayload")
                .addStatement("this._deserializationPending = true")
                .build())
        .addMethod(
            // Convenience constructor for raw bytes — delegates to the SerializedJson constructor
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(byteArrayType, "_serializedPayload")
                .addStatement("this(new $T(_serializedPayload))", BytesJson.class)
                .build())
        .addMethod(
            // Convenience constructor for inputStream
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(InputStream.class, "_inputStream")
                .addParameter(boolean.class, "_retainBytes")
                .addException(IOException.class)
                .addCode(
"""
    if (_retainBytes) {
      this._serializedPayload = new $T(_inputStream.readAllBytes());
      this._deserializationPending = true;
    } else {
      _reader().withValueToUpdate(this).readValue(_inputStream);
    }
""",
                    BytesJson.class)
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
      // @JsonCreator receives Errable<T> directly (provided by ErrableDeserializer), so use
      // the non-builder param type which preserves the full Errable<T>.
      constructorBuilder.addParameter(
          ParameterSpec.builder(util.getVariableType(method, false), fieldName).build());
      if (util.isErrable(method.getReturnType())) {
        // Bypass the @JsonSetter (which takes @Nullable T) and set the Errable<T>/Errable<List<T>>/
        // Errable<Map<K,V>> field directly. Null-safety: if Jackson passes null (absent field),
        // convert to Errable.nil().
        constructorBuilder.addStatement(
            "this.$L = $L == null ? $T.nil() : $L", fieldName, fieldName, Errable.class, fieldName);
      } else {
        constructorBuilder.addStatement("this.$L($L)", fieldName, fieldName);
      }
    }
    return constructorBuilder
        .addAnnotation(JsonCreator.class)
        .addStatement("this._deserializationPending = false");
  }

  private CodeBlock setterCode(ExecutableElement method) {
    String fieldName = method.getSimpleName().toString();
    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);

    // Errable<T>: @JsonSetter accepts Errable<T> (provided by ErrableDeserializer).
    // Jackson calls getNullValue() → Errable.nil() for absent fields, so null-guard here
    // is just extra safety per the contract that Errable fields must never be null.
    if (util.isErrable(method.getReturnType())) {
      return CodeBlock.of(
          "this.$L = $L == null ? $T.nil() : $L;", fieldName, fieldName, Errable.class, fieldName);
    }

    return switch (util.getContainerType(method.getReturnType())) {
      case NO_CONTAINER -> {
        if (fieldModelRootInfo.isPresent()
            && !util.isEnumModel(fieldModelRootInfo.get().element())) {
          yield CodeBlock.of(
              "this.$L = $L;",
              fieldName,
              convertToImmutJson(
                  fieldName,
                  fieldModelRootInfo,
                  util.getImmutClassName(fieldModelRootInfo.get().element(), JSON)));
        } else {
          yield CodeBlock.of("this.$L = $L;", fieldName, fieldName);
        }
      }
      case RANGE -> CodeBlock.of("this.$L = $L;", fieldName, fieldName);
      case LIST -> {
        boolean isContentErrable = util.isContentErrable(method.getReturnType());
        if (fieldModelRootInfo.isPresent()
            && !util.isEnumModel(fieldModelRootInfo.get().element())) {
          // Each element's conversion target: for content-Errable fields, unwrap _e.value()
          // before converting to the Immut type, then re-wrap - the field keeps the raw Model
          // interface element type (Errable<> is invariant), so no Immut cast is needed on it.
          CodeBlock elementExpr =
              isContentErrable
                  ? CodeBlock.of(
                      "_e == null || _e.value() == null ? $T.nil() : $T.withValue($L)",
                      Errable.class,
                      Errable.class,
                      convertToImmutJson(
                          "_e.value()",
                          fieldModelRootInfo,
                          util.getImmutClassName(fieldModelRootInfo.get().element(), JSON)))
                  : convertToImmutJson(
                      "_e",
                      fieldModelRootInfo,
                      util.getImmutClassName(fieldModelRootInfo.get().element(), JSON));
          yield CodeBlock.of(
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
              elementExpr);
        } else {
          yield CodeBlock.of(
              "this.$L = $L == null ? null :$T.copyOf($L);",
              fieldName,
              fieldName,
              ImmutableList.class,
              fieldName);
        }
      }
      case MAP -> {
        boolean isContentErrable = util.isContentErrable(method.getReturnType());
        if (fieldModelRootInfo.isPresent()
            && !util.isEnumModel(fieldModelRootInfo.get().element())) {
          CodeBlock valueExpr =
              isContentErrable
                  ? CodeBlock.of(
                      "_e == null || _e.value() == null ? $T.nil() : $T.withValue($L)",
                      Errable.class,
                      Errable.class,
                      convertToImmutJson(
                          "_e.value()",
                          fieldModelRootInfo,
                          util.getImmutClassName(fieldModelRootInfo.get().element(), JSON)))
                  : convertToImmutJson(
                      "_e",
                      fieldModelRootInfo,
                      util.getImmutClassName(fieldModelRootInfo.get().element(), JSON));
          yield CodeBlock.of(
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
              valueExpr);
        } else {
          yield CodeBlock.of(
              "this.$L = $L == null ? null : $T.copyOf($L);",
              fieldName,
              fieldName,
              ImmutableMap.class,
              fieldName);
        }
      }
    };
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
            fieldModelRootInfo.isPresent()
                    && fieldModelRootInfo.get().annotation().builderExtendsModelRoot()
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

  private List<FieldSpec> fields(List<ExecutableElement> modelMethods, boolean isBuilder) {
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      TypeName fieldType = util.getModelFieldType(method, isBuilder, Json.JSON).fieldType();
      // Strip @Nullable for @IfAbsent(FAIL) fields since they are guaranteed non-null after build
      if (!isBuilder && isIfAbsentFail(method, util, modelRoot)) {
        fieldType = stripNullableAnnotation(fieldType);
      }
      FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(fieldType, method.getSimpleName().toString(), PRIVATE);
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      if (util.isErrable(method.getReturnType())) {
        // Initialize to Errable.nil() so absent JSON fields deserialize as Nil rather than null
        fieldBuilder.initializer("$T.nil()", Errable.class);
      } else if (isBuilder
          && fieldModelRootInfo.isPresent()
          && !util.isEnumModel(fieldModelRootInfo.get().element())
          && !util.isContentErrable(method.getReturnType())) {
        switch (fieldModelRootInfo.get().containerType()) {
          case LIST -> fieldBuilder.initializer("$T.empty()", ModelsListBuilder.class);
          case MAP -> fieldBuilder.initializer("$T.empty()", ModelsMapBuilder.class);
        }
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
        builderGettersAndSetters(codeGenContext, modelMethods, builderType, modelRoot, JSON, util);

    // Create the builder class
    return builderSpec
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(immutableModelName.nestedClass("Builder"))
        .addFields(fields(modelMethods, true))
        .addMethods(dataAccessMethods)
        .addMethod(buildForBuilder(modelMethods, immutableJsonName, util, modelRoot).build())
        .addMethod(newCopyForBuilder(modelMethods, builderType, util).build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC)
                .returns(builderType)
                .addStatement("return this")
                .build())
        .build();
  }

  /**
   * Generates {@code _asBuilder()} that reads each field via its getter so that lazy
   * deserialization (e.g. the pending-flag pattern in Json immutable models) is triggered before
   * the values are copied into the new builder. For Optional-typed getters the value is unwrapped
   * via {@code .orElse(null)} to match the builder setter's {@code @Nullable T} signature.
   */
  private static MethodSpec.Builder asBuilderUsingGetters(
      List<ExecutableElement> modelMethods, TypeName builderType, CodeGenUtility util) {
    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("_asBuilder")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(builderType);
    builder.addCode("return new $T()", builderType);
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      if (util.isOptional(method.getReturnType())) {
        builder.addCode(".$L($L().orElse(null))", fieldName, fieldName);
      } else {
        builder.addCode(".$L($L())", fieldName, fieldName);
      }
    }
    builder.addCode(";");
    return builder;
  }

  private void validateMapKeyTypes(List<ExecutableElement> modelMethods) {
    for (ExecutableElement method : modelMethods) {
      TypeMirror returnType = method.getReturnType();
      if (util.isOptional(returnType)) {
        returnType = util.getOptionalInnerType(returnType);
      }
      if (util.isMapType(returnType)) {
        TypeMirror keyType = util.getMapKeyType(returnType);
        if (!util.isString(keyType) && !util.isSameRawType(keyType, Integer.class)) {
          util.error(
              "Field '%s' in model '%s' is a Map with key type '%s'. JSON only supports String and Integer map key types."
                  .formatted(
                      method.getSimpleName(),
                      codeGenContext.modelRootType().getQualifiedName(),
                      keyType),
              method);
        }
      }
    }
  }

  private boolean isApplicable() {
    if (!CodegenPhase.MODELS.equals(codeGenContext.codegenPhase()) || !isJsonSerdeSupported()) {
      return false;
    }

    // Enum models don't need generated JSON wrapper classes -
    // Jackson handles them directly via the EnumModelModule
    return !util.isEnumModel(codeGenContext.modelRootType());
  }

  /**
   * Checks if the model root supports JSON serialization.
   *
   * @return true if JSON is supported, false otherwise
   */
  private boolean isJsonSerdeSupported() {
    return util.typeExplicitlySupportsProtocol(codeGenContext.modelRootType(), Json.class);
  }
}
