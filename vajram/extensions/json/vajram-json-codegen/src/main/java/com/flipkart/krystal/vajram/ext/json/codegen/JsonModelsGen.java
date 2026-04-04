package com.flipkart.krystal.vajram.ext.json.codegen;

import static com.flipkart.krystal.model.PlainJavaObject.POJO;
import static com.flipkart.krystal.vajram.json.Json.JSON;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajram.json.SerializableJsonModel;
import com.google.common.base.Suppliers;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
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
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

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

    TypeSpec immutablePojo =
        generateJsonModel(packageName, modelRootType, modelMethods, immutClassName);

    util.writeJavaFile(packageName, immutablePojo, modelRootType);
  }

  private ClassName getImmutableJsonName(TypeElement modelRootType) {
    ClassName immutClassName = util.getImmutInterfaceName(modelRootType);
    return ClassName.get(
        immutClassName.packageName(), immutClassName.simpleName() + JSON.modelClassesSuffix());
  }

  /**
   * Generates the immutable Json model class that implements the immutable interface.
   *
   * @param packageName The package name of the model root type
   * @param modelRootType The model root type
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @return TypeSpec for the immutable POJO
   */
  private TypeSpec generateJsonModel(
      String packageName,
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName) {
    ClassName immutableJsonModelName = getImmutableJsonName(modelRootType);
    TypeSpec.Builder classBuilder =
        util.classBuilder(
                immutableJsonModelName.simpleName(), modelRootType.getQualifiedName().toString())
            .addAnnotation(
                AnnotationSpec.builder(JsonDeserialize.class)
                    .addMember("builder", "$T.class", immutableJsonModelName.nestedClass("Builder"))
                    .build());

    // Create constructor for the class
    ClassName immutablePojoName =
        ClassName.get(packageName, immutableModelName.simpleName() + POJO.modelClassesSuffix());

    TypeName byteArrayType = ArrayTypeName.of(TypeName.BYTE);

    jacksonToolFields(classBuilder, immutableJsonModelName);
    classBuilder.addField(FieldSpec.builder(immutablePojoName, "_pojo", PRIVATE).build());
    classBuilder.addField(
        FieldSpec.builder(byteArrayType, "_serializedPayload", PRIVATE)
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

    // Create getter methods
    List<MethodSpec> methods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      methods.add(
          MethodSpec.methodBuilder(methodName)
              .addAnnotation(JsonProperty.class)
              .addModifiers(PUBLIC)
              .returns(TypeName.get(method.getReturnType()))
              .addStatement("return _pojo().$L()", methodName)
              .build());
    }

    methods.addAll(commonMethods(immutableJsonModelName));

    // Add method to lazily deserialize the JSON
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_pojo")
            .addModifiers(PRIVATE)
            .returns(immutablePojoName)
            .addCode(
"""
        if (_pojo == null && _serializedPayload != null) {
          try{
            _pojo = _READER.get().readValue(_serializedPayload, $T.class)._pojo();
          } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize json bytes", e);
          }
        } else if (_pojo == null) {
          throw new IllegalStateException("Both _pojo and _serializedPayload are null");
        }
        return _pojo;
""",
                immutableJsonModelName)
            .build());

    copyCtor(modelRootType, classBuilder, immutablePojoName);

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
        .addMethod(
            // Add constructor accepting pojo
            MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .addParameter(immutablePojoName, "_pojo")
                .addStatement("this._pojo = _pojo")
                .build())
        .addMethod(
            // Add constructor for serialized payload
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(byteArrayType, "_serializedPayload")
                .addStatement("this._serializedPayload = _serializedPayload")
                .addStatement("this._pojo = null")
                .build())
        .addMethod(
            // Add constructor for serialized payload and pojo
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(byteArrayType, "_serializedPayload")
                .addParameter(immutablePojoName, "_pojo")
                .addStatement("this._serializedPayload = _serializedPayload")
                .addStatement("this._pojo = _pojo")
                .build())
        .addMethods(methods)
        .addType(builderClass)
        .build();
  }

  private void copyCtor(
      TypeElement modelRootType, Builder classBuilder, ClassName immutablePojoName) {
    ModelRootInfo modelRootInfo = util.asModelRoot(modelRootType.asType()).orElseThrow();

    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(TypeName.get(modelRootType.asType()), "_from")
            .addCode(
"""
    this._pojo =
      _from instanceof $T _pojo ? _pojo : $L new $T(_from);
""",
                immutablePojoName,
                modelRootInfo.annotation().builderExtendsModelRoot()
                    ? CodeBlock.of(
                        "_from instanceof $T.Builder _builder ? _builder._build() : ",
                        immutablePojoName)
                    : CodeBlock.of(""),
                immutablePojoName)
            .build());
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

  private static List<MethodSpec> commonMethods(ClassName immutableJsonModelName) {
    // Create _asBuilder method to return a new Builder instance with all fields
    MethodSpec.Builder asBuilderMethodBuilder =
        MethodSpec.methodBuilder("_asBuilder")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(immutableJsonModelName.nestedClass("Builder"))
            .addStatement(
                "return new $T(_pojo()._asBuilder())",
                immutableJsonModelName.nestedClass("Builder"));

    MethodSpec builderMethod =
        MethodSpec.methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .returns(immutableJsonModelName.nestedClass("Builder"))
            .addStatement("return new $T()", immutableJsonModelName.nestedClass("Builder"))
            .build();
    // Add methods to the list
    return List.of(
        asBuilderMethodBuilder.build(),
        builderMethod,
        // Add _newCopy method from ImmutableModel interface
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutableJsonModelName)
            .addCode(
                """
                    if(_serializedPayload != null && _pojo != null ) {
                      return new $T(_serializedPayload, _pojo);
                    } else if(_serializedPayload != null) {
                      return new $T(_serializedPayload);
                    } else if(_pojo != null){
                      return new $T(_pojo);
                    } else {
                      throw new $T("Both _pojo and _serializedPayload are null");
                    }
                    """,
                immutableJsonModelName,
                immutableJsonModelName,
                immutableJsonModelName,
                IllegalStateException.class)
            .build());
  }

  /**
   * Generates the builder class for the immutable Json model.
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
    ClassName immutableJsonName = getImmutableJsonName(modelRootType);
    var builderSpec =
        util.classBuilder("Builder", modelRootType.getQualifiedName().toString())
            .addAnnotation(
                AnnotationSpec.builder(JsonPOJOBuilder.class)
                    .addMember("buildMethodName", "$S", "_build")
                    .addMember("withPrefix", "$S", "")
                    .build());
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));
    ClassName immutablePojoName =
        ClassName.get(
            immutableJsonName.packageName(),
            immutableModelName.simpleName() + POJO.modelClassesSuffix());

    builderSpec.addField(
        FieldSpec.builder(immutablePojoName.nestedClass("Builder"), "_pojo", PRIVATE, FINAL)
            .build());

    // Create no-arg constructor
    builderSpec.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .addStatement("this._pojo = $T._builder()", immutablePojoName)
            .build());

    // Create builder copy constructor
    builderSpec.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .addParameter(immutablePojoName.nestedClass("Builder"), "_pojo")
            .addStatement("this._pojo = _pojo")
            .build());

    // Create setter methods
    List<MethodSpec> dataAccessMethods = new ArrayList<>();
    ClassName builderType = immutableJsonName.nestedClass("Builder");
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      TypeName variableType = util.getVariableType(method, true);
      MethodSpec.Builder setterBuilder =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PUBLIC)
              .addParameter(variableType, fieldName)
              .returns(builderType)
              .addAnnotation(Override.class)
              .addAnnotation(JsonSetter.class);
      Optional<ModelRootInfo> modelRootInfo = util.asModelRoot(method.getReturnType());
      if (modelRootInfo.isPresent()) {
        ClassName nestedImmutableJsonName = getImmutableJsonName(modelRootInfo.get().element());
        setterBuilder.addCode(
"""
      if($L){
        this._pojo.$L($L);
      } else {
        this._pojo.$L(new $T($L));
      }
""",
            !modelRootInfo.get().annotation().builderExtendsModelRoot()
                ? CodeBlock.of("$L instanceof $T", fieldName, nestedImmutableJsonName)
                : CodeBlock.of(
                    "$L instanceof $T || $L instanceof $T",
                    fieldName,
                    nestedImmutableJsonName,
                    fieldName,
                    nestedImmutableJsonName.nestedClass("Builder")),
            fieldName,
            fieldName,
            fieldName,
            nestedImmutableJsonName,
            fieldName);
      } else {
        setterBuilder.addStatement("this._pojo.$L($L)", fieldName, fieldName);
      }

      MethodSpec setter = setterBuilder.addStatement("return this").build();
      dataAccessMethods.add(setter);

      if (modelRootInfo.isPresent()
          && !modelRootInfo.get().annotation().builderExtendsModelRoot()) {
        ClassName nestedImmutableJsonName = getImmutableJsonName(modelRootInfo.get().element());
        dataAccessMethods.add(
            MethodSpec.methodBuilder(fieldName)
                .addModifiers(PUBLIC)
                .addParameter(
                    util.getImmutInterfaceName(modelRootInfo.get().element())
                        .nestedClass("Builder"),
                    fieldName)
                .returns(builderType)
                .addAnnotation(Override.class)
                .addCode(
"""
      if($L instanceof $T.Builder){
        this._pojo.$L($L);
      } else {
        this._pojo.$L(new $T($L._build()));
      }
      return this;
""",
                    fieldName,
                    nestedImmutableJsonName,
                    fieldName,
                    fieldName,
                    fieldName,
                    nestedImmutableJsonName,
                    fieldName)
                .build());
      }
      if (modelRoot.builderExtendsModelRoot()) {
        dataAccessMethods.add(
            MethodSpec.methodBuilder(fieldName)
                .addModifiers(PUBLIC)
                .returns(TypeName.get(method.getReturnType()))
                .addStatement("return _pojo.$L()", fieldName)
                .build());
      }
    }

    // Create _build method
    MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("_build")
            .addModifiers(PUBLIC)
            .returns(immutableJsonName)
            .addStatement("return new $T(_pojo._build())", immutableJsonName)
            .addAnnotation(Override.class);

    // Create _newCopy method for the Builder
    MethodSpec.Builder builderCopyMethodBuilder =
        MethodSpec.methodBuilder("_newCopy")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(builderType)
            .addStatement("return new $T(_pojo._newCopy())", builderType);

    // Create the builder class
    return builderSpec
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(immutableModelName.nestedClass("Builder"))
        .addMethods(dataAccessMethods)
        .addMethod(buildMethodBuilder.build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC)
                .returns(builderType)
                .addStatement("return this")
                .build())
        .addMethod(builderCopyMethodBuilder.build())
        .build();
  }

  private boolean isApplicable() {
    return CodegenPhase.MODELS.equals(codeGenContext.codegenPhase()) && isJsonSerdeSupported();
  }

  /**
   * Checks if the model root supports Json serialization.
   *
   * @return true if Json is supported, false otherwise
   */
  private boolean isJsonSerdeSupported() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return false;
    }
    // Check if Json is mentioned in the annotation value
    return util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
        .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
        .filter(elem -> elem instanceof QualifiedNameable)
        .map(element -> requireNonNull((QualifiedNameable) element).getQualifiedName().toString())
        .anyMatch(Json.class.getCanonicalName()::equals);
  }
}
