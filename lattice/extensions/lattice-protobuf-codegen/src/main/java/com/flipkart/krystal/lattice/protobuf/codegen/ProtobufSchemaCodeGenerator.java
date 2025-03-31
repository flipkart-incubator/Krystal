package com.flipkart.krystal.lattice.protobuf.codegen;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.lattice.protobuf.codegen.Constants.PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.lattice.protobuf.codegen.Constants.PROTO_OUTER_CLASS_SUFFIX;
import static com.flipkart.krystal.lattice.protobuf.codegen.Constants.PROTO_SCHEMA_SUFFIX;
import static java.nio.file.Files.createDirectories;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.ext.protobuf.Protobuf3;
import com.flipkart.krystal.lattice.annotations.RemoteInvocation;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Mandatory.IfNotSet;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.GivenFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.models.VajramValidationException;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGeneratorCreationContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenerator;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ProtobufSchemaCodeGenerator implements VajramCodeGenerator {

  private final CodeGeneratorCreationContext creationContext;
  private final Utils util;
  // Mapping Java types to protobuf types
  private static final Map<String, String> JAVA_TO_PROTO_TYPE_MAP =
      ImmutableMap.<String, String>builder()
          .put("java.lang.String", "string")
          .put("java.lang.Integer", "int32")
          .put("java.lang.Long", "int64")
          .put("java.lang.Float", "float")
          .put("java.lang.Double", "double")
          .put("java.lang.Boolean", "bool")
          .put("int", "int32")
          .put("long", "int64")
          .put("float", "float")
          .put("double", "double")
          .put("boolean", "bool")
          .put("byte", "bytes")
          .put("java.lang.Byte", "bytes")
          .build();

  public ProtobufSchemaCodeGenerator(CodeGeneratorCreationContext creationContext) {
    this.creationContext = creationContext;
    this.util = creationContext.util();
  }

  @Override
  public void generate() throws VajramValidationException {
    if (isNotValid()) return;
    generateProtobufSchema(creationContext.vajramInfo());
  }

  private boolean isNotValid() {
    if (!CodegenPhase.MODELS.equals(creationContext.codegenPhase())) {
      util.note("Skipping protobuf codegen since current phase is not MODELS");
      return true;
    }
    TypeElement vajramClass = creationContext.vajramInfo().vajramClass();
    RemoteInvocation remoteInvocation = vajramClass.getAnnotation(RemoteInvocation.class);
    if (remoteInvocation == null || !remoteInvocation.allow()) {
      util.note(
          "Skipping class '%s' since remote invocation is not enabled"
              .formatted(vajramClass.getQualifiedName()));
      return true;
    }
    List<? extends TypeMirror> serializationProtocols = getSerializationProtocols(remoteInvocation);
    if (serializationProtocols.stream()
        .noneMatch(
            serializationProtocol -> util.isSameType(serializationProtocol, Protobuf3.class))) {
      util.note(
          "Skipping class '%s' since Protobuf is not one of the intended serialization protocols : %s "
              .formatted(vajramClass.getQualifiedName(), serializationProtocols));
      return true;
    }
    return false;
  }

  private static List<? extends TypeMirror> getSerializationProtocols(
      RemoteInvocation remoteInvocation) {
    try {
      var ignore = remoteInvocation.serializationProtocols();
    } catch (MirroredTypesException e) {
      return e.getTypeMirrors();
    }
    throw new AssertionError(
        "remoteInvocation.serializationProtocols will definitely throw MirroredTypesException");
  }

  private void generateProtobufSchema(VajramInfo vajramInfo) {
    String vajramName = vajramInfo.vajramClass().getSimpleName().toString();
    String packageName = vajramInfo.lite().packageName();
    String protoFileName = vajramName + PROTO_FILE_SUFFIX;

    try {
      // Create output directory if it doesn't exist
      Path outputDir = createOutputDirectory();

      // Generate proto file content
      String protoContent = generateProtoFileContent(vajramInfo, packageName);

      // Write proto file
      Path protoFilePath = outputDir.resolve(protoFileName);
      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(protoFilePath))) {
        out.println(protoContent);
      }

      log.info("Generated protobuf schema file: {}", protoFilePath);
    } catch (IOException e) {
      util.error(
          String.format("Error generating protobuf schema for %s: %s", vajramName, e.getMessage()),
          vajramInfo.vajramClass());
    }
  }

  private Path createOutputDirectory() throws IOException {
    // Get the location where generated source files should be placed
    try {
      // Create a dummy file to get the location
      FileObject dummyFile =
          util.processingEnv()
              .getFiler()
              .createResource(StandardLocation.SOURCE_OUTPUT, "", "dummy.txt");
      Path sourcePath = Paths.get(dummyFile.toUri());
      dummyFile.delete();

      // Navigate to find the 'java' directory to create a parallel 'protobuf'
      // directory
      // Ex: "/generated/sources/annotationProcessor/java/main" becomes
      // "/generated/sources/annotationProcessor/protobuf/main"

      Path currentDir = sourcePath.getParent();
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
      Path protoRootDir = javaDir.getParent().resolve("protobuf");

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

  private String generateProtoFileContent(VajramInfo vajramInfo, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String vajramName = vajramInfo.vajramName();

    // Add syntax, package, and options
    protoBuilder.append("syntax = \"proto3\";\n\n");

    protoBuilder.append("package ").append(packageName.replace('.', '_')).append(";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    protoBuilder.append("option java_multiple_files = true;\n");
    protoBuilder
        .append("option java_outer_classname = \"")
        .append(vajramName)
        .append(PROTO_OUTER_CLASS_SUFFIX + "\";\n\n");

    // Add message definition
    protoBuilder.append("message ").append(vajramName).append(PROTO_SCHEMA_SUFFIX + " {\n");

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
                facet.name(), vajramName),
            facet.facetField());
      }

      // Get the field number from the annotation
      int fieldNumber = serialId.value();

      // Validate the field number
      if (fieldNumber <= 0) {
        throw util.errorAndThrow(
            String.format(
                "Invalid SerialId %d for input '%s' in Vajram '%s'. SerialId must be positive.",
                fieldNumber, facet.name(), vajramName),
            facet.facetField());
      }

      // Check for duplicate field numbers
      if (!usedFieldNumbers.add(fieldNumber)) {
        throw util.errorAndThrow(
            String.format(
                "Duplicate SerialId %d for input '%s' in Vajram '%s'",
                fieldNumber, facet.name(), vajramName),
            facet.facetField());
      }

      String fieldType = getProtobufType(facet.dataType());
      String fieldName = facet.name();

      // Check if the field has the @Mandatory annotation
      Mandatory mandatory = facet.facetField().getAnnotation(Mandatory.class);
      boolean isOptional = true; // Default to optional for proto3

      if (mandatory != null) {
        // Field has @Mandatory annotation
        IfNotSet ifNotSet = mandatory.ifNotSet();

        if (ifNotSet == IfNotSet.FAIL) {
          // Proto3 cannot enforce mandatory fields with FAIL strategy
          throw util.errorAndThrow(
              String.format(
                  "Input '%s' in Vajram '%s' has @Mandatory with ifNotSet=FAIL which is not supported in protobuf. "
                      + "Use a different ifNotSet strategy or remove @Mandatory annotation.",
                  facet.name(), vajramName),
              facet.facetField());
        } else if (ifNotSet.usePlatformDefault()) {
          // If the strategy allows defaulting, we can make it a required field in proto3
          isOptional = false;
        }
      }

      // In proto3, the 'optional' keyword is needed for all primitive types to check presence
      // This includes numeric types, booleans, strings, bytes, and enums
      // Only message types (complex objects) don't need the optional keyword
      boolean isPrimitiveType =
          fieldType.equals("int32")
              || fieldType.equals("int64")
              || fieldType.equals("float")
              || fieldType.equals("double")
              || fieldType.equals("bool")
              || fieldType.equals("string")
              || fieldType.equals("bytes")
              || fieldType.startsWith("enum");

      protoBuilder.append("  ");

      // Add 'optional' keyword if needed
      if (isOptional && isPrimitiveType) {
        protoBuilder.append("optional ");
      }

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

  private String getProtobufType(DataType<?> dataType) {
    if (dataType instanceof JavaType<?> javaType) {

      // Check if we have a direct mapping
      String protoType = JAVA_TO_PROTO_TYPE_MAP.get(javaType.canonicalClassName());
      if (protoType != null) {
        return protoType;
      }

      // For complex types, default to string (serialized form)
      return "string";
    }

    // Default to string for unknown types
    return "string";
  }
}
