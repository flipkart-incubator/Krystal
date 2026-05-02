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
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
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

  /**
   * Converts a camelCase identifier to the lower_snake_case form expected by protobuf field names
   * (and required by the editions 2024 naming-style enforcement). Examples:
   *
   * <ul>
   *   <li>{@code "mandatoryInt"} → {@code "mandatory_int"}
   *   <li>{@code "optionalByteString"} → {@code "optional_byte_string"}
   *   <li>{@code "string"} → {@code "string"} (no change)
   *   <li>{@code "URL"} → {@code "u_r_l"} - acronyms get split per char (acceptable; Krystal model
   *       methods avoid all-uppercase acronyms by convention)
   * </ul>
   */
  public static String toSnakeCase(String camelCase) {
    return UPPER_CAMEL.to(LOWER_UNDERSCORE, camelCase);
  }

  /**
   * Strips underscores from a Krystal model identifier so it conforms to the protobuf TitleCase
   * convention required for message and enum names in editions 2024+. Used only when computing
   * proto schema names; Java class names that retain the underscores (e.g. the {@code _Immut}
   * wrapper interfaces) are unaffected.
   *
   * <p>Examples: {@code "Foo_Req"} → {@code "FooReq"}, {@code "Status"} → {@code "Status"}.
   */
  public static String toTitleCaseProtoName(String modelRootName) {
    return modelRootName.replace("_", "");
  }

  /**
   * Converts a Java package name (which may contain camelCase segments) to the lower_snake_case
   * form required by editions 2024+. Each dot-separated segment is split on uppercase boundaries
   * and lowercased. {@code option java_package} retains the original Java package - only the proto
   * {@code package} declaration is affected.
   *
   * <p>Example: {@code "com.foo.sampleProtoService"} → {@code "com.foo.sample_proto_service"}.
   */
  public static String toLowerSnakeCasePackage(String javaPackage) {
    if (javaPackage.isEmpty()) {
      return javaPackage;
    }
    StringBuilder out = new StringBuilder(javaPackage.length() + 8);
    String[] segments = javaPackage.split("\\.");
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        out.append('.');
      }
      out.append(toSnakeCase(segments[i]));
    }
    return out.toString();
  }

  public static Optional<String> getPackageName(String responseTypeName) {
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
      // Java Optional<T> is the model-level signal that this field is presence-aware. Let the
      // protocol's presence wrapper decide whether that needs to be reflected in the .proto schema
      // (proto3: yes, emit `optional`; editions 2024+: no, all singular fields already have
      // explicit presence).
      ProtoFieldType inner = getProtobufType(typeParameters.get(0), util, element, config);
      return config.presenceWrapper().wrap(inner, util, element);
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
        // Strip underscores from the model name for the proto type reference - proto messages and
        // enums must be TitleCase. The file name keeps the original Java simple name since the
        // file path doesn't have to obey TitleCase.
        String protoTypeName =
            toTitleCaseProtoName(modelRootName.simpleName()) + config.messageSuffix();
        if (util.isEnumModelType(javaModelType)) {
          return new EnumFieldType(
              modelRootName.packageName(),
              protoTypeName,
              modelRootName.simpleName(),
              config.fileSuffix());
        }
        return new MessageFieldType(
            modelRootName.packageName(),
            protoTypeName,
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
