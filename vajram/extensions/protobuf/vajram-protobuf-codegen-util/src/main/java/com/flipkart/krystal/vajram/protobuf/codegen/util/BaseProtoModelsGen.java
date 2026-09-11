package com.flipkart.krystal.vajram.protobuf.codegen.util;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType.LIST;
import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType.MAP;
import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.capitalizeFirstChar;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.BaseProtoSchemaGen.validateModelType;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.convertJavaToProtoCode;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.convertProtoToJavaCode;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.isProtoTypeMap;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.isProtoTypeRepeated;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.toTitleCaseProtoName;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.MandatoryFieldMissingException;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.list.UnmodifiableModelsList;
import com.flipkart.krystal.model.map.UnmodifiableModelsMap;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.protobuf.util.ProtoListBuilder;
import com.flipkart.krystal.vajram.protobuf.util.ProtoMapBuilder;
import com.flipkart.krystal.vajram.protobuf.util.SerializableProtoModel;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates the framework wrapper class (e.g. {@code Foo_ImmutProto3} or {@code Foo_ImmutProto})
 * that bridges a protoc-generated message to the Krystal model interface. Protocol-specific
 * suffixes, the protocol instance, and the serializable sub-interface come from a {@link
 * ProtoSchemaConfig}.
 */
@Slf4j
public abstract class BaseProtoModelsGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;
  private final ModelRoot modelRoot;
  private final ProtoSchemaConfig config;
  private final ProcessingEnvironment processingEnv;

  protected BaseProtoModelsGen(ModelsCodeGenContext codeGenContext, ProtoSchemaConfig config) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
    this.modelRoot = requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
    this.config = config;
    processingEnv = util.processingEnv();
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }
    if (util.isEnumModel(codeGenContext.modelRootType())) {
      generateEnumProtoUtils();
      return;
    }
    validate();
    generateProtoImplementation();
  }

  private boolean isApplicable() {
    if (!CodegenPhase.FINAL.equals(codeGenContext.codegenPhase())) {
      util.note("Skipping protobuf models codegen since current phase is not FINAL");
      return false;
    }
    if (!isProtoSerdeSupported()) {
      util.note(
          "Skipping protobuf models codegen since "
              + config.protocolClass().getSimpleName()
              + " is not a supported protocol of the model "
              + codeGenContext.modelRootType());
      return false;
    }

    return true;
  }

  private boolean isProtoSerdeSupported() {
    return util.typeExplicitlySupportsProtocol(
        codeGenContext.modelRootType(), config.protocolClass());
  }

  private void validate() {
    validateModelType(codeGenContext.modelRootType(), util);
  }

  private void generateProtoImplementation() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    ClassName immutClassName = util.getImmutInterfaceName(modelRootType);

    String protoClassName =
        util.getImmutClassName(codeGenContext.modelRootType(), config.protocolInstance())
            .simpleName();
    String packageName = immutClassName.packageName();

    TypeSpec typeSpec = generateImplementationTypeSpec(modelRootType, packageName, protoClassName);

    JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();

    util.generateSourceFile(packageName + "." + protoClassName, javaFile.toString(), modelRootType);

    log.info("Generated protobuf implementation class: {}", protoClassName);
  }

  /**
   * Generates a {@code <EnumName>_Proto[3]Utils} class for an EnumModel. Provides {@code
   * protoToJava} / {@code javaToProto} static converters that all generated wrappers reference.
   */
  private void generateEnumProtoUtils() {
    TypeElement enumElement = codeGenContext.modelRootType();
    String packageName = util.getCodegenPackageName(enumElement);
    String utilsClassName = enumElement.getSimpleName().toString() + config.utilsSuffix();

    ClassName javaEnumType = ClassName.get(enumElement);
    // The protoc-generated Java class mirrors the proto enum name, which is TitleCased -
    // underscores stripped from the model name.
    ClassName protoEnumType =
        ClassName.get(
            packageName,
            toTitleCaseProtoName(enumElement.getSimpleName().toString()) + config.messageSuffix());

    Builder classBuilder =
        util.classBuilder(utilsClassName, enumElement.getQualifiedName().toString())
            .addModifiers(PUBLIC, FINAL);

    classBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

    classBuilder.addMethod(
        methodBuilder("protoToJava")
            .addModifiers(PUBLIC, STATIC)
            .returns(javaEnumType)
            .addParameter(protoEnumType, "protoValue")
            .addStatement("return $L", protoToJavaSwitchExpr(enumElement, javaEnumType))
            .build());

    classBuilder.addMethod(
        methodBuilder("javaToProto")
            .addModifiers(PUBLIC, STATIC)
            .returns(protoEnumType)
            .addParameter(javaEnumType, "javaValue")
            .addStatement("return $L", javaToProtoSwitchExpr(enumElement, protoEnumType))
            .build());

    JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
    util.generateSourceFile(packageName + "." + utilsClassName, javaFile.toString(), enumElement);

    log.info("Generated protobuf enum utils class: {}", utilsClassName);
  }

  private ClassName getProtoUtilsClassName(TypeElement enumElement) {
    return ClassName.get(
        util.getCodegenPackageName(enumElement),
        enumElement.getSimpleName() + config.utilsSuffix());
  }

  private TypeSpec generateImplementationTypeSpec(
      TypeElement modelRootType, String packageName, String protoClassName) {
    ClassName immutableProtoType = ClassName.get(packageName, protoClassName);
    ClassName immutInterfaceName = util.getImmutInterfaceName(modelRootType);
    ClassName immutModelName = immutInterfaceName;
    // The protoc-generated Java class mirrors the proto message name, which is TitleCased -
    // underscores stripped from the model name.
    ClassName protoMsgType =
        ClassName.get(
            packageName,
            toTitleCaseProtoName(modelRootType.getSimpleName().toString())
                + config.messageSuffix());

    ClassName protoMsgOrBuilderType =
        ClassName.get(protoMsgType.packageName(), protoMsgType.simpleName() + "OrBuilder");

    List<ExecutableElement> modelMethods = util.getModelFieldsForCodegen(modelRootType);

    TypeName serializableTypeName =
        ParameterizedTypeName.get(
            ClassName.get(config.serializableProtoSubInterface()), protoMsgType);

    Builder classBuilder =
        util.classBuilder(protoClassName, modelRootType.getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addSuperinterface(immutModelName)
            .addSuperinterface(serializableTypeName);

    classBuilder.addField(
        FieldSpec.builder(ByteString.class, "_serializedPayload", PRIVATE).build());
    classBuilder.addField(
        FieldSpec.builder(
                protoMsgType.annotated(AnnotationSpec.builder(MonotonicNonNull.class).build()),
                "_proto",
                PRIVATE)
            .build());

    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(ArrayTypeName.of(TypeName.BYTE), "_serializedPayload")
            .addStatement("this($T.copyFrom(_serializedPayload))", ByteString.class)
            .build());

    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(ByteString.class, "_serializedPayload")
            .addStatement("this._serializedPayload = _serializedPayload")
            .addStatement("this._proto = null")
            .build());

    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(protoMsgOrBuilderType, "_proto")
            .addCode(
"""
    this._proto =
        _proto instanceof $T _message
            ? _message
            : _proto instanceof $T.Builder _builder ? _builder.build() : /* Impossible */ null;
""",
                protoMsgType,
                protoMsgType)
            .addStatement("this._serializedPayload = null")
            .build());

    classBuilder.addMethod(copyCtor(modelMethods));

    classBuilder.addMethod(
        MethodSpec.overriding(util.getMethod(SerializableModel.class, "_serialize", 0))
            .addCode(
"""
    if (_serializedPayload == null){
      this._serializedPayload = _proto.toByteString();
    }
    return _serializedPayload.newInput();
""")
            .build());

    classBuilder.addMethod(
        methodBuilder("_build")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutModelName)
            .addStatement("return this")
            .build());

    classBuilder.addMethod(
        methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutableProtoType)
            .addStatement("return this")
            .build());

    classBuilder.addMethod(
        methodBuilder("_asBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ClassName.get("", "Builder"))
            .addStatement("return new Builder(_proto().toBuilder())")
            .build());

    MethodSpec.Builder getProtoMsgBuilder =
        MethodSpec.overriding(util.getMethod(SerializableProtoModel.class, "_proto", 0))
            .returns(protoMsgType)
            .beginControlFlow("if (_proto == null && _serializedPayload != null)")
            .beginControlFlow("try")
            .addStatement("_proto = $T.parseFrom(_serializedPayload)", protoMsgType)
            .nextControlFlow("catch (Exception e)")
            .addStatement("throw new RuntimeException(\"Failed to deserialize proto message\", e)")
            .endControlFlow()
            .endControlFlow()
            .beginControlFlow("if (_proto == null)")
            .addStatement(
                "throw new IllegalStateException(\"Both _proto and _serializedPayload are null\")")
            .endControlFlow()
            .addStatement("return _proto");

    classBuilder.addMethod(getProtoMsgBuilder.build());

    for (ExecutableElement method : modelMethods) {

      TypeMirror returnType = method.getReturnType();
      CodeGenType dataType = new DeclaredTypeVisitor(util, method).visit(returnType);

      classBuilder.addMethod(getterMethod(method, dataType, false, null, null).build());
    }

    util.addImmutableModelObjectMethods(
        immutModelName,
        modelMethods.stream().map(ExecutableElement::getSimpleName).collect(Collectors.toSet()),
        classBuilder);

    TypeSpec builderTypeSpec =
        generateBuilderTypeSpec(
            modelRootType, packageName, protoClassName, protoMsgType, immutModelName, modelMethods);
    classBuilder.addType(builderTypeSpec);
    ClassName immutProtoBuilderClass = immutableProtoType.nestedClass("Builder");
    classBuilder.addMethod(
        methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .returns(immutProtoBuilderClass)
            .addStatement("return new $T()", immutProtoBuilderClass)
            .build());
    classBuilder.addMethod(
        methodBuilder("_proto")
            .addModifiers(PUBLIC, STATIC)
            .returns(protoMsgType)
            .addParameter(TypeName.get(modelRootType.asType()), "_model")
            .addCode(
"""
    return _model instanceof $T _immut
        ? _immut._proto()
        : new $T(_model)._proto();
""",
                immutableProtoType,
                immutableProtoType)
            .build());
    classBuilder.addMethod(
        methodBuilder("_proto")
            .addModifiers(PUBLIC, STATIC)
            .returns(protoMsgType.nestedClass("Builder"))
            .addParameter(immutInterfaceName.nestedClass("Builder"), "_builder")
            .addCode(
"""
    return _builder instanceof $T _protoBuilder
        ? _protoBuilder._proto()
        : new $T(_builder._build())._asBuilder()._proto();
""",
                immutProtoBuilderClass,
                immutableProtoType)
            .build());
    return classBuilder.build();
  }

  private MethodSpec copyCtor(List<ExecutableElement> modelMethods) {
    MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

    ctorBuilder.addParameter(TypeName.get(codeGenContext.modelRootType().asType()), "_from");

    ctorBuilder.addCode("this._proto = _builder()");
    ctorBuilder.addCode(
        modelMethods.stream()
            .map(
                method -> {
                  boolean isOptional = util.isOptional(method.getReturnType());
                  boolean isNullable = util.isAnyNullable(method.getReturnType(), method);
                  String methodName = method.getSimpleName().toString();
                  Optional<ModelRootInfo> modelRoot =
                      util.asModelRoot(method.getReturnType(), method);
                  CodeBlock accessor;
                  if (modelRoot.isPresent() && !util.isEnumModel(modelRoot.get().element())) {
                    if (isOptional) {
                      accessor =
                          CodeBlock.of(
                              "($T) _from.$L().map($T::_asBuilder).orElse(null)",
                              util.getImmutInterfaceName(modelRoot.get().element())
                                  .nestedClass("Builder"),
                              methodName,
                              Model.class);
                    } else if (isNullable) {
                      accessor =
                          CodeBlock.of(
                              "($T) $T.ofNullable(_from.$L()).map($T::_asBuilder).orElse(null)",
                              util.getImmutInterfaceName(modelRoot.get().element())
                                  .nestedClass("Builder"),
                              Optional.class,
                              methodName,
                              Model.class);
                    } else {
                      accessor = CodeBlock.of("_from.$L()", methodName);
                    }
                  } else {
                    accessor =
                        CodeBlock.of(
                            "_from.$L()" + (isOptional ? ".orElse(null)" : ""), methodName);
                  }
                  return CodeBlock.of(".$L($L)", methodName, accessor);
                })
            .collect(CodeBlock.joining("")));
    ctorBuilder.addCode("._build()._proto();");
    return ctorBuilder.build();
  }

  private MethodSpec.Builder getterMethod(
      ExecutableElement method,
      CodeGenType dataType,
      boolean isBuilder,
      @Nullable ModelRoot modelRoot,
      @Nullable ClassName immutableProtoTypeName) {
    final TypeMirror specifiedType = method.getReturnType();
    TypeName typeName = TypeName.get(specifiedType);

    String methodName = method.getSimpleName().toString();

    MethodSpec.Builder getterBuilder =
        methodBuilder(methodName).addAnnotation(Override.class).addModifiers(PUBLIC);

    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(specifiedType, method);
    if (isBuilder
        && fieldModelRootInfo.isPresent()
        && !util.isEnumModel(fieldModelRootInfo.get().element())
        && !util.isContentErrable(specifiedType)
        && !util.isErrable(specifiedType)) {
      // The live UnmodifiableModelsList/Map view (backed by ProtoListBuilder/ProtoMapBuilder)
      // covariantly narrows the return type, which only works when the declared type is exactly
      // List<Model>/Map<K,Model> - it cannot represent per-element Errable-wrapping
      // (List<Errable<Model>>/Map<K,Errable<Model>>, since ModelsListBuilder/ModelsMapBuilder
      // require M extends Model) nor whole-field wrapping (Errable<List<Model>> is not a List, so
      // it can't be covariantly narrowed to a List subtype at all). Fall through to the plain
      // declared type in both cases, matching the non-builder getter's type.
      ClassName immutInterfaceName = util.getImmutInterfaceName(fieldModelRootInfo.get().element());
      if (LIST.equals(fieldModelRootInfo.get().containerType())) {
        typeName =
            ParameterizedTypeName.get(
                ClassName.get(UnmodifiableModelsList.class),
                TypeName.get(fieldModelRootInfo.get().type()),
                immutInterfaceName);
      } else if (MAP.equals(fieldModelRootInfo.get().containerType())) {
        TypeName mapKeyTypeName = TypeName.get(util.getMapKeyType(specifiedType));
        typeName =
            ParameterizedTypeName.get(
                ClassName.get(UnmodifiableModelsMap.class),
                mapKeyTypeName,
                TypeName.get(fieldModelRootInfo.get().type()),
                immutInterfaceName);
      }
    }

    if (needsPresenceCheckInModels(method)
        && !isMandatoryField(method)
        && !util.isOptional(specifiedType)
        && !util.isErrable(specifiedType)) {
      typeName = typeName.annotated(AnnotationSpec.builder(Nullable.class).build());
    }

    getterBuilder.returns(typeName);

    addGetterCode(
        getterBuilder, method, dataType, methodName, isBuilder, modelRoot, immutableProtoTypeName);
    return getterBuilder;
  }

  private void addGetterCode(
      MethodSpec.Builder getterBuilder,
      ExecutableElement method,
      CodeGenType dataType,
      String fieldName,
      boolean isBuilder,
      @Nullable ModelRoot modelRoot,
      @Nullable ClassName immutableProtoTypeName) {

    TypeMirror rawReturnType = method.getReturnType();
    boolean isWholeErrable = util.isErrable(rawReturnType);
    boolean isContentErrable = util.isContentErrable(rawReturnType);
    // Unwrap the outer Errable<> (if any) so LIST/MAP detection and content-type resolution below
    // operate on the real container/scalar type. The whole-field Nil/Failure state, if present, is
    // folded back in at the end by wrapping the computed expression in Errable.withValue(...) -
    // protobuf repeated/map fields have no way to represent "absent" distinctly from "empty", so
    // (mirroring the scalar Errable<T> design) Nil/Failure round-trips as an empty list/map wrapped
    // in Errable.withValue(...), never as Errable.nil().
    TypeMirror unwrappedReturnType =
        isWholeErrable ? util.getErrableInnerType(rawReturnType) : rawReturnType;
    CodeGenType unwrappedDataType =
        isWholeErrable
            ? new DeclaredTypeVisitor(util, method).visit(unwrappedReturnType)
            : dataType;

    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(rawReturnType, method);
    if (isProtoTypeRepeated(unwrappedDataType) || isProtoTypeMap(unwrappedDataType)) {
      CodeBlock plainExpr =
          isProtoTypeRepeated(unwrappedDataType)
              ? listGetterExpr(
                  unwrappedReturnType, fieldName, isBuilder, fieldModelRootInfo, isContentErrable)
              : mapGetterExpr(
                  unwrappedReturnType, fieldName, isBuilder, fieldModelRootInfo, isContentErrable);
      getterBuilder.addStatement(
          "return $L", isWholeErrable ? errableWithValue(plainExpr) : plainExpr);
      return;
    }

    // Scalar (non-container) field. Optional<T> and Errable<T> are mutually exclusive wrappers
    // around T - unwrap whichever is present (if any) so the value expression underneath is
    // computed exactly once, then re-wrap uniformly below.
    boolean isOptionalReturnType = !isWholeErrable && util.isOptional(unwrappedReturnType);
    TypeMirror scalarType =
        isOptionalReturnType ? util.getOptionalInnerType(unwrappedReturnType) : unwrappedReturnType;
    CodeBlock valueExpr =
        scalarValueExpr(unwrappedDataType, scalarType, fieldName, fieldModelRootInfo);

    // Errable<T>: check proto presence; absent → Errable.nil(), present → Errable.withValue(value).
    // Errable fields have no "mandatory" concept (Errable itself represents absence-with-reason).
    if (isWholeErrable) {
      getterBuilder
          .addCode(presenceCheckHeader(fieldName))
          .addCode("return $T.nil();\n}\n", Errable.class)
          .addStatement("return $L", errableWithValue(valueExpr));
      return;
    }

    if (needsPresenceCheckInModels(method)) {
      boolean isMandatory =
          isMandatoryField(method)
              || (isBuilder
                  && modelRoot != null
                  && modelRoot.builderExtendsModelRoot()
                  && !typeSupportsAbsentValues(method)
                  && !(fieldModelRootInfo.isPresent()
                      && fieldModelRootInfo.get().containerType().isContainer()));
      if (isMandatory) {
        getterBuilder
            .addCode(presenceCheckHeader(fieldName))
            .addCode(
                """
                throw new $T($S, $S);
              }
              """,
                MandatoryFieldMissingException.class,
                immutableProtoTypeName != null
                    ? immutableProtoTypeName.simpleName()
                    : util.getImmutClassName(
                            codeGenContext.modelRootType(), config.protocolInstance())
                        .simpleName(),
                fieldName);
      } else {
        getterBuilder
            .addCode(presenceCheckHeader(fieldName))
            .addCode(
                "return $L;\n}\n",
                isOptionalReturnType
                    ? CodeBlock.of("$T.empty()", Optional.class)
                    : CodeBlock.of("null"));
      }
    }

    getterBuilder.addStatement(
        "return $L",
        isOptionalReturnType ? CodeBlock.of("$T.of($L)", Optional.class, valueExpr) : valueExpr);
  }

  /** Wraps a value expression: {@code Errable.withValue(<expr>)}. */
  private static CodeBlock errableWithValue(CodeBlock valueExpr) {
    return CodeBlock.of("$T.withValue($L)", Errable.class, valueExpr);
  }

  /**
   * {@code if (!_proto().has<FieldName>()){} - the caller fills in the block body and closes it.}
   */
  private static CodeBlock presenceCheckHeader(String fieldName) {
    return CodeBlock.of(
        """
        if (!_proto().has$L()){
        """,
        capitalizeFirstChar(fieldName));
  }

  /**
   * Builds the getter expression for a single (already Errable/Optional-unwrapped) scalar field - a
   * {@code @ModelRoot} Model, an {@link com.flipkart.krystal.model.EnumModel}, or a plain
   * proto-convertible value. Shared by both the whole-field-Errable and Optional/plain getter
   * paths, which differ only in how the result is presence-checked and (un)wrapped.
   */
  private CodeBlock scalarValueExpr(
      CodeGenType dataType,
      TypeMirror scalarType,
      String fieldName,
      Optional<ModelRootInfo> fieldModelRootInfo) {
    if (fieldModelRootInfo.isPresent() && !util.isEnumModel(fieldModelRootInfo.get().element())) {
      return CodeBlock.of(
          "new $T(_proto().get$L())",
          util.getImmutClassName(fieldModelRootInfo.get().element(), config.protocolInstance()),
          capitalizeFirstChar(fieldName));
    } else if (util.isEnumModelType(scalarType)) {
      TypeElement enumElement =
          (TypeElement) requireNonNull(processingEnv.getTypeUtils().asElement(scalarType));
      return CodeBlock.of(
          "$T.protoToJava(_proto().get$L())",
          getProtoUtilsClassName(enumElement),
          capitalizeFirstChar(fieldName));
    } else {
      return convertProtoToJavaCode(dataType, fieldName);
    }
  }

  /**
   * Builds the getter expression for a (already Errable-unwrapped) {@code List<T>} / {@code
   * List<Errable<T>>} field, where {@code T} may be a primitive, an {@link
   * com.flipkart.krystal.model.EnumModel}, or a {@code @ModelRoot} Model. Live {@link
   * ProtoListBuilder}-backed views are only used for plain {@code List<Model>} builder getters (see
   * the covariant-narrowing note in {@link #getterMethod}); every other case returns a plain,
   * non-live {@code Lists.transform(...)} expression.
   */
  private CodeBlock listGetterExpr(
      TypeMirror listType,
      String fieldName,
      boolean isBuilder,
      Optional<ModelRootInfo> fieldModelRootInfo,
      boolean isContentErrable) {
    if (fieldModelRootInfo.isPresent() && !util.isEnumModel(fieldModelRootInfo.get().element())) {
      ClassName immutProtoClass =
          util.getImmutClassName(fieldModelRootInfo.get().element(), config.protocolInstance());
      if (isBuilder && !isContentErrable) {
        ClassName protoMsgOrBuilderType =
            ClassName.get(
                immutProtoClass.packageName(),
                toTitleCaseProtoName(fieldModelRootInfo.get().element().getSimpleName().toString())
                    + config.messageSuffix()
                    + "OrBuilder");
        return CodeBlock.builder()
            .addNamed(
"""
new $protoListBuilder:T<>(
        $modelRoot:T.class,
        $immutIface:T.class,
        $immutIface:T.Builder.class,
        $lists:T.transform(_proto().get$fieldNameCap:LList(), $immutProto:T::new),
        $lists:T.transform(
            _proto().get$fieldNameCap:LBuilderList(), $immutProto:T.Builder::new),
        _m -> _proto().add$fieldNameCap:L($immutProto:T._proto(_m)),
        _b -> _proto().add$fieldNameCap:L($immutProto:T._proto(_b)),
        (index, _model) ->
            _proto().add$fieldNameCap:L(index, $immutProto:T._proto(_model)),
        (index, _builder) ->
            _proto().add$fieldNameCap:L(index, $immutProto:T._proto(_builder)),
        _models -> {
          _proto()
              .addAll$fieldNameCap:L(
                  $iterables:T.transform(_models, $immutProto:T::_proto));
          return _models.iterator().hasNext();
        },
        _proto()::clear$fieldNameCap:L,
        (index, _model) ->
            _proto().set$fieldNameCap:L(index, $immutProto:T._proto(_model)),
        (index, _builder) ->
            _proto().set$fieldNameCap:L(index, $immutProto:T._proto(_builder)),
        index -> {
          $protoMsgOrBuilder:T ret = _proto().get$fieldNameCap:LOrBuilder(index);
          _proto().remove$fieldNameCap:L(index);
          return new $immutProto:T(ret);
        })
    .unmodifiableModelsView()""",
                ofEntries(
                    entry("protoListBuilder", ProtoListBuilder.class),
                    entry("modelRoot", fieldModelRootInfo.get().type()),
                    entry(
                        "immutIface",
                        util.getImmutInterfaceName(fieldModelRootInfo.get().element())),
                    entry("immutProto", immutProtoClass),
                    entry("lists", Lists.class),
                    entry("fieldNameCap", capitalizeFirstChar(fieldName)),
                    entry("iterables", Iterables.class),
                    entry("protoMsgOrBuilder", protoMsgOrBuilderType)))
            .build();
      } else {
        CodeBlock mapper =
            isContentErrable
                ? CodeBlock.of("_e -> $T.withValue(new $T(_e))", Errable.class, immutProtoClass)
                : CodeBlock.of("$T::new", immutProtoClass);
        return CodeBlock.of(
            "$T.transform(_proto().get$LList(), $L)",
            Lists.class,
            capitalizeFirstChar(fieldName),
            mapper);
      }
    } else {
      TypeMirror listElemType = util.getContentType(listType);
      if (isContentErrable) {
        listElemType = util.getErrableInnerType(listElemType);
      }
      if (listElemType != null && util.isEnumModelType(listElemType)) {
        TypeElement enumElement =
            (TypeElement) requireNonNull(processingEnv.getTypeUtils().asElement(listElemType));
        ClassName protoUtils = getProtoUtilsClassName(enumElement);
        CodeBlock mapper =
            isContentErrable
                ? CodeBlock.of("_e -> $T.withValue($T.protoToJava(_e))", Errable.class, protoUtils)
                : CodeBlock.of("$T::protoToJava", protoUtils);
        return CodeBlock.of(
            "$T.transform(_proto().get$LList(), $L)",
            Lists.class,
            capitalizeFirstChar(fieldName),
            mapper);
      } else if (isContentErrable) {
        return CodeBlock.of(
            "$T.transform(_proto().get$LList(), $T::withValue)",
            Lists.class,
            capitalizeFirstChar(fieldName),
            Errable.class);
      } else {
        return CodeBlock.of("_proto().get$LList()", capitalizeFirstChar(fieldName));
      }
    }
  }

  /**
   * Map analogue of {@link #listGetterExpr}, for {@code Map<K, V>} / {@code Map<K, Errable<V>>}.
   */
  private CodeBlock mapGetterExpr(
      TypeMirror mapType,
      String fieldName,
      boolean isBuilder,
      Optional<ModelRootInfo> fieldModelRootInfo,
      boolean isContentErrable) {
    if (fieldModelRootInfo.isPresent() && !util.isEnumModel(fieldModelRootInfo.get().element())) {
      ClassName immutProtoClass =
          util.getImmutClassName(fieldModelRootInfo.get().element(), config.protocolInstance());
      if (isBuilder && !isContentErrable) {
        return CodeBlock.builder()
            .addNamed(
"""
new $protoMapBuilder:T<>(
        $modelRoot:T.class,
        $immutIface:T.class,
        $immutIface:T.Builder.class,
        () -> $maps:T.transformValues(_proto().get$fieldNameCap:LMap(), $immutProto:T::new),
        _proto()::get$fieldNameCap:LCount,
        _proto()::contains$fieldNameCap:L,
        (_k, _d) -> {
          var _v = _proto().get$fieldNameCap:LOrDefault(_k, _d == null ? null : $immutProto:T._proto(($modelRoot:T) _d));
          return _v == null ? null : new $immutProto:T(_v);
        },
        _k -> new $immutProto:T.Builder(_proto().put$fieldNameCap:LBuilderIfAbsent(_k)),
        _map -> _proto().putAll$fieldNameCap:L($maps:T.transformValues(_map, _v -> $immutProto:T._proto(($modelRoot:T) _v))),
        (_k, _i) -> _proto().put$fieldNameCap:L(_k, $immutProto:T._proto(($modelRoot:T) _i)),
        _proto()::clear$fieldNameCap:L,
        _k -> _proto().remove$fieldNameCap:L(_k))
    .unmodifiableModelsView()""",
                ofEntries(
                    entry("protoMapBuilder", ProtoMapBuilder.class),
                    entry("modelRoot", fieldModelRootInfo.get().type()),
                    entry(
                        "immutIface",
                        util.getImmutInterfaceName(fieldModelRootInfo.get().element())),
                    entry("immutProto", immutProtoClass),
                    entry("maps", Maps.class),
                    entry("fieldNameCap", capitalizeFirstChar(fieldName))))
            .build();
      } else {
        CodeBlock mapper =
            isContentErrable
                ? CodeBlock.of("_v -> $T.withValue(new $T(_v))", Errable.class, immutProtoClass)
                : CodeBlock.of("$T::new", immutProtoClass);
        return CodeBlock.of(
            "$T.transformValues(_proto().get$LMap(), $L)",
            Maps.class,
            capitalizeFirstChar(fieldName),
            mapper);
      }
    } else {
      TypeMirror mapValueType = util.getMapValueType(mapType);
      if (isContentErrable) {
        mapValueType = util.getErrableInnerType(mapValueType);
      }
      if (mapValueType != null && util.isEnumModelType(mapValueType)) {
        TypeElement enumElement =
            (TypeElement) requireNonNull(processingEnv.getTypeUtils().asElement(mapValueType));
        ClassName protoUtils = getProtoUtilsClassName(enumElement);
        CodeBlock mapper =
            isContentErrable
                ? CodeBlock.of("_v -> $T.withValue($T.protoToJava(_v))", Errable.class, protoUtils)
                : CodeBlock.of("$T::protoToJava", protoUtils);
        return CodeBlock.of(
            "$T.transformValues(_proto().get$LMap(), $L)",
            Maps.class,
            capitalizeFirstChar(fieldName),
            mapper);
      } else if (isContentErrable) {
        return CodeBlock.of(
            "$T.transformValues(_proto().get$LMap(), $T::withValue)",
            Maps.class,
            capitalizeFirstChar(fieldName),
            Errable.class);
      } else {
        return CodeBlock.of("_proto().get$LMap()", capitalizeFirstChar(fieldName));
      }
    }
  }

  private TypeSpec generateBuilderTypeSpec(
      TypeElement modelRootType,
      String packageName,
      String protoClassName,
      ClassName protoMsgClassName,
      ClassName immutInterfaceName,
      List<ExecutableElement> modelMethods) {
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));

    ClassName immutableProtoType = ClassName.get(packageName, protoClassName);

    ClassName builderInterfaceClassName = immutInterfaceName.nestedClass("Builder");

    ClassName protoMsgBuilderClassName = protoMsgClassName.nestedClass("Builder");

    Builder builderClassBuilder =
        util.classBuilder("Builder", modelRootType.getQualifiedName().toString())
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(builderInterfaceClassName);

    builderClassBuilder.addField(
        FieldSpec.builder(protoMsgBuilderClassName, "_proto", PRIVATE, FINAL).build());

    builderClassBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(protoMsgBuilderClassName, "_proto")
            .addStatement("this._proto = _proto")
            .build());

    builderClassBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addStatement("this._proto = $T.newBuilder()", protoMsgClassName)
            .build());

    builderClassBuilder.addMethod(
        methodBuilder("_build")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutableProtoType)
            .addStatement("return new $L(_proto.build())", protoClassName)
            .build());

    ClassName builderType = ClassName.get("", "Builder");
    builderClassBuilder.addMethod(
        methodBuilder("_asBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(builderType)
            .addStatement("return this")
            .build());

    builderClassBuilder.addMethod(
        methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(builderType)
            .addStatement("return new Builder(_proto.clone())")
            .build());

    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      boolean isErrable = util.isErrable(method.getReturnType());
      boolean isContentErrable = util.isContentErrable(method.getReturnType());
      TypeMirror returnType = util.getErrableInnerType(method.getReturnType());
      CodeGenType dataType = new DeclaredTypeVisitor(util, method).visit(returnType);

      Optional<ModelRootInfo> fieldModelRoot = util.asModelRoot(method.getReturnType(), method);
      if (modelRoot.builderExtendsModelRoot()
          || (fieldModelRoot.isPresent()
              && !util.isEnumModel(fieldModelRoot.get().element())
              && fieldModelRoot.get().containerType().isContainer())) {
        builderClassBuilder.addMethod(
            getterMethod(method, dataType, true, modelRoot, immutableProtoType).build());
      }
      MethodSpec.Builder setterBuilder =
          methodBuilder(fieldName)
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(builderType);

      CodeBlock valueAccessor =
          isErrable ? CodeBlock.of("$L.value()", fieldName) : CodeBlock.of("$L", fieldName);
      // Per-element source to iterate for LIST/MAP fields. When the content is Errable-wrapped
      // (List<Errable<T>>/Map<K, Errable<T>>), nil/failure elements have no way to be represented
      // in a protobuf repeated/map field without changing its size or key set, so they are simply
      // dropped on write (and every element read back is Errable.withValue(...), never nil -
      // ponytail: lossy for nil/failure elements, same tradeoff already accepted for whole-field
      // Errable; upgrade path is a dedicated presence side-channel if ever needed).
      CodeBlock listSource =
          isContentErrable
              ? CodeBlock.of(
                  "$T.newArrayList($T.transform($T.filter($L, _e -> _e != null && _e.value() != null), _e -> _e.value()))",
                  Lists.class,
                  Iterables.class,
                  Iterables.class,
                  valueAccessor)
              : valueAccessor;
      CodeBlock mapSource =
          isContentErrable
              ? CodeBlock.of(
                  "$T.transformValues($T.filterValues($L, _e -> _e != null && _e.value() != null), _e -> _e.value())",
                  Maps.class,
                  Maps.class,
                  valueAccessor)
              : valueAccessor;
      if (isProtoTypeRepeated(dataType)) {
        Object addAllArg;
        if (fieldModelRoot.isPresent()
            && !util.isEnumModel(fieldModelRoot.get().element())
            && LIST.equals(fieldModelRoot.get().containerType())) {
          addAllArg =
              CodeBlock.of(
"""

          $T.transform(
              $L,
              _element ->
                  _element instanceof $T _proto
                      ? _proto._proto()
                      : _element instanceof $T.Builder _proto
                          ? _proto._proto().build()
                          : new $T(_element)._proto())
""",
                  Lists.class,
                  listSource,
                  util.getImmutClassName(fieldModelRoot.get().element(), config.protocolInstance()),
                  util.getImmutClassName(fieldModelRoot.get().element(), config.protocolInstance()),
                  util.getImmutClassName(
                      fieldModelRoot.get().element(), config.protocolInstance()));
        } else {
          TypeMirror contentType = util.getContentType(returnType);
          if (isContentErrable && contentType != null) {
            contentType = util.getErrableInnerType(contentType);
          }
          if (contentType != null && util.isEnumModelType(contentType)) {
            TypeElement enumElement =
                (TypeElement) requireNonNull(processingEnv.getTypeUtils().asElement(contentType));
            addAllArg =
                CodeBlock.of(
                    "\n          $T.transform($L, $T::javaToProto)\n",
                    Lists.class,
                    listSource,
                    getProtoUtilsClassName(enumElement));
          } else {
            addAllArg = listSource;
          }
        }
        setterBuilder.addCode(
            """
                _proto.clear$L();
                if ($L == null$L){
                  return this;
                }
                _proto.addAll$L($L);
              """,
            capitalizeFirstChar(fieldName),
            fieldName,
            isErrable ? CodeBlock.of(" || $L == null", valueAccessor) : CodeBlock.of(""),
            capitalizeFirstChar(fieldName),
            addAllArg);
      } else if (isProtoTypeMap(dataType)) {
        setterBuilder.addCode(
            """
                  _proto.clear$L();
                  if ($L == null$L){
                    return this;
                  }
                  _proto.putAll$L($L);
                """,
            capitalizeFirstChar(fieldName),
            fieldName,
            isErrable ? CodeBlock.of(" || $L == null", valueAccessor) : CodeBlock.of(""),
            capitalizeFirstChar(fieldName),
            fieldModelRoot.isPresent()
                    && !util.isEnumModel(fieldModelRoot.get().element())
                    && ContainerType.MAP.equals(fieldModelRoot.get().containerType())
                ? CodeBlock.of(
"""

          $T.transformValues(
              $L,
              _value ->
                  _value instanceof $T _proto
                      ? _proto._proto()
                      : _value instanceof $T.Builder _proto
                          ? _proto._proto().build()
                          : new $T(_value)._proto())
""",
                    Maps.class,
                    mapSource,
                    util.getImmutClassName(
                        fieldModelRoot.get().element(), config.protocolInstance()),
                    util.getImmutClassName(
                        fieldModelRoot.get().element(), config.protocolInstance()),
                    util.getImmutClassName(
                        fieldModelRoot.get().element(), config.protocolInstance()))
                : fieldModelRoot.isPresent()
                        && util.isEnumModel(fieldModelRoot.get().element())
                        && ContainerType.MAP.equals(fieldModelRoot.get().containerType())
                    ? CodeBlock.of(
                        "\n          $T.transformValues($L, $T::javaToProto)\n",
                        Maps.class,
                        mapSource,
                        getProtoUtilsClassName(fieldModelRoot.get().element()))
                    : mapSource);
      } else {
        setterBuilder.addCode(
            """
                  if ($L == null$L){
                    _proto.clear$L();
                    return this;
                  }
                """,
            fieldName,
            isErrable ? CodeBlock.of(" || $L.value() == null", fieldName) : "",
            capitalizeFirstChar(fieldName));
        if (fieldModelRoot.isPresent() && !util.isEnumModel(fieldModelRoot.get().element())) {
          ClassName fieldProtoClassName =
              util.getImmutClassName(fieldModelRoot.get().element(), config.protocolInstance());
          setterBuilder.addCode(
"""
      if($L instanceof $T _builder){
        _proto.set$L(_builder._proto());
      } else if($L instanceof $T _immut){
        _proto.set$L(_immut._proto());
      } else {
        _proto.set$L(new $T(($T)$L._build())._proto());
      }
""",
              valueAccessor,
              fieldProtoClassName.nestedClass("Builder"),
              capitalizeFirstChar(fieldName),
              valueAccessor,
              fieldProtoClassName,
              capitalizeFirstChar(fieldName),
              capitalizeFirstChar(fieldName),
              fieldProtoClassName,
              fieldModelRoot.get().element(),
              valueAccessor);
        } else {
          TypeMirror rawFieldType =
              util.isOptional(returnType)
                  ? util.getOptionalInnerType(returnType)
                  : isErrable ? util.getErrableInnerType(returnType) : returnType;
          if (util.isEnumModelType(rawFieldType)) {
            TypeElement enumElement =
                (TypeElement) requireNonNull(processingEnv.getTypeUtils().asElement(rawFieldType));
            setterBuilder.addStatement(
                "_proto.set$L($T.javaToProto($L))",
                capitalizeFirstChar(fieldName),
                getProtoUtilsClassName(enumElement),
                valueAccessor);
          } else {
            setterBuilder.addStatement(
                convertJavaToProtoCode(dataType, fieldName, valueAccessor.toString()));
          }
        }
      }

      setterBuilder.addStatement("return this");

      TypeName variableType = util.getVariableType(method, true);
      MethodSpec setter = setterBuilder.addParameter(variableType, fieldName).build();
      builderClassBuilder.addMethod(setter);

      if (fieldModelRoot.isPresent()
          && !fieldModelRoot.get().annotation().builderExtendsModelRoot()
          && !util.isEnumModel(fieldModelRoot.get().element())
          && ContainerType.NO_CONTAINER.equals(fieldModelRoot.get().containerType())) {
        builderClassBuilder.addMethod(
            methodBuilder(fieldName)
                .addModifiers(PUBLIC)
                .addParameter(
                    util.getImmutInterfaceName(fieldModelRoot.get().element())
                        .nestedClass("Builder"),
                    fieldName)
                .addAnnotation(Override.class)
                .returns(builderType)
                .addCode(setter.code)
                .build());
      }
    }

    return builderClassBuilder
        .addMethod(
            methodBuilder("_proto")
                .addModifiers(PUBLIC)
                .returns(protoMsgBuilderClassName)
                .addStatement("return _proto")
                .build())
        .build();
  }

  private boolean needsPresenceCheckInModels(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();
    CodeGenType dataType = new DeclaredTypeVisitor(util, method).visit(returnType, null);
    if (isProtoTypeRepeated(dataType)) {
      return false;
    }
    if (isProtoTypeMap(dataType)) {
      return false;
    }
    return !util.getIfAbsent(method, modelRoot).value().usePlatformDefault();
  }

  private boolean isMandatoryField(ExecutableElement method) {
    return util.getIfAbsent(method, modelRoot).value() == IfAbsentThen.FAIL;
  }

  private boolean typeSupportsAbsentValues(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();
    return !returnType.getKind().isPrimitive()
        && (util.isOptional(returnType)
            || util.isErrable(returnType)
            || util.isAnyNullable(returnType, method));
  }

  private CodeBlock protoToJavaSwitchExpr(TypeElement javaEnumElement, ClassName javaEnumType) {
    CodeBlock.Builder cb = CodeBlock.builder();
    cb.add("switch ($L) {\n", "protoValue");
    String firstConstant = null;
    for (var enclosed : javaEnumElement.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
        String name = enclosed.getSimpleName().toString();
        if (firstConstant == null) {
          firstConstant = name;
        }
        cb.add("  case $L -> $T.$L;\n", name, javaEnumType, name);
      }
    }
    if (firstConstant != null) {
      cb.add("  default -> $T.$L;\n", javaEnumType, firstConstant);
    }
    cb.add("}");
    return cb.build();
  }

  private CodeBlock javaToProtoSwitchExpr(TypeElement javaEnumElement, ClassName protoEnumType) {
    CodeBlock.Builder cb = CodeBlock.builder();
    cb.add("switch ($L) {\n", "javaValue");
    for (var enclosed : javaEnumElement.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
        String name = enclosed.getSimpleName().toString();
        cb.add("  case $L -> $T.$L;\n", name, protoEnumType, name);
      }
    }
    cb.add("}");
    return cb.build();
  }

  protected ProtoSchemaConfig config() {
    return config;
  }
}
