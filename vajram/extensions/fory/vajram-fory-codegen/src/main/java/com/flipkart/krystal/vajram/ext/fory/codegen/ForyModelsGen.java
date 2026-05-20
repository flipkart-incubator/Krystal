package com.flipkart.krystal.vajram.ext.fory.codegen;

import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.buildForBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.builderGettersAndSetters;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.copyCtor;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.getterMethod;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.isIfAbsentFail;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.newCopyForBuilder;
import static com.flipkart.krystal.vajram.codegen.common.generators.JavaModelsGen.stripNullableAnnotation;
import static com.flipkart.krystal.vajram.fory.Fory.FORY;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.TRANSIENT;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.model.list.ModelsListBuilder;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.codegen.common.generators.SerdeModelValidator;
import com.flipkart.krystal.vajram.fory.Fory;
import com.flipkart.krystal.vajram.fory.Fory.ForyClassProvider;
import com.flipkart.krystal.vajram.fory.SerializableForyModel;
import com.google.auto.service.AutoService;
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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.apache.fory.ThreadSafeFory;

/**
 * Code generator that produces {@code _ImmutFory} wrapper classes for models that declare Apache
 * Fory as a supported serde protocol via {@code @SupportedModelProtocols(Fory.class)}.
 *
 * <p>Generated classes follow the same lazy-deserialization pattern as the JSON module: a
 * constructor from {@code byte[]} stores the payload and defers deserialization until the first
 * getter call. Serialization is handled by the shared {@link Fory#foryInstance()} singleton.
 *
 * <p>Unlike protobuf, Apache Fory works directly with Java POJOs — no IDL or schema files are
 * generated. The {@code _ImmutFory} class <b>is</b> both the Krystal model implementation and the
 * Fory serialization target. Meta-fields ({@code _serializedPayload}, {@code
 * _deserializationPending}) are marked {@code transient} so Fory ignores them.
 */
final class ForyModelsGen implements CodeGenerator {
  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;
  private final ModelRoot modelRoot;

  ForyModelsGen(ModelsCodeGenContext codeGenContext) {
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

    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    new SerdeModelValidator(util, modelRootType, Fory.FORY).validate(modelMethods);

    TypeSpec immutableFory = generateForyModel(modelRootType, modelMethods, immutClassName);

    util.writeJavaFile(packageName, immutableFory, modelRootType);
  }

  private TypeSpec generateForyModel(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName) {
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));

    ClassName immutableForyModelName = util.getImmutClassName(modelRootType, Fory.FORY);
    ClassName builderType = immutableForyModelName.nestedClass("Builder");
    TypeSpec.Builder classBuilder =
        util.classBuilder(
            immutableForyModelName.simpleName(), modelRootType.getQualifiedName().toString());

    TypeName byteArrayType = ArrayTypeName.of(TypeName.BYTE);

    // Static Fory field — Krystal models are not thread-safe by design
    classBuilder.addField(
        FieldSpec.builder(ThreadSafeFory.class, "_FORY", PRIVATE, STATIC, FINAL)
            .initializer("$T.foryInstance()", Fory.class)
            .build());

    // Transient meta-fields (Fory ignores transient fields)
    classBuilder.addField(
        FieldSpec.builder(byteArrayType, "_serializedPayload", PRIVATE, TRANSIENT).build());
    classBuilder.addField(
        FieldSpec.builder(boolean.class, "_deserializationPending", PRIVATE, TRANSIENT).build());

    // _serialize method
    classBuilder.addMethod(
        MethodSpec.overriding(util.getMethod(SerializableModel.class, "_serialize", 0))
            .addCode(
                """
                if (_serializedPayload == null) {
                  this._serializedPayload = _FORY.serialize(this);
                }
                return _serializedPayload;
                """)
            .build());

    // _deserialize method (lazily copies fields from a Fory-deserialized temp instance)
    MethodSpec.Builder deserializeBuilder =
        MethodSpec.methodBuilder("_deserialize").addModifiers(PRIVATE);
    deserializeBuilder.addCode(
        """
        if (_deserializationPending) {
          $T _temp = ($T) _FORY.deserialize(_serializedPayload);
        """,
        immutableForyModelName,
        immutableForyModelName);
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      deserializeBuilder.addCode("  this.$L = _temp.$L;\n", fieldName, fieldName);
    }
    deserializeBuilder.addCode(
        """
          this._deserializationPending = false;
        }
        """);
    classBuilder.addMethod(deserializeBuilder.build());

    List<MethodSpec> methods = new ArrayList<>();

    for (ExecutableElement method : modelMethods) {
      MethodSpec pojoGetterMethod =
          getterMethod(method, false, FORY, util, immutableForyModelName, modelRoot)
              .addAnnotation(Override.class)
              .build();
      // Wrap getter to call _deserialize() first
      MethodSpec.Builder getterBuilder =
          MethodSpec.methodBuilder(pojoGetterMethod.name)
              .returns(pojoGetterMethod.returnType)
              .addParameters(pojoGetterMethod.parameters)
              .addModifiers(pojoGetterMethod.modifiers)
              .addAnnotations(pojoGetterMethod.annotations)
              .addStatement("_deserialize()")
              .addCode(pojoGetterMethod.code);
      methods.add(getterBuilder.build());

      // Private setter for field assignment
      String fieldName = method.getSimpleName().toString();
      MethodSpec.Builder setter =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PRIVATE)
              .addParameter(util.getVariableType(method, false), fieldName);
      setter.addCode(setterCode(method));
      methods.add(setter.build());
    }

    methods.addAll(
        List.of(
            asBuilderUsingGetters(modelMethods, builderType, util).build(),
            MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
                .addModifiers(Modifier.PUBLIC)
                .returns(immutableForyModelName)
                .addStatement("return this")
                .build(),
            MethodSpec.methodBuilder("_builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderType)
                .addStatement("return new $T()", builderType)
                .build()));

    TypeSpec builderClass = generateBuilderClass(modelRootType, modelMethods, immutableModelName);

    util.addImmutableModelObjectMethods(
        immutableModelName,
        modelMethods.stream().map(ExecutableElement::getSimpleName).collect(Collectors.toSet()),
        classBuilder);

    return classBuilder
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(immutableModelName)
        .addSuperinterface(SerializableForyModel.class)
        .addFields(fields(modelMethods, false))
        .addMethod(
            // Constructor from serialized payload
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
        .addType(
            TypeSpec.classBuilder("_ForyImmutClassProvider2")
                .addAnnotation(
                    AnnotationSpec.builder(AutoService.class)
                        .addMember("value", "$T.class", ForyClassProvider.class)
                        .build())
                .addModifiers(PUBLIC, STATIC)
                .addSuperinterface(ForyClassProvider.class)
                .addMethod(
                    MethodSpec.overriding(
                            util.getMethod(() -> ForyClassProvider.class.getMethod("getForyClass")))
                        .addStatement("return $T.class", immutableForyModelName)
                        .build())
                .build())
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
    return constructorBuilder.addStatement("this._deserializationPending = false");
  }

  private CodeBlock setterCode(ExecutableElement method) {
    String fieldName = method.getSimpleName().toString();
    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);

    return switch (util.getContainerType(method.getReturnType())) {
      case NO_CONTAINER -> {
        if (fieldModelRootInfo.isPresent()
            && !util.isEnumModel(fieldModelRootInfo.get().element())) {
          yield CodeBlock.of(
              "this.$L = $L;",
              fieldName,
              convertToImmutFory(
                  fieldName,
                  fieldModelRootInfo,
                  util.getImmutClassName(fieldModelRootInfo.get().element(), FORY)));
        } else {
          yield CodeBlock.of("this.$L = $L;", fieldName, fieldName);
        }
      }
      case LIST -> {
        if (fieldModelRootInfo.isPresent()
            && !util.isEnumModel(fieldModelRootInfo.get().element())) {
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
              convertToImmutFory(
                  "_e",
                  fieldModelRootInfo,
                  util.getImmutClassName(fieldModelRootInfo.get().element(), FORY)));
        } else {
          yield CodeBlock.of(
              """
              this.$L = $L == null
                ? null
                : $T.copyOf($L);
              """,
              fieldName,
              fieldName,
              ImmutableList.class,
              fieldName);
        }
      }
      case MAP -> {
        if (fieldModelRootInfo.isPresent()
            && !util.isEnumModel(fieldModelRootInfo.get().element())) {
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
              convertToImmutFory(
                  "_e",
                  fieldModelRootInfo,
                  util.getImmutClassName(fieldModelRootInfo.get().element(), FORY)));
        } else {
          yield CodeBlock.of(
              """
              this.$L = $L == null
                ? null
                : $T.copyOf($L);
              """,
              fieldName,
              fieldName,
              ImmutableMap.class,
              fieldName);
        }
      }
    };
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static CodeBlock convertToImmutFory(
      String fieldName, Optional<ModelRootInfo> fieldModelRootInfo, ClassName immutForyClassName) {
    CodeBlock convertToImmutFory =
        CodeBlock.of(
            """
                $L == null
                  ? null
                  $L
                  : $L instanceof $T _immutFory
                    ? _immutFory
                    : new $T($L)
            """,
            fieldName,
            fieldModelRootInfo.get().annotation().builderExtendsModelRoot()
                ? CodeBlock.of(
                    ": $L instanceof $T _foryBuilder ? _foryBuilder._build()",
                    fieldName,
                    immutForyClassName.nestedClass("Builder"))
                : CodeBlock.of(""),
            fieldName,
            immutForyClassName,
            immutForyClassName,
            fieldName);
    return convertToImmutFory;
  }

  private List<FieldSpec> fields(List<ExecutableElement> modelMethods, boolean isBuilder) {
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      TypeName fieldType = util.getModelFieldType(method, isBuilder, Fory.FORY).fieldType();
      if (!isBuilder && isIfAbsentFail(method, util, modelRoot)) {
        fieldType = stripNullableAnnotation(fieldType);
      }
      FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(fieldType, method.getSimpleName().toString(), PRIVATE);
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      if (isBuilder
          && fieldModelRootInfo.isPresent()
          && !util.isEnumModel(fieldModelRootInfo.get().element())) {
        switch (fieldModelRootInfo.get().containerType()) {
          case LIST -> fieldBuilder.initializer("$T.empty()", ModelsListBuilder.class);
          case MAP -> fieldBuilder.initializer("$T.empty()", ModelsMapBuilder.class);
        }
      }
      fields.add(fieldBuilder.build());
    }
    return fields;
  }

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

  private TypeSpec generateBuilderClass(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName) {
    ClassName immutableForyName = util.getImmutClassName(modelRootType, Fory.FORY);
    var builderSpec = util.classBuilder("Builder", modelRootType.getQualifiedName().toString());
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));

    builderSpec.addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

    ClassName builderType = immutableForyName.nestedClass("Builder");
    List<MethodSpec> dataAccessMethods =
        builderGettersAndSetters(modelMethods, builderType, modelRoot, FORY, util);

    return builderSpec
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(immutableModelName.nestedClass("Builder"))
        .addFields(fields(modelMethods, true))
        .addMethods(dataAccessMethods)
        .addMethod(buildForBuilder(modelMethods, immutableForyName, util, modelRoot).build())
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
    if (!CodegenPhase.MODELS.equals(codeGenContext.codegenPhase()) || !isForySerdeSupported()) {
      return false;
    }

    TypeElement modelRootType = codeGenContext.modelRootType();
    if (!util.getModelProtocols(modelRootType).contains(FORY)) {
      util.note(
          "Skipping fory codegen for %s as model protocols doesn't contain Fory"
              .formatted(modelRootType),
          modelRootType);
      return false;
    }
    // Enum models don't need generated Fory wrapper classes -
    // Fory handles Java enums directly
    return !util.isEnumModel(modelRootType);
  }

  private boolean isForySerdeSupported() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return false;
    }
    return util.getTypeElemsFromAnnotationMember(supportedModelProtocols::value).stream()
        .map(element -> element.getQualifiedName().toString())
        .anyMatch(Fory.class.getCanonicalName()::equals);
  }
}
