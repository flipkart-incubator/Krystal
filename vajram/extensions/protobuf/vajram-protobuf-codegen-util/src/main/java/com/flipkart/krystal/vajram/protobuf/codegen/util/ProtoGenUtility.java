package com.flipkart.krystal.vajram.protobuf.codegen.util;

import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.BOOLEAN;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.BYTE_ARRAY;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.DOUBLE;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.FLOAT;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.INT;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.LONG;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.STRING;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.BOOL_P;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.BYTES_P;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.DOUBLE_P;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.FLOAT_P;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.SINT32_P;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.SINT64_P;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType.STRING_P;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

import com.flipkart.krystal.annos.InvocableOutsideProcess;
import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.EnumFieldType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.MapFieldType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.MessageFieldType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.OptionalFieldType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoFieldType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoScalarType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.RepeatedFieldType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Protocol-agnostic helpers shared across the protobuf codegen pipeline. */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ProtoGenUtility {

  /** Map of Java DataType objects to their Protocol Buffers scalar-type mapping. */
  private static final Map<CodeGenType, ProtoScalarType> JAVA_TO_PROTO_SCALAR_TYPES =
      ImmutableMap.<CodeGenType, ProtoScalarType>builder()
          .put(BOOLEAN, BOOL_P)
          .put(INT, SINT32_P)
          .put(LONG, SINT64_P)
          .put(FLOAT, FLOAT_P)
          .put(DOUBLE, DOUBLE_P)
          .put(STRING, STRING_P)
          .put(BYTE_ARRAY, BYTES_P)
          .build();

  public static String getSimpleClassName(String canonicalClassName) {
    String typeName = canonicalClassName;

    if (typeName.contains(".")) {
      typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
    }
    return typeName;
  }

  public static @NonNull Optional<String> getPackageName(String responseTypeName) {
    int lastDotIndex = responseTypeName.lastIndexOf('.');
    if (lastDotIndex == -1) {
      return Optional.empty();
    }
    return Optional.of(responseTypeName.substring(0, lastDotIndex));
  }

  /**
   * Checks if the protobuf code generator is applicable to the given vajram for the supplied
   * protocol class. This method only checks conditions that determine whether we should proceed
   * with generation.
   */
  public static boolean isProtoApplicable(
      VajramInfo vajramInfo,
      VajramCodeGenUtility util,
      Class<? extends ModelProtocol> protocolClass) {

    TypeElement vajramClass = vajramInfo.vajramClassElem();
    InvocableOutsideProcess invocableOutsideProcess =
        vajramClass.getAnnotation(InvocableOutsideProcess.class);
    if (invocableOutsideProcess == null) {
      CharSequence message =
          "Skipping class '%s' since remote invocation is not enabled"
              .formatted(vajramClass.getQualifiedName());
      util.codegenUtil().note(message);
      return false;
    }

    Element annotationSource =
        vajramInfo.inputsElement() != null ? vajramInfo.inputsElement() : vajramClass;
    SupportedModelProtocols supportedSerdeProtocols =
        annotationSource.getAnnotation(SupportedModelProtocols.class);
    List<? extends TypeMirror> serializationProtocols =
        getSerializationProtocols(supportedSerdeProtocols, util);
    if (serializationProtocols.stream()
        .noneMatch(
            serializationProtocol ->
                util.codegenUtil().isSameRawType(serializationProtocol, protocolClass))) {
      CharSequence message =
          "Skipping class '%s' since %s is not one of the intended serialization protocols : %s "
              .formatted(
                  vajramClass.getQualifiedName(),
                  protocolClass.getSimpleName(),
                  serializationProtocols);
      util.codegenUtil().note(message);
      return false;
    }

    return true;
  }

  public static Path createOutputDirectory(Path sourceOutputLocation, CodeGenUtility util)
      throws IOException {
    try {
      // Navigate to find the 'java' directory and create a parallel 'protobuf' directory.
      // E.g. "/generated/sources/annotationProcessor/java/main" becomes
      //      "/generated/sources/annotationProcessor/protobuf/main"
      List<String> pathComponents = new ArrayList<>();
      Path javaDir = null;

      while (sourceOutputLocation != null && sourceOutputLocation.getFileName() != null) {
        if (sourceOutputLocation.getFileName().toString().equals("java")) {
          javaDir = sourceOutputLocation;
          break;
        }
        Path parent = sourceOutputLocation.getParent();
        if (parent == null) {
          break;
        }
        pathComponents.add(0, sourceOutputLocation.getFileName().toString());
        sourceOutputLocation = parent;
      }

      if (javaDir == null) {
        throw util.errorAndThrow("Failed to find 'java' directory in the source path");
      }

      Path protoRootDir = checkNotNull(javaDir.getParent()).resolve("protobuf");

      Path rootDir = protoRootDir;
      for (String component : pathComponents) {
        rootDir = rootDir.resolve(component);
      }

      createDirectories(rootDir);
      log.info("Created protobuf output directory at: {}", rootDir);
      return rootDir;
    } catch (IOException e) {
      log.error("Error creating output directory", e);
      throw e;
    }
  }

  /** Returns true iff the given data type maps to a protobuf scalar type. */
  public static boolean isProtoTypeScalar(CodeGenType dataType, CodeGenUtility util) {
    if (util.isSameRawType(
        dataType.rawType().javaModelType(util.processingEnv()), Optional.class)) {
      CodeGenType innerType = dataType.typeParameters().get(0);
      return isProtoTypeScalar(innerType, util);
    }
    return JAVA_TO_PROTO_SCALAR_TYPES.containsKey(dataType);
  }

  public static boolean isProtoTypeRepeated(CodeGenType dataType) {
    return dataType.canonicalClassName().equals(List.class.getCanonicalName());
  }

  public static boolean isProtoTypeMap(CodeGenType dataType) {
    return dataType.canonicalClassName().equals(Map.class.getCanonicalName());
  }

  /**
   * Resolves the protobuf field type for a given Krystal data type, given the protocol's schema
   * config.
   */
  public static ProtoFieldType getProtobufType(
      CodeGenType dataType, CodeGenUtility util, Element element, ProtoSchemaConfig config) {
    ImmutableList<CodeGenType> typeParameters = dataType.typeParameters();
    TypeMirror javaModelType = dataType.javaModelType(util.processingEnv());
    if (util.isOptional(javaModelType)) {
      return new OptionalFieldType(
          getProtobufType(typeParameters.get(0), util, element, config), util, element);
    } else if (isProtoTypeRepeated(dataType)) {
      if (typeParameters.isEmpty()) {
        throw util.errorAndThrow("Raw list types are not supported by protobuf", element);
      }
      return new RepeatedFieldType(
          getProtobufType(typeParameters.get(0), util, element, config), util, element);
    } else if (isProtoTypeMap(dataType)) {
      if (typeParameters.size() != 2) {
        throw util.errorAndThrow("Raw map types are not supported by protobuf", element);
      }
      return new MapFieldType(
          getProtobufType(typeParameters.get(0), util, element, config),
          getProtobufType(typeParameters.get(1), util, element, config),
          util,
          element);
    } else if (JAVA_TO_PROTO_SCALAR_TYPES.containsKey(dataType)) {
      return JAVA_TO_PROTO_SCALAR_TYPES.get(dataType);
    } else {
      Element javaElement = util.processingEnv().getTypeUtils().asElement(javaModelType);
      if (javaElement != null
          && util.typeExplicitlySupportsProtocol(javaElement, config.protocolClass())
          && TypeName.get(dataType.javaModelType(util.processingEnv()))
              instanceof ClassName modelRootName) {
        if (util.isEnumModelType(javaModelType)) {
          return new EnumFieldType(
              modelRootName.packageName(),
              modelRootName.simpleName() + config.messageSuffix(),
              modelRootName.simpleName(),
              config.fileSuffix());
        }
        return new MessageFieldType(
            modelRootName.packageName(),
            modelRootName.simpleName() + config.messageSuffix(),
            modelRootName.simpleName(),
            config.fileSuffix());
      }
      throw util.errorAndThrow(
          String.format(
              "Unsupported data type: %s. Cannot map to a Protocol Buffers type.", dataType),
          element);
    }
  }

  /**
   * Validates that a vajram's return type conforms to protobuf RPC requirements. RPC methods must
   * return a message type that supports the supplied protocol.
   */
  public static void validateReturnTypeForProtobuf(
      VajramInfo vajramInfo, CodeGenUtility util, Class<? extends ModelProtocol> protocolClass) {
    CodeGenType returnType = vajramInfo.lite().responseType();

    Element typeElement =
        requireNonNull(
            util.processingEnv()
                .getTypeUtils()
                .asElement(returnType.javaModelType(util.processingEnv())));
    SupportedModelProtocols supportedModelProtocols =
        typeElement.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null
        || util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
            .noneMatch(t -> util.isSameRawType(t, protocolClass))) {
      util.error(
          String.format(
              "Vajram '%s' has return type '%s' which is not a supported model protocol. "
                  + "RPC methods must return a message type that is compatible with %s.",
              vajramInfo.vajramName(), returnType, protocolClass.getSimpleName()),
          vajramInfo.vajramClassElem());
    }
  }

  public static List<? extends TypeMirror> getSerializationProtocols(
      @Nullable SupportedModelProtocols supportedSerdeProtocols, VajramCodeGenUtility util) {
    return supportedSerdeProtocols == null
        ? List.of()
        : util.codegenUtil().getTypesFromAnnotationMember(supportedSerdeProtocols::value);
  }

  /** Validates the Vajram for protobuf compatibility. Throws compile errors if validations fail. */
  public static void validateProtobufCompatibility(
      VajramInfo vajramInfo, CodeGenUtility util, Class<? extends ModelProtocol> protocolClass) {
    validateReturnTypeForProtobuf(vajramInfo, util, protocolClass);
  }
}
