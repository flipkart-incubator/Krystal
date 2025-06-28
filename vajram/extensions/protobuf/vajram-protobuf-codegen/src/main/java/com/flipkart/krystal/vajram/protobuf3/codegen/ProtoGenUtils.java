package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.BOOLEAN;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.DOUBLE;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.FLOAT;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.INT;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.LONG;
import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.STRING;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.BOOL_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.BYTES_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.DOUBLE_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.FLOAT_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.SINT32_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.SINT64_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType.STRING_P;
import static com.flipkart.krystal.vajram.protobuf3.codegen.types.StandardProto3Type.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.MapFieldType;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.OptionalFieldType;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoFieldType;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoScalarType;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.RepeatedFieldType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class ProtoGenUtils {

  /** Map of Java DataType objects to their Protocol Buffers type mapping metadata */
  private static final Map<CodeGenType, ProtoScalarType> JAVA_TO_PROTO_SCALAR_TYPES =
      ImmutableMap.<CodeGenType, ProtoScalarType>builder()
          .put(BOOLEAN, BOOL_P)
          .put(INT, SINT32_P)
          .put(LONG, SINT64_P)
          .put(FLOAT, FLOAT_P)
          .put(DOUBLE, DOUBLE_P)
          .put(STRING, STRING_P)
          .put(BYTE_STRING, BYTES_P)
          .build();

  public static String getSimpleClassName(String canonicalClassName) {
    String typeName = canonicalClassName;

    // Extract the simple name from the fully qualified name
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
   * Checks if the code generator is applicable to the current Vajram. This method only checks
   * conditions that determine whether we should proceed with generation.
   *
   * @return true if the code generator is applicable, false otherwise
   */
  public static boolean isProto3Applicable(VajramInfo vajramInfo, VajramCodeGenUtility util) {

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

    SupportedModelProtocols supportedSerdeProtocols =
        vajramClass.getAnnotation(SupportedModelProtocols.class);
    List<? extends TypeMirror> serializationProtocols =
        getSerializationProtocols(supportedSerdeProtocols, util);
    if (serializationProtocols.stream()
        .noneMatch(
            serializationProtocol ->
                util.codegenUtil().isSameRawType(serializationProtocol, Protobuf3.class))) {
      CharSequence message =
          "Skipping class '%s' since Protobuf3 is not one of the intended serialization protocols : %s "
              .formatted(vajramClass.getQualifiedName(), serializationProtocols);
      util.codegenUtil().note(message);
      return false;
    }

    return true;
  }

  public static Path createOutputDirectory(Path sourceOutputLocation, CodeGenUtility util)
      throws IOException {
    try {

      // Navigate to find the 'java' directory to create a parallel 'protobuf'
      // directory
      // Ex: "/generated/sources/annotationProcessor/java/main" becomes
      // "/generated/sources/annotationProcessor/protobuf/main"
      // Keep track of the path components we traverse
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
        // Add directory name to the beginning of our list (we're going up)
        pathComponents.add(0, sourceOutputLocation.getFileName().toString());
        sourceOutputLocation = parent;
      }

      if (javaDir == null) {
        throw util.errorAndThrow("Failed to find 'java' directory in the source path");
      }

      // Create a parallel 'protobuf' directory at the same level as 'java'
      Path protoRootDir = checkNotNull(javaDir.getParent()).resolve("protobuf");

      // Reconstruct the subdirectory structure
      Path rootDir = protoRootDir;
      for (String component : pathComponents) {
        rootDir = rootDir.resolve(component);
      }

      // Create protobuf output directory
      createDirectories(rootDir);
      log.info("Created protobuf output directory at: {}", rootDir);
      return rootDir;
    } catch (IOException e) {
      log.error("Error creating output directory", e);
      throw e;
    }
  }

  /**
   * Checks if the given data type is a Protocol Buffers scalar type
   *
   * @param dataType The data type to check
   * @param util Code gen utility
   * @return true if the data type is a Protocol Buffers scalar type, false otherwise
   */
  static boolean isProtoTypeScalar(CodeGenType dataType, CodeGenUtility util) {
    if (util.isSameRawType(
        dataType.rawType().javaModelType(util.processingEnv()), Optional.class)) {
      // Extract the inner type parameter from Optional
      CodeGenType innerType = dataType.typeParameters().get(0);
      // Get the protobuf type for the inner type
      return isProtoTypeScalar(innerType, util);
    }
    return JAVA_TO_PROTO_SCALAR_TYPES.containsKey(dataType);
  }

  static boolean isProtoTypeRepeated(CodeGenType dataType) {
    return dataType.canonicalClassName().equals(List.class.getCanonicalName());
  }

  static boolean isProtoTypeMap(CodeGenType dataType) {
    return dataType.canonicalClassName().equals(Map.class.getCanonicalName());
  }

  /**
   * Gets the Protocol Buffers type for a given data type
   *
   * @param dataType The data type to get the Protocol Buffers type for
   * @param element The element whose type needs to be mapped to a protobuf type
   * @return The Protocol Buffers type as a string
   */
  static ProtoFieldType getProtobufType(
      CodeGenType dataType, CodeGenUtility util, Element element) {
    // Check if the type is an Optional
    ImmutableList<CodeGenType> typeParameters = dataType.typeParameters();
    if (util.isOptional(dataType.javaModelType(util.processingEnv()))) {
      // Extract the inner type parameter from Optional
      // Get the protobuf type for the inner type
      return new OptionalFieldType(
          getProtobufType(typeParameters.get(0), util, element), util, element);
    } else if (isProtoTypeRepeated(dataType)) {
      if (typeParameters.isEmpty()) {
        throw util.errorAndThrow("Raw list types are not supported by protobuf", element);
      }
      // Handle List types as repeated fields
      return new RepeatedFieldType(
          getProtobufType(typeParameters.get(0), util, element), util, element);
    } else if (isProtoTypeMap(dataType)) {
      if (typeParameters.size() != 2) {
        throw util.errorAndThrow("Raw map types are not supported by protobuf", element);
      }
      // Handle Map types as map fields
      return new MapFieldType(
          getProtobufType(typeParameters.get(0), util, element),
          getProtobufType(typeParameters.get(1), util, element),
          util,
          element);
    } else if (JAVA_TO_PROTO_SCALAR_TYPES.containsKey(dataType)) {
      return JAVA_TO_PROTO_SCALAR_TYPES.get(dataType);
    } else {
      // Throw an error for unsupported types
      throw util.errorAndThrow(
          String.format(
              "Unsupported data type: %s. Cannot map to a Protocol Buffers type.", dataType),
          element);
    }
  }

  /**
   * Validates that the Vajram's return type conforms to protobuf RPC requirements. In protobuf, RPC
   * methods must return message types, not scalar values, repeated fields, or map fields. The
   * return type must be a single message type.
   */
  public static void validateReturnTypeForProtobuf(VajramInfo vajramInfo, CodeGenUtility util) {
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
            .noneMatch(t -> util.isSameRawType(t, Protobuf3.class))) {
      util.error(
          String.format(
              "Vajram '%s' has return type '%s' which is not a supported model protocol. "
                  + "RPC methods must return a message type that is compatible with Protobuf3.",
              vajramInfo.vajramName(), returnType),
          vajramInfo.vajramClassElem());
    }
  }

  static List<? extends TypeMirror> getSerializationProtocols(
      @Nullable SupportedModelProtocols supportedSerdeProtocols, VajramCodeGenUtility util) {
    return supportedSerdeProtocols == null
        ? List.of()
        : util.codegenUtil().getTypesFromAnnotationMember(supportedSerdeProtocols::value);
  }

  /** Validates the Vajram for protobuf compatibility. Throws exceptions if validations fail. */
  static void validateProtobufCompatibility(VajramInfo vajramInfo, CodeGenUtility util) {
    // Validate that the Vajram's return type conforms to protobuf RPC requirements
    validateReturnTypeForProtobuf(vajramInfo, util);
  }
}
