package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.createOutputDirectory;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.getProtobufType;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeMap;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.isProtoTypeRepeated;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_OUTER_CLASS_SUFFIX;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static javax.lang.model.element.ElementKind.INTERFACE;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.codegen.common.generators.SerdeModelValidator;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.OptionalFieldType;
import com.flipkart.krystal.vajram.protobuf3.codegen.types.ProtoFieldType;
import com.google.common.base.Splitter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;

/** Code generator which generates protobuf schema for models derived from a ModelRoot interface. */
@Slf4j
final class Proto3SchemaGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;
  private final ModelRoot modelRoot;

  public Proto3SchemaGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
    this.modelRoot = requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }
    validate();
    generateProtobufSchema();
  }

  private boolean isApplicable() {
    if (!MODELS.equals(codeGenContext.codegenPhase())) {
      util.note("Skipping protobuf models codegen since current phase is not MODELS");
      return false;
    }

    TypeElement modelRootType = codeGenContext.modelRootType();
    if (modelRootType == null) {
      util.note("Skipping protobuf codegen since model root type is null");
      return false;
    }

    // Check if the model root has the ModelRoot annotation
    ModelRoot modelRootAnnotation = modelRootType.getAnnotation(ModelRoot.class);
    if (modelRootAnnotation == null) {
      util.note(
          "Skipping class '%s' since it doesn't have @ModelRoot annotation"
              .formatted(modelRootType.getQualifiedName()));
      return false;
    }

    return true;
  }

  private void validate() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    validateModelType(modelRootType, util);
    // Validate purity (if required by protocol) and nested model protocol support
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);
    new SerdeModelValidator(util, modelRootType, Protobuf3.PROTOBUF_3).validate(modelMethods);
  }

  static void validateModelType(TypeElement modelRootType, CodeGenUtility util) {
    if (!INTERFACE.equals(modelRootType.getKind())) {
      util.error("Model root '%s' must be an interface".formatted(modelRootType), modelRootType);
    }

    if (!util.isRawAssignable(modelRootType.asType(), Model.class)) {
      util.error(
          "Model root '%s' must implement Model interface".formatted(modelRootType), modelRootType);
    }
  }

  private void generateProtobufSchema() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    String modelRootName = modelRootType.getSimpleName().toString();
    String packageName =
        util.processingEnv().getElementUtils().getPackageOf(modelRootType).toString();
    String protoFileName = modelRootName + MODELS_PROTO_FILE_SUFFIX;

    try {
      // Create output directory if it doesn't exist
      Path outputDir = createOutputDirectory(util.detectSourceOutputPath(modelRootType), util);

      // Generate proto file content
      String protoContent = generateProtoFileContent(modelRootType, packageName);

      Path packageDir = outputDir;

      for (String packageElem : Splitter.on('.').omitEmptyStrings().split(packageName)) {
        packageDir = packageDir.resolve(packageElem);
      }

      //noinspection ResultOfMethodCallIgnored
      Files.createDirectories(packageDir);
      // Write proto file
      Path protoFilePath = packageDir.resolve(protoFileName);
      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(protoFilePath))) {
        out.println(protoContent);
      }

      log.info("Generated protobuf schema file: {}", protoFilePath);
    } catch (Exception e) {
      util.error(
          String.format(
              "Error generating protobuf schema for %s: %s",
              modelRootName, getStackTraceAsString(e)),
          modelRootType);
    }
  }

  private String generateProtoFileContent(TypeElement modelRootType, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String modelRootName = modelRootType.getSimpleName().toString();

    // Add auto-generated comment
    protoBuilder
        .append("// AUTOMATICALLY GENERATED - DO NOT EDIT!\n")
        .append("// This schema is auto-generated by Krystal's code generator.\n")
        .append("// It models the proto for Model Root: ")
        .append(modelRootName)
        .append("\n")
        .append("// Source: ")
        .append(modelRootType.getQualifiedName())
        .append("\n")
        .append("// Any manual edits to this file will be overwritten.\n\n");

    // Add syntax, package, and options
    protoBuilder.append("syntax = \"proto3\";\n\n");

    protoBuilder.append("package ").append(packageName).append(";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    protoBuilder.append("option java_multiple_files = true;\n");
    //noinspection SpellCheckingInspection: java_outer_classname
    protoBuilder
        .append("option java_outer_classname = \"")
        .append(modelRootName)
        .append(MODELS_PROTO_OUTER_CLASS_SUFFIX + "\";\n\n");

    Set<String> imports = new LinkedHashSet<>();

    StringBuilder protoMessageBody = generateMessageBodyAndCollectImports(modelRootType, imports);

    for (String anImport : imports) {
      protoBuilder.append("import ").append('"').append(anImport).append('"').append(";\n");
    }

    // Add documentation as comments if available
    String messageDoc =
        requireNonNullElse(util.processingEnv().getElementUtils().getDocComment(modelRootType), "");
    if (!messageDoc.trim().isEmpty()) {
      // Format the documentation as a proto comment
      // Split by newlines and add proper indentation and comment markers
      Iterable<String> docLines = Splitter.on('\n').split(messageDoc);
      for (String line : docLines) {
        protoBuilder.append("// ").append(line.trim()).append("\n");
      }
    }
    protoBuilder.append("message ").append(modelRootName).append(MODELS_PROTO_MSG_SUFFIX + " {\n");
    protoBuilder.append(protoMessageBody);
    protoBuilder.append("}\n");

    return protoBuilder.toString();
  }

  private StringBuilder generateMessageBodyAndCollectImports(
      TypeElement modelRootType, Set<String> imports) {
    String modelRootName = modelRootType.getSimpleName().toString();
    // Extract methods from the model root interface
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    // Add fields from model methods using SerialId annotation for field numbers
    Set<Integer> usedFieldNumbers = new HashSet<>();
    StringBuilder protoMessageBody = new StringBuilder();

    for (ExecutableElement method : modelMethods) {
      // Get the SerialId annotation from the method
      SerialId serialId = method.getAnnotation(SerialId.class);
      // Get the field number from the annotation
      int fieldNumber;
      if (serialId == null) {
        util.error(
            String.format(
                "Missing @SerialId annotation on method '%s' in Model Root '%s'",
                method.getSimpleName(), modelRootName),
            method);
        fieldNumber = -1;
      } else {
        fieldNumber = serialId.value();
      }

      // Validate the field number
      if (fieldNumber <= 0) {
        util.error(
            String.format(
                "Invalid SerialId %d for method '%s' in Model Root '%s'. SerialId must be positive.",
                fieldNumber, method.getSimpleName(), modelRootName),
            method);
      }

      // Check for duplicate field numbers
      if (!usedFieldNumbers.add(fieldNumber)) {
        util.error(
            String.format(
                "Duplicate SerialId %d for method '%s' in Model Root '%s'",
                fieldNumber, method.getSimpleName(), modelRootName),
            method);
      }

      // Get the return type and convert it to a DataType
      CodeGenType dataType = new DeclaredTypeVisitor(util, method).visit(method.getReturnType());

      boolean isOptional = true; // Default to optional for proto3

      // In proto3, the 'optional' keyword is needed for all primitive types to check
      // presence. This includes numeric types, booleans, strings, bytes, and enums.

      IfAbsentThen ifAbsentThen = util.getIfAbsent(method, modelRoot).value();

      boolean isPrimitiveArray =
          util.isPrimitiveArray(dataType.javaModelType(util.processingEnv()));
      boolean isRepeated = isProtoTypeRepeated(dataType);
      boolean isMap = isProtoTypeMap(dataType);
      if ((isRepeated || isMap || isPrimitiveArray) && !ifAbsentThen.usePlatformDefault()) {
        // Proto3 always defaults repeated and map fields to default values
        util.error(
            String.format(
                "Method '%s' in Model Root '%s' is a %s field, and has @IfAbsent(%s) which is not supported in protobuf3. "
                    + "Please use a @IfAbsent(%s) as that is the only strategy supported for repeated and map fields in protobuf3.",
                method.getSimpleName(),
                modelRootName,
                isRepeated ? "list" : isPrimitiveArray ? "array" : "map",
                ifAbsentThen,
                ASSUME_DEFAULT_VALUE),
            method);
      } else if (ifAbsentThen.usePlatformDefault()) {
        // If the strategy allows defaulting, we can make it a required field in proto3
        isOptional = false;
      }

      // Add documentation as comments if available
      String documentation = util.processingEnv().getElementUtils().getDocComment(method);
      if (documentation != null && !documentation.trim().isEmpty()) {
        // Format the documentation as a proto comment
        // Split by newlines and add proper indentation and comment markers
        Iterable<String> docLines = Splitter.on('\n').split(documentation);
        for (String line : docLines) {
          protoMessageBody.append("  // ").append(line.trim()).append("\n");
        }
      }

      protoMessageBody.append("  ");

      ProtoFieldType protobufType = getProtobufType(dataType, util, method);

      imports.addAll(protobufType.imports());

      // Add 'optional' keyword if needed
      // Note: repeated and map fields don't need the optional keyword
      if (isOptional && !(protobufType instanceof OptionalFieldType)) {
        protobufType = new OptionalFieldType(protobufType, util, method);
      }
      // For repeated and map fields, the 'repeated' or 'map<>' prefix is already included in
      // fieldType
      protoMessageBody
          .append(protobufType.typeInProtoFile())
          .append(" ")
          .append(method.getSimpleName().toString())
          .append(" = ")
          .append(fieldNumber)
          .append(";\n");
    }
    return protoMessageBody;
  }
}
