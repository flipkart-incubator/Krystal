package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.createOutputDirectory;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.getProtobufType;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProto3Applicable;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeMap;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeRepeated;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeScalar;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.validateProtobufCompatibility;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.VAJRAM_REQ_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.VAJRAM_REQ_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import lombok.extern.slf4j.Slf4j;

/**
 * Code generator which generates protobuf schema for a request proto containing the input facets of
 * a vajram
 */
@Slf4j
class VajramModelsProto3SchemaGen implements CodeGenerator {

  private final VajramCodeGenContext creationContext;
  private final VajramCodeGenUtility util;

  public VajramModelsProto3SchemaGen(VajramCodeGenContext creationContext) {
    this.creationContext = creationContext;
    this.util = creationContext.util();
  }

  @Override
  public void generate() {
    if (!isApplicable(creationContext, util)) {
      return;
    }
    validateProtobufCompatibility(creationContext.vajramInfo(), util.codegenUtil());
    generateProtobufSchema(creationContext.vajramInfo());
  }

  private static boolean isApplicable(
      VajramCodeGenContext creationContext, VajramCodeGenUtility util) {
    if (!MODELS.equals(creationContext.codegenPhase())) {
      util.codegenUtil().note("Skipping protobuf schema codegen since current phase is not MODELS");
      return false;
    }
    return isProto3Applicable(creationContext.vajramInfo(), util);
  }

  private void generateProtobufSchema(VajramInfo vajramInfo) {
    String vajramName = vajramInfo.vajramClassElem().getSimpleName().toString();
    String packageName = vajramInfo.lite().packageName();
    String reqProtoFileName = vajramName + VAJRAM_REQ_PROTO_FILE_SUFFIX;

    try {
      // Create output directory if it doesn't exist
      Path outputDir =
          createOutputDirectory(
              util.detectSourceOutputPath(vajramInfo.vajramClassElem()), util.codegenUtil());

      // Generate request proto file content
      String reqProtoContent = generateRequestProtoFileContent(vajramInfo, packageName);

      // Write request proto file
      Path reqProtoFilePath = outputDir.resolve(reqProtoFileName);
      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(reqProtoFilePath))) {
        out.println(reqProtoContent);
      }

      log.info("Generated request protobuf schema file: {}", reqProtoFilePath);

    } catch (IOException e) {
      String message =
          String.format("Error generating protobuf schema for %s: %s", vajramName, e.getMessage());
      util.codegenUtil().error(message, vajramInfo.vajramClassElem());
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
    String vajramClassName = vajramInfo.vajramClassElem().getQualifiedName().toString();

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

    String currentPackageName = packageName;

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

    for (DefaultFacetModel facet :
        vajramInfo.givenFacets().stream().filter(f -> f.facetTypes().contains(INPUT)).toList()) {
      // Get the SerialId annotation from the facet field
      SerialId serialId = facet.facetField().getAnnotation(SerialId.class);
      int fieldNumber;
      if (serialId == null) {
        util.codegenUtil()
            .error(
                String.format(
                    "Missing @SerialId annotation on input '%s' in Vajram '%s'",
                    facet.name(), vajramId),
                facet.facetField());
        fieldNumber = -1;
      } else {

        // Get the field number from the annotation
        fieldNumber = serialId.value();
      }

      // Validate the field number
      if (fieldNumber <= 0) {
        util.codegenUtil()
            .error(
                String.format(
                    "Invalid SerialId %d for input '%s' in Vajram '%s'. SerialId must be positive.",
                    fieldNumber, facet.name(), vajramId),
                facet.facetField());
      }

      // Check for duplicate field numbers
      if (!usedFieldNumbers.add(fieldNumber)) {
        util.codegenUtil()
            .error(
                String.format(
                    "Duplicate SerialId %d for input '%s' in Vajram '%s'",
                    fieldNumber, facet.name(), vajramId),
                facet.facetField());
      }

      // Check if the field has the @Mandatory annotation
      IfAbsent ifAbsent = facet.facetField().getAnnotation(IfAbsent.class);
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
      if (ifAbsent != null) {
        // Field has @Mandatory annotation
        IfAbsentThen ifAbsentThen = ifAbsent.value();

        TypeMirror rawType =
            util.processingEnv()
                .getTypeUtils()
                .erasure(facet.dataType().javaModelType(util.processingEnv()));
        boolean isRepeated = util.codegenUtil().isRawAssignable(rawType, List.class);
        boolean isMap = util.codegenUtil().isRawAssignable(rawType, Map.class);
        if (!ifAbsentThen.usePlatformDefault() && (isRepeated || isMap)) {
          // Proto3 cannot enforce mandatory fields with FAIL strategy for repeated and
          // map fields
          String message =
              String.format(
                  "Input '%s' in Vajram '%s' is a %s field, and has @IfAbsent(%s) which is not supported in protobuf3. "
                      + "Use a different IfAbsent strategy or remove @IfAbsent annotation.",
                  facet.name(), vajramId, isRepeated ? "repeated" : "map", ifAbsentThen);
          util.codegenUtil().error(message, facet.facetField());
        } else if (ifAbsentThen.usePlatformDefault()) {
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
          && isProtoTypeScalar(facet.dataType(), util.codegenUtil())
          && !isProtoTypeRepeated(facet.dataType())
          && !isProtoTypeMap(facet.dataType())) {
        protoBuilder.append("optional ");
      }

      // For repeated and map fields, the 'repeated' or 'map<>' prefix is already included in
      // fieldType
      protoBuilder
          .append(
              getProtobufType(facet.dataType(), util.codegenUtil(), facet.facetField())
                  .typeInProtoFile())
          .append(" ")
          .append(facet.name())
          .append(" = ")
          .append(fieldNumber)
          .append(";\n");
    }

    protoBuilder.append("}\n");

    return protoBuilder.toString();
  }
}
