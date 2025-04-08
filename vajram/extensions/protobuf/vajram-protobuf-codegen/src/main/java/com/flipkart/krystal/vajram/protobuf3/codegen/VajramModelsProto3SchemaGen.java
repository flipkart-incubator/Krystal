package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.datatypes.JavaTypes.BOOLEAN;
import static com.flipkart.krystal.datatypes.JavaTypes.BYTE;
import static com.flipkart.krystal.datatypes.JavaTypes.DOUBLE;
import static com.flipkart.krystal.datatypes.JavaTypes.FLOAT;
import static com.flipkart.krystal.datatypes.JavaTypes.INT;
import static com.flipkart.krystal.datatypes.JavaTypes.LIST_RAW;
import static com.flipkart.krystal.datatypes.JavaTypes.LONG;
import static com.flipkart.krystal.datatypes.JavaTypes.MAP_RAW;
import static com.flipkart.krystal.datatypes.JavaTypes.SHORT;
import static com.flipkart.krystal.datatypes.JavaTypes.STRING;
import com.flipkart.krystal.datatypes.JavaType;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.MODELS_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.MODELS_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.VAJRAM_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.VAJRAM_REQ_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.VAJRAM_REQ_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.getPackageName;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.javaPackageToProtoPackageName;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.createDirectories;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.lattice.annotations.RemotelyInvocable;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.serial.SupportedSerdeProtocols;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.GivenFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.models.VajramValidationException;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.data.IfNoValue.Strategy;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Code generator which generates protobuf schema for a request proto containing the input facets of
 * a vajram
 */
@Slf4j
class VajramModelsProto3SchemaGen implements CodeGenerator {

  private final VajramCodeGenContext creationContext;
  private final Utils util;

  /** Map of Java DataType objects to their Protocol Buffers type mapping metadata */
  private static final Map<DataType<?>, String> JAVA_TO_PROTO_SCALAR_TYPES =
      ImmutableMap.<DataType<?>, String>builder()
          .put(BOOLEAN, "bool")
          .put(BYTE, "bytes")
          .put(SHORT, "int32")
          .put(INT, "int32")
          .put(LONG, "int64")
          .put(FLOAT, "float")
          .put(DOUBLE, "double")
          .put(STRING, "string")
          .put(JavaType.create(ByteString.class), "bytes")
          .build();

  public VajramModelsProto3SchemaGen(VajramCodeGenContext creationContext) {
    this.creationContext = creationContext;
    this.util = creationContext.util();
  }

  @Override
  public void generate() throws VajramValidationException {
    if (!isApplicable(creationContext, util)) {
      return;
    }
    validate(creationContext, util);
    generateProtobufSchema(creationContext.vajramInfo());
  }

  private static boolean isApplicable(VajramCodeGenContext creationContext, Utils util) {
    if (!CodegenPhase.MODELS.equals(creationContext.codegenPhase())) {
      util.note("Skipping protobuf codegen since current phase is not MODELS");
      return false;
    }
    return isProto3Applicable(creationContext, util);
  }

  /**
   * Checks if the code generator is applicable to the current Vajram. This method only checks
   * conditions that determine whether we should proceed with generation.
   *
   * @return true if the code generator is applicable, false otherwise
   */
  static boolean isProto3Applicable(VajramCodeGenContext creationContext, Utils util) {

    TypeElement vajramClass = creationContext.vajramInfo().vajramClass();
    RemotelyInvocable remotelyInvocable = vajramClass.getAnnotation(RemotelyInvocable.class);
    if (remotelyInvocable == null) {
      util.note(
          "Skipping class '%s' since remote invocation is not enabled"
              .formatted(vajramClass.getQualifiedName()));
      return false;
    }

    SupportedSerdeProtocols supportedSerdeProtocols =
        vajramClass.getAnnotation(SupportedSerdeProtocols.class);
    List<? extends TypeMirror> serializationProtocols =
        getSerializationProtocols(supportedSerdeProtocols, util);
    if (serializationProtocols.stream()
        .noneMatch(
            serializationProtocol -> util.isSameType(serializationProtocol, Protobuf3.class))) {
      util.note(
          "Skipping class '%s' since Protobuf3 is not one of the intended serialization protocols : %s "
              .formatted(vajramClass.getQualifiedName(), serializationProtocols));
      return false;
    }

    return true;
  }

  /**
   * Validates the Vajram for protobuf compatibility. Throws exceptions if validations fail.
   *
   * @throws VajramValidationException if validation fails
   */
  static void validate(VajramCodeGenContext creationContext, Utils util)
      throws VajramValidationException {
    // Validate that the Vajram's return type conforms to protobuf RPC requirements
    validateReturnTypeForProtobuf(creationContext, util);
  }

  private static List<? extends TypeMirror> getSerializationProtocols(
      @Nullable SupportedSerdeProtocols supportedSerdeProtocols, Utils util) {
    return supportedSerdeProtocols == null
        ? List.of()
        : util.getTypesFromAnnotationMember(supportedSerdeProtocols::value);
  }

  private void generateProtobufSchema(VajramInfo vajramInfo) {
    String vajramName = vajramInfo.vajramClass().getSimpleName().toString();
    String packageName = vajramInfo.lite().packageName();
    String reqProtoFileName = vajramName + VAJRAM_REQ_PROTO_FILE_SUFFIX;
    String vajramProtoFileName = vajramName + VAJRAM_PROTO_FILE_SUFFIX;

    try {
      // Create output directory if it doesn't exist
      Path outputDir = createOutputDirectory();

      // Generate request proto file content
      String reqProtoContent = generateRequestProtoFileContent(vajramInfo, packageName);

      // Write request proto file
      Path reqProtoFilePath = outputDir.resolve(reqProtoFileName);
      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(reqProtoFilePath))) {
        out.println(reqProtoContent);
      }

      log.info("Generated request protobuf schema file: {}", reqProtoFilePath);

      // Check if the vajram is remotely invocable
      TypeElement vajramClass = vajramInfo.vajramClass();
      RemotelyInvocable remotelyInvocable = vajramClass.getAnnotation(RemotelyInvocable.class);

      if (remotelyInvocable != null) {
        // Generate service proto file content
        String serviceProtoContent = generateServiceProtoFileContent(vajramInfo, packageName);

        // Write service proto file
        Path serviceProtoFilePath = outputDir.resolve(vajramProtoFileName);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(serviceProtoFilePath))) {
          out.println(serviceProtoContent);
        }

        log.info("Generated service protobuf schema file: {}", serviceProtoFilePath);
      }
    } catch (IOException e) {
      util.error(
          String.format("Error generating protobuf schema for %s: %s", vajramName, e.getMessage()),
          vajramInfo.vajramClass());
    }
  }

  private Path createOutputDirectory() throws IOException {
    try {

      // Get the location where generated source files should be placed
      Path currentDir =
          util.detectSourceOutputPath(creationContext.vajramInfo().vajramClass()).getParent();
      // Navigate to find the 'java' directory to create a parallel 'protobuf'
      // directory
      // Ex: "/generated/sources/annotationProcessor/java/main" becomes
      // "/generated/sources/annotationProcessor/protobuf/main"
      // Keep track of the path components we traverse
      List<String> pathComponents = new ArrayList<>();
      Path javaDir = null;

      while (currentDir != null && currentDir.getFileName() != null) {
        if (currentDir.getFileName().toString().equals("java")) {
          javaDir = currentDir;
          break;
        }
        // Add directory name to the beginning of our list (we're going up)
        pathComponents.add(0, currentDir.getFileName().toString());
        currentDir = currentDir.getParent();
      }

      if (javaDir == null) {
        throw util.errorAndThrow("Failed to find 'java' directory in the source path", null);
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
   * Generates the content for the request proto file that contains the message definition for the
   * Vajram's input facets.
   *
   * @param vajramInfo The Vajram information
   * @param packageName The package name
   * @return The content for the request proto file
   */
  private String generateRequestProtoFileContent(VajramInfo vajramInfo, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String vajramId = vajramInfo.vajramName();
    String vajramClassName = vajramInfo.vajramClass().getQualifiedName().toString();

    // Add auto-generated comment
    protoBuilder
        .append("// AUTOMATICALLY GENERATED - DO NOT EDIT!\n")
        .append("// This schema is auto-generated by Krystal's code generator.\n")
        .append("// It models the request for Vajram with ID: ")
        .append(vajramId)
        .append("\n")
        .append("// Source: ")
        .append(vajramClassName)
        .append("\n")
        .append("// Any manual edits to this file will be overwritten.\n\n");

    // Add syntax, package, and options
    protoBuilder.append("syntax = \"proto3\";\n\n");

    String currentPackageName = javaPackageToProtoPackageName(packageName);

    protoBuilder.append("package ").append(currentPackageName).append(";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    protoBuilder.append("option java_multiple_files = true;\n");
    protoBuilder
        .append("option java_outer_classname = \"")
        .append(vajramId)
        .append(VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX + "\";\n\n");

    // Add message definition
    protoBuilder.append("message ").append(vajramId).append(VAJRAM_REQ_PROTO_MSG_SUFFIX + " {\n");

    // Add fields from input facets using SerialId annotation for field numbers
    Set<Integer> usedFieldNumbers = new HashSet<>();

    for (GivenFacetModel facet :
        vajramInfo.givenFacets().stream().filter(f -> f.facetTypes().contains(INPUT)).toList()) {
      // Get the SerialId annotation from the facet field
      SerialId serialId = facet.facetField().getAnnotation(SerialId.class);
      if (serialId == null) {
        throw util.errorAndThrow(
            String.format(
                "Missing @SerialId annotation on input '%s' in Vajram '%s'",
                facet.name(), vajramId),
            facet.facetField());
      }

      // Get the field number from the annotation
      int fieldNumber = serialId.value();

      // Validate the field number
      if (fieldNumber <= 0) {
        throw util.errorAndThrow(
            String.format(
                "Invalid SerialId %d for input '%s' in Vajram '%s'. SerialId must be positive.",
                fieldNumber, facet.name(), vajramId),
            facet.facetField());
      }

      // Check for duplicate field numbers
      if (!usedFieldNumbers.add(fieldNumber)) {
        throw util.errorAndThrow(
            String.format(
                "Duplicate SerialId %d for input '%s' in Vajram '%s'",
                fieldNumber, facet.name(), vajramId),
            facet.facetField());
      }

      String fieldType = getProtobufType(facet.dataType(), util);
      String fieldName = facet.name();

      // Check if the field has the @Mandatory annotation
      IfNoValue ifNoValue = facet.facetField().getAnnotation(IfNoValue.class);
      boolean isOptional = true; // Default to optional for proto3

      // In proto3, the 'optional' keyword is needed for all primitive types to check
      // presence. This includes numeric types, booleans, strings, bytes, and enums. If 'optional'
      // keyword is not present on primitives, then we cannot check for presence of value as always
      // default value is returned.
      // Message types (complex objects) and OneOfs don't need optional as they are always optional
      // (support presence checks)
      // Repeated fields, and maps support the optional keyword (they can never be
      // checked for presence)
      // Ref: https://protobuf.dev/programming-guides/field_presence/#presence-in-proto3-apis
      if (ifNoValue != null) {
        // Field has @Mandatory annotation
        Strategy ifNoValueStrategy = ifNoValue.then();

        TypeMirror rawType =
            util.processingEnv()
                .getTypeUtils()
                .erasure(facet.dataType().javaModelType(util.processingEnv()));
        boolean isRepeated = util.isRawAssignable(rawType, List.class);
        boolean isMap = util.isRawAssignable(rawType, Map.class);
        if (!ifNoValueStrategy.usePlatformDefault() && (isRepeated || isMap)) {
          // Proto3 cannot enforce mandatory fields with FAIL strategy for repeated and
          // map fields
          throw util.errorAndThrow(
              String.format(
                  "Input '%s' in Vajram '%s' is a %s field, and has @IfNoValue(then=%s) which is not supported in protobuf3. "
                      + "Use a different IfNoValue strategy or remove @IfNoValue annotation.",
                  facet.name(), vajramId, isRepeated ? "repeated" : "map", ifNoValueStrategy),
              facet.facetField());
        } else if (ifNoValueStrategy.usePlatformDefault()) {
          // If the strategy allows defaulting, we can make it a required field in proto3
          isOptional = false;
        }
      }

      // Add documentation as comments if available
      String documentation = facet.documentation();
      if (documentation != null && !documentation.trim().isEmpty()) {
        // Format the documentation as a proto comment
        // Split by newlines and add proper indentation and comment markers
        Iterable<String> docLines = Splitter.on('\n').split(documentation);
        for (String line : docLines) {
          protoBuilder.append("  // ").append(line.trim()).append("\n");
        }
      }

      protoBuilder.append("  ");

      // Add 'optional' keyword if needed
      // Note: repeated and map fields don't need the optional keyword
      if (isOptional
          && isProtoTypeScalar(facet.dataType())
          && !isProtoTypeRepeated(facet.dataType())
          && !isProtoTypeMap(facet.dataType())) {
        protoBuilder.append("optional ");
      }

      // For repeated and map fields, the 'repeated' or 'map<>' prefix is already included in
      // fieldType
      protoBuilder
          .append(fieldType)
          .append(" ")
          .append(fieldName)
          .append(" = ")
          .append(fieldNumber)
          .append(";\n");
    }

    protoBuilder.append("}\n");

    return protoBuilder.toString();
  }

  /**
   * Generates the content for the service proto file that contains the service definition for the
   * remotely invocable Vajram.
   *
   * @param vajramInfo The Vajram information
   * @param packageName The package name
   * @return The content for the service proto file
   */
  private String generateServiceProtoFileContent(VajramInfo vajramInfo, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String vajramId = vajramInfo.vajramName();
    String vajramClassName = vajramInfo.vajramClass().getQualifiedName().toString();

    // Get the response type name (without package) to use in the service definition
    String responseTypeName =
        ProtoGenUtils.getSimpleClassName(vajramInfo.lite().responseType().canonicalClassName());

    // Add auto-generated comment
    protoBuilder
        .append("// AUTOMATICALLY GENERATED - DO NOT EDIT!\n")
        .append("// This schema is auto-generated by Krystal's code generator.\n")
        .append("// It defines the service for Vajram with ID: ")
        .append(vajramId)
        .append("\n")
        .append("// Source: ")
        .append(vajramClassName)
        .append("\n")
        .append("// Any manual edits to this file will be overwritten.\n\n");

    // Add syntax, package, and options
    protoBuilder.append("syntax = \"proto3\";\n\n");

    String currentPackageName = javaPackageToProtoPackageName(packageName);

    protoBuilder.append("package ").append(currentPackageName).append(";\n\n");

    // Add imports for the request and response messages
    protoBuilder
        .append("import \"")
        .append(vajramId)
        .append(VAJRAM_REQ_PROTO_FILE_SUFFIX)
        .append("\";\n");

    protoBuilder
        .append("import \"")
        .append(responseTypeName)
        .append(MODELS_PROTO_FILE_SUFFIX)
        .append("\";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    protoBuilder.append("option java_multiple_files = true;\n\n");

    // Add documentation for the service
    protoBuilder.append("// Service definition for the remotely invocable Vajram\n");

    // Create the service definition
    protoBuilder.append("service ").append(vajramId).append(" {\n");

    // Add the RPC method
    protoBuilder.append("  // Execute the Vajram remotely\n");
    protoBuilder
        .append("  rpc Execute(")
        .append(vajramId)
        .append(VAJRAM_REQ_PROTO_MSG_SUFFIX)
        .append(") returns (")
        .append(
            getPackageName(vajramInfo.lite().responseType().canonicalClassName())
                .map(ProtoGenUtils::javaPackageToProtoPackageName)
                .filter(
                    // We don't need to prefix messages with package
                    // names with the protos are in the same package
                    s -> !s.equals(currentPackageName))
                .map(s -> s + ".")
                .orElse(""))
        .append(responseTypeName)
        .append(MODELS_PROTO_MSG_SUFFIX)
        .append(");\n");

    // Close the service definition
    protoBuilder.append("}\n");

    return protoBuilder.toString();
  }

  /**
   * Checks if the given data type is a Protocol Buffers scalar type
   *
   * @param dataType The data type to check
   * @return true if the data type is a Protocol Buffers scalar type, false otherwise
   */
  static boolean isProtoTypeScalar(DataType<?> dataType) {
    return JAVA_TO_PROTO_SCALAR_TYPES.containsKey(dataType);
  }

  static boolean isProtoTypeRepeated(DataType<?> dataType) {
    return dataType.getRawType().equals(LIST_RAW);
  }

  static boolean isProtoTypeMap(DataType<?> dataType) {
    return dataType.getRawType().equals(MAP_RAW);
  }

  /**
   * Gets the Protocol Buffers type for a given data type
   *
   * @param dataType The data type to get the Protocol Buffers type for
   * @return The Protocol Buffers type as a string
   */
  static String getProtobufType(DataType<?> dataType, Utils util) {
    if (isProtoTypeScalar(dataType)) {
      return JAVA_TO_PROTO_SCALAR_TYPES.get(dataType);
    } else if (isProtoTypeRepeated(dataType)) {
      // Handle List types as repeated fields
      DataType<?> elementType = dataType.typeParameters().get(0);
      return "repeated " + getProtobufType(elementType, util);
    } else if (isProtoTypeMap(dataType)) {
      // Handle Map types as map fields
      List<DataType<?>> typeParams = dataType.typeParameters();
      DataType<?> keyType = typeParams.get(0);
      DataType<?> valueType = typeParams.get(1);

      // Validate that key type is allowed in proto3 maps
      validateMapKeyType(keyType, util);

      return "map<"
          + getProtobufType(keyType, util)
          + ", "
          + getProtobufType(valueType, util)
          + ">";
    } else {
      // Throw an error for unsupported types
      throw util.errorAndThrow(
          String.format(
              "Unsupported data type: %s. Cannot map to a Protocol Buffers type.", dataType),
          null);
    }
  }

  /**
   * Validates that the key type for a map is allowed in proto3. Proto3 only allows integral types,
   * string, and bool as map keys
   *
   * @param keyType The key type to validate
   */
  private static void validateMapKeyType(DataType<?> keyType, Utils util) {
    // In proto3, map keys can only be integral types, string, or bool
    boolean isValidKeyType =
        keyType.equals(INT)
            || keyType.equals(LONG)
            || keyType.equals(BOOLEAN)
            || keyType.equals(STRING)
            || keyType.equals(SHORT)
            || keyType.equals(BYTE);

    if (!isValidKeyType) {
      throw util.errorAndThrow(
          String.format(
              "Invalid map key type: %s. Proto3 only allows integral types, string, and bool as map keys.",
              keyType),
          null);
    }
  }

  /**
   * Validates that the Vajram's return type conforms to protobuf RPC requirements. In protobuf, RPC
   * methods must return message types, not scalar values, repeated fields, or map fields. The
   * return type must be a single message type.
   *
   * @throws VajramValidationException if the return type is not valid for protobuf RPC
   */
  private static void validateReturnTypeForProtobuf(
      VajramCodeGenContext creationContext, Utils util) throws VajramValidationException {
    VajramInfo vajramInfo = creationContext.vajramInfo();
    DataType<?> returnType = vajramInfo.lite().responseType();

    // Check if the return type is a scalar type
    if (isProtoTypeScalar(returnType)) {
      throw util.errorAndThrow(
          String.format(
              "Vajram '%s' has scalar return type '%s' which is not allowed in protobuf RPC. "
                  + "RPC methods must return message types, not scalar values. "
                  + "Consider wrapping the scalar value in a message type.",
              vajramInfo.vajramName(), returnType),
          vajramInfo.vajramClass());
    }

    // Check if the return type is a repeated field
    if (isProtoTypeRepeated(returnType)) {
      throw util.errorAndThrow(
          String.format(
              "Vajram '%s' has repeated field return type '%s' which is not allowed in protobuf RPC. "
                  + "RPC methods must return a single message type, not repeated fields. "
                  + "Consider wrapping the repeated field in a message type.",
              vajramInfo.vajramName(), returnType),
          vajramInfo.vajramClass());
    }

    // Check if the return type is a map field
    if (isProtoTypeMap(returnType)) {
      throw util.errorAndThrow(
          String.format(
              "Vajram '%s' has map field return type '%s' which is not allowed in protobuf RPC. "
                  + "RPC methods must return a single message type, not map fields. "
                  + "Consider wrapping the map field in a message type.",
              vajramInfo.vajramName(), returnType),
          vajramInfo.vajramClass());
    }
  }
}
