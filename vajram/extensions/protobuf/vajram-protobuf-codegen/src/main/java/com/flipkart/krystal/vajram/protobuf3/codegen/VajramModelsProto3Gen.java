package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.datatypes.JavaTypes.BYTE;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.VAJRAM_REQ_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.capitalize;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProto3Applicable;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeMap;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeRepeated;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.validateProtobufCompatibility;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.codegen.common.models.CodeGenParams;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.models.VajramValidationException;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.data.IfNull;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
class VajramModelsProto3Gen implements CodeGenerator {

  private final VajramCodeGenContext creationContext;
  private final Utils util;

  VajramModelsProto3Gen(VajramCodeGenContext creationContext) {
    this.creationContext = creationContext;
    this.util = creationContext.util();
  }

  @Override
  public void generate() throws VajramValidationException {
    if (!_isApplicable(creationContext, util)) {
      return;
    }
    validateProtobufCompatibility(creationContext.vajramInfo(), util);
    generateProtoImplementation(creationContext.vajramInfo());
  }

  private static boolean _isApplicable(VajramCodeGenContext creationContext, Utils util) {
    if (!CodegenPhase.FINAL.equals(creationContext.codegenPhase())) {
      util.note("Skipping protobuf codegen since current phase is not MODELS");
      return false;
    }
    return isProto3Applicable(creationContext.vajramInfo(), util);
  }

  private void generateProtoImplementation(VajramInfo vajramInfo) {
    String vajramName = vajramInfo.vajramName();
    String packageName = vajramInfo.lite().packageName();
    String protoClassName = vajramName + "_ImmutReqProto";

    // Generate the implementation class using JavaPoet
    TypeSpec typeSpec = generateImplementationTypeSpec(vajramInfo, packageName, protoClassName);

    // Create JavaFile and write to source file
    JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();

    util.generateSourceFile(
        packageName + "." + protoClassName, javaFile.toString(), vajramInfo.vajramClass());

    log.info("Generated protobuf implementation class: {}", protoClassName);
  }

  private TypeSpec generateImplementationTypeSpec(
      VajramInfo vajramInfo, String packageName, String protoClassName) {
    ClassName immutableProtoType = ClassName.get(packageName, protoClassName);
    String vajramName = vajramInfo.vajramName();
    String immutReqInterfaceName = vajramName + "_ImmutReq";
    String protoMsgClassName = vajramName + VAJRAM_REQ_PROTO_MSG_SUFFIX;

    // Get the list of input facets
    List<DefaultFacetModel> inputFacets =
        vajramInfo.givenFacets().stream()
            .filter(f -> f.facetTypes().contains(INPUT))
            .collect(Collectors.toList());

    // Create class annotations
    AnnotationSpec suppressWarningsAnnotation =
        AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "$L", "{\"unchecked\", \"ClassReferencesSubclass\"}")
            .build();

    // Create class interfaces
    ClassName immutReqInterfaceClassName = ClassName.get(packageName, immutReqInterfaceName);
    ClassName serializableClassName = ClassName.get(SerializableModel.class);

    // Create field types
    TypeName byteArrayType = ArrayTypeName.of(TypeName.BYTE);
    ClassName protoMsgType = ClassName.get(packageName, protoMsgClassName);
    TypeName nullableProtoMsgType =
        protoMsgType.annotated(AnnotationSpec.builder(Nullable.class).build());

    // Create class builder
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(protoClassName)
            .addModifiers(PUBLIC)
            .addAnnotation(suppressWarningsAnnotation)
            .addSuperinterface(immutReqInterfaceClassName)
            .addSuperinterface(serializableClassName);

    util.addGeneratedAnnotations(classBuilder);

    // Add fields
    classBuilder.addField(FieldSpec.builder(byteArrayType, "_serializedPayload", PRIVATE).build());
    classBuilder.addField(FieldSpec.builder(nullableProtoMsgType, "_proto", PRIVATE).build());

    // Add constructor for serialized payload
    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(byteArrayType, "_serializedPayload")
            .addStatement("this._serializedPayload = _serializedPayload")
            .addStatement("this._proto = null")
            .build());

    // Add constructor for proto message
    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(protoMsgType, "_proto")
            .addStatement("this._proto = _proto")
            .addStatement("this._serializedPayload = null")
            .build());

    // Add _serialize method from Serializable interface with lazy initialization
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_serialize")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(byteArrayType)
            .beginControlFlow("if (_serializedPayload == null)")
            .addStatement("this._serializedPayload = _proto.toByteArray()")
            .endControlFlow()
            .addStatement("return _serializedPayload")
            .build());

    // Add _build method from ImmutableRequest interface
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_build")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutReqInterfaceClassName)
            .addStatement("return this")
            .build());

    // Add _asBuilder method from ImmutableRequest interface
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_asBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ClassName.get("", "Builder"))
            .addStatement("return new Builder(_proto().toBuilder())")
            .build());

    // Add _newCopy method from ImmutableRequest interface
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutReqInterfaceClassName)
            .addStatement("return new $L(_serializedPayload)", protoClassName)
            .build());

    // Add method to lazily deserialize the proto message
    MethodSpec.Builder getProtoMsgBuilder =
        MethodSpec.methodBuilder("_proto")
            .addModifiers(PUBLIC)
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

    // Add getters for all input facets
    for (DefaultFacetModel facet : inputFacets) {
      String facetName = facet.name();

      MethodSpec.Builder getterBuilder =
          MethodSpec.methodBuilder(facetName).addAnnotation(Override.class).addModifiers(PUBLIC);

      getterBuilder.returns(
          util.getFacetReturnType(facet, CodeGenParams.builder().isRequest(true).withImpl(true).build())
              .javaTypeName(facet)
              .annotated(AnnotationSpec.builder(Nullable.class).build()));

      addGetterCode(getterBuilder, facet);

      classBuilder.addMethod(getterBuilder.build());
    }

    // Add equals method that delegates to the proto object
    MethodSpec equalsMethod =
        MethodSpec.methodBuilder("equals")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(boolean.class)
            .addParameter(Object.class, "obj")
            .addCode(
                """
            if (this == obj) {
              return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
              return false;
            }
            $L other = ($L) obj;
            return _proto().equals(other._proto());
            """,
                protoClassName,
                protoClassName)
            .build();
    classBuilder.addMethod(equalsMethod);

    // Add hashCode method that delegates to the proto object
    MethodSpec hashCodeMethod =
        MethodSpec.methodBuilder("hashCode")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(int.class)
            .addStatement("return _proto().hashCode()")
            .build();
    classBuilder.addMethod(hashCodeMethod);

    // Add Builder inner class
    TypeSpec builderTypeSpec =
        generateBuilderTypeSpec(
            packageName, protoClassName, protoMsgClassName, immutReqInterfaceName, inputFacets);
    classBuilder.addType(builderTypeSpec);
    classBuilder.addMethod(
        methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .returns(immutableProtoType.nestedClass("Builder"))
            .addStatement("return new $T()", immutableProtoType.nestedClass("Builder"))
            .build());
    return classBuilder.build();
  }

  private void addGetterCode(Builder getterBuilder, DefaultFacetModel facet) {
    DataType<?> dataType = facet.dataType();

    if (isProtoTypeRepeated(dataType)) {
      // For repeated fields, use getXList() method
      getterBuilder.addStatement("return _proto().get$LList()", capitalize(facet.name()));
      return;
    }

    if (isProtoTypeMap(dataType)) {
      // For map fields, use getXMap() method
      getterBuilder.addStatement("return _proto().get$LMap()", capitalize(facet.name()));
      return;
    }

    // Return null for fields which can be inspected for presence/absence of value
    if (needsPresenceCheckInModels(facet)) {
      getterBuilder.addCode(
          """
              if (!_proto().has$L()){
                return null;
              }""",
          capitalize(facet.name()));
    }

    // Get the value from the proto message
    // Special handling for byte/Byte types to convert from ByteString to byte/Byte
    if (isByteType(facet)) {
      getterBuilder.addCode(
          """
              if (_proto().get$L().isEmpty()) {
                return null;
              }
              return _proto().get$L().byteAt(0);
              """,
          capitalize(facet.name()),
          capitalize(facet.name()));
    } else {
      getterBuilder.addStatement("return _proto().get$L()", capitalize(facet.name()));
    }
  }

  private TypeSpec generateBuilderTypeSpec(
      String packageName,
      String protoClassName,
      String protoMsgClassName,
      String immutReqInterfaceName,
      List<DefaultFacetModel> inputFacets) {

    ClassName immutableProtoType = ClassName.get(packageName, protoClassName);

    // Create class interfaces
    ClassName builderInterfaceClassName =
        ClassName.get(packageName, immutReqInterfaceName).nestedClass("Builder");

    ClassName protoBuilderClassName =
        ClassName.get(packageName, protoMsgClassName).nestedClass("Builder");
    ClassName protoMsgClassNameObj = ClassName.get(packageName, protoMsgClassName);

    // Create Builder class
    TypeSpec.Builder builderClassBuilder =
        TypeSpec.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(builderInterfaceClassName);

    util.addGeneratedAnnotations(builderClassBuilder);

    // Add Builder field
    builderClassBuilder.addField(
        FieldSpec.builder(protoBuilderClassName, "_proto", PRIVATE, FINAL).build());

    // Add Builder constructor with _proto parameter
    builderClassBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addParameter(protoBuilderClassName, "_proto")
            .addStatement("this._proto = _proto")
            .build());

    // Add Builder default constructor
    builderClassBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addStatement("this._proto = $T.newBuilder()", protoMsgClassNameObj)
            .build());

    // Add _build method
    builderClassBuilder.addMethod(
        MethodSpec.methodBuilder("_build")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutableProtoType)
            .addStatement("return new $L(_proto.build())", protoClassName)
            .build());

    // Add _asBuilder method
    builderClassBuilder.addMethod(
        MethodSpec.methodBuilder("_asBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ClassName.get("", "Builder"))
            .addStatement("return this")
            .build());

    // Add _newCopy method
    builderClassBuilder.addMethod(
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ClassName.get("", "Builder"))
            .addStatement("return new Builder(_proto.clone())")
            .build());

    // Add getters and setters for all input facets
    for (DefaultFacetModel facet : inputFacets) {
      String facetName = facet.name();

      // Add getter method
      MethodSpec.Builder getterBuilder =
          MethodSpec.methodBuilder(facetName).addAnnotation(Override.class).addModifiers(PUBLIC);

      getterBuilder.returns(
          util.getFacetReturnType(
                  facet,
                  CodeGenParams.builder().isRequest(true).isBuilder(true).withImpl(true).build())
              .javaTypeName(facet)
              .annotated(AnnotationSpec.builder(Nullable.class).build()));

      addGetterCode(getterBuilder, facet);

      builderClassBuilder.addMethod(getterBuilder.build());

      // Add setter method
      MethodSpec.Builder setterBuilder =
          MethodSpec.methodBuilder(facetName)
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(ClassName.get("", "Builder"));

      // Add parameter
      ParameterSpec.Builder paramBuilder =
          ParameterSpec.builder(util.getFacetFieldType(facet).javaTypeName(facet), facetName);
      paramBuilder.addAnnotation(Nullable.class);

      // Check if the field is a repeated field (List) or a map field
      DataType<?> dataType = facet.dataType();

      if (isProtoTypeRepeated(dataType)) {
        // For repeated fields, use clear and addAll pattern
        setterBuilder.addCode(
            """
                    _proto.clear$L();
                    if ($L == null){
                      return this;
                    }
                    _proto.addAll$L($L);
                  """,
            capitalize(facetName),
            facetName,
            capitalize(facetName),
            facetName);
      } else if (isProtoTypeMap(dataType)) {
        // For map fields, use clear and putAll pattern
        setterBuilder.addCode(
            """
                    _proto.clear$L();
                    if ($L == null){
                      return this;
                    }
                    _proto.putAll$L($L);
                  """,
            capitalize(facetName),
            facetName,
            capitalize(facetName),
            facetName);
      } else {
        // For regular fields
        setterBuilder.addCode(
            """
                    if ($L == null){
                      _proto.clear$L();
                      return this;
                    }
                  """,
            facetName,
            capitalize(facetName));

        setterBuilder.addStatement(
            isByteType(facet)
                ? "_proto.set$L(com.google.protobuf.ByteString.copyFrom(new byte[]{$L}))"
                : "_proto.set$L($L)",
            capitalize(facetName),
            facetName);
      }

      setterBuilder.addStatement("return this");

      builderClassBuilder.addMethod(setterBuilder.addParameter(paramBuilder.build()).build());
    }

    // Add method to lazily deserialize the proto message
    return builderClassBuilder
        .addMethod(
            MethodSpec.methodBuilder("_proto")
                .addModifiers(PUBLIC)
                .returns(protoBuilderClassName)
                .addStatement("return _proto")
                .build())
        .build();
  }

  private static boolean needsPresenceCheckInModels(DefaultFacetModel facet) {
    DataType<Object> dataType = facet.dataType();
    if (isProtoTypeRepeated(dataType)) {
      return false;
    }
    if (isProtoTypeMap(dataType)) {
      return false;
    }
    IfNull ifNull = facet.facetField().getAnnotation(IfNull.class);
    return ifNull == null || !ifNull.value().usePlatformDefault();
  }

  /**
   * O Checks if the facet's data type is byte or Byte
   *
   * @param facet The facet to check
   * @return true if the facet's data type is byte or Byte, false otherwise
   */
  private boolean isByteType(DefaultFacetModel facet) {
    return facet.dataType().equals(BYTE);
  }
}
