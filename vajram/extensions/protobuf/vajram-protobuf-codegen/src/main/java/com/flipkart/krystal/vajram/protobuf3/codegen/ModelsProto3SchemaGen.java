package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.MODELS_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.MODELS_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.Constants.MODELS_PROTO_OUTER_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.javaPackageToProtoPackageName;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramModelsProto3SchemaGen.getProtobufType;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramModelsProto3SchemaGen.isProtoTypeMap;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramModelsProto3SchemaGen.isProtoTypeRepeated;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramModelsProto3SchemaGen.isProtoTypeScalar;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.createDirectories;
import static javax.lang.model.element.ElementKind.INTERFACE;

import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.data.IfNoValue.Strategy;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.ModelsCodeGenContext;
import com.google.common.base.Splitter;

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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.extern.slf4j.Slf4j;

/** Code generator which generates protobuf schema for models derived from a ModelRoot interface. */
@Slf4j
final class ModelsProto3SchemaGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final Utils util;

  public ModelsProto3SchemaGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
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
    if (!CodegenPhase.MODELS.equals(codeGenContext.codegenPhase())) {
      util.note("Skipping protobuf codegen since current phase is not MODELS");
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
    validateModelType(codeGenContext.modelRootType(), util);
  }

  static void validateModelType(TypeElement modelRootType, Utils util) {
    if (!INTERFACE.equals(modelRootType.getKind())) {
      throw util.errorAndThrow(
          "Model root '%s' must be an interface".formatted(modelRootType), modelRootType);
    }

    if (!util.isRawAssignable(modelRootType.asType(), Model.class)) {
      throw util.errorAndThrow(
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
      Path outputDir = createOutputDirectory();

      // Generate proto file content
      String protoContent = generateProtoFileContent(modelRootType, packageName);

      // Write proto file
      Path protoFilePath = outputDir.resolve(protoFileName);
      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(protoFilePath))) {
        out.println(protoContent);
      }

      log.info("Generated protobuf schema file: {}", protoFilePath);
    } catch (IOException e) {
      util.error(
          String.format(
              "Error generating protobuf schema for %s: %s", modelRootName, e.getMessage()),
          modelRootType);
    }
  }

  private Path createOutputDirectory() throws IOException {
    try {

      // Get the location where generated source files should be placed
      Path currentDir = util.detectSourceOutputPath(codeGenContext.modelRootType()).getParent();
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

  private String generateProtoFileContent(TypeElement modelRootType, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String modelRootName = modelRootType.getSimpleName().toString();

    // Add auto-generated comment
    protoBuilder
        .append("// AUTOMATICALLY GENERATED - DO NOT EDIT!\n")
        .append("// This schema is auto-generated by Krystal's code generator.\n")
        .append("// It models the request for Model Root: ")
        .append(modelRootName)
        .append("\n")
        .append("// Source: ")
        .append(modelRootType.getQualifiedName())
        .append("\n")
        .append("// Any manual edits to this file will be overwritten.\n\n");

    // Add syntax, package, and options
    protoBuilder.append("syntax = \"proto3\";\n\n");

    protoBuilder
        .append("package ")
        .append(javaPackageToProtoPackageName(packageName))
        .append(";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    protoBuilder.append("option java_multiple_files = true;\n");
    protoBuilder
        .append("option java_outer_classname = \"")
        .append(modelRootName)
        .append(MODELS_PROTO_OUTER_CLASS_SUFFIX + "\";\n\n");

    // Add message definition
    protoBuilder.append("message ").append(modelRootName).append(MODELS_PROTO_MSG_SUFFIX + " {\n");

    // Extract methods from the model root interface
    List<ExecutableElement> modelMethods = extractModelMethods(modelRootType);

    // Add fields from model methods using SerialId annotation for field numbers
    Set<Integer> usedFieldNumbers = new HashSet<>();

    for (ExecutableElement method : modelMethods) {
      // Get the SerialId annotation from the method
      SerialId serialId = method.getAnnotation(SerialId.class);
      if (serialId == null) {
        throw util.errorAndThrow(
            String.format(
                "Missing @SerialId annotation on method '%s' in Model Root '%s'",
                method.getSimpleName(), modelRootName),
            method);
      }

      // Get the field number from the annotation
      int fieldNumber = serialId.value();

      // Validate the field number
      if (fieldNumber <= 0) {
        throw util.errorAndThrow(
            String.format(
                "Invalid SerialId %d for method '%s' in Model Root '%s'. SerialId must be positive.",
                fieldNumber, method.getSimpleName(), modelRootName),
            method);
      }

      // Check for duplicate field numbers
      if (!usedFieldNumbers.add(fieldNumber)) {
        throw util.errorAndThrow(
            String.format(
                "Duplicate SerialId %d for method '%s' in Model Root '%s'",
                fieldNumber, method.getSimpleName(), modelRootName),
            method);
      }

      // Get the return type and convert it to a DataType
      TypeMirror returnType = method.getReturnType();
      DataType<?> dataType = new DeclaredTypeVisitor<>(util, method).visit(returnType);

      String fieldType = getProtobufType(dataType, util);
      String fieldName = method.getSimpleName().toString();

      // Check if the method has the @IfNoValue annotation (equivalent to @Mandatory)
      IfNoValue ifNoValue = method.getAnnotation(IfNoValue.class);
      boolean isOptional = true; // Default to optional for proto3

      // In proto3, the 'optional' keyword is needed for all primitive types to check
      // presence. This includes numeric types, booleans, strings, bytes, and enums.
      if (ifNoValue != null) {
        // Field has @IfNoValue annotation
        Strategy ifNoValueStrategy = ifNoValue.then();

        TypeMirror rawType =
            util.processingEnv()
                .getTypeUtils()
                .erasure(dataType.javaModelType(util.processingEnv()));
        boolean isRepeated = util.isRawAssignable(rawType, List.class);
        boolean isMap = util.isRawAssignable(rawType, Map.class);
        if (!ifNoValueStrategy.usePlatformDefault() && (isRepeated || isMap)) {
          // Proto3 cannot enforce mandatory fields with FAIL strategy for repeated and
          // map fields
          throw util.errorAndThrow(
              String.format(
                  "Method '%s' in Model Root '%s' is a %s field, and has @IfNoValue(then=%s) which is not supported in protobuf3. "
                      + "Use a different IfNoValue strategy or remove @IfNoValue annotation.",
                  method.getSimpleName(),
                  modelRootName,
                  isRepeated ? "repeated" : "map",
                  ifNoValueStrategy),
              method);
        } else if (ifNoValueStrategy.usePlatformDefault()) {
          // If the strategy allows defaulting, we can make it a required field in proto3
          isOptional = false;
        }
      }

      // Add documentation as comments if available
      String documentation = util.processingEnv().getElementUtils().getDocComment(method);
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
          && isProtoTypeScalar(dataType)
          && !isProtoTypeRepeated(dataType)
          && !isProtoTypeMap(dataType)) {
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

  private List<ExecutableElement> extractModelMethods(TypeElement modelRootType) {
    List<ExecutableElement> modelMethods = new ArrayList<>();

    for (Element element : modelRootType.getEnclosedElements()) {
      if (ElementKind.METHOD.equals(element.getKind())) {
        ExecutableElement method = (ExecutableElement) element;

        // Skip methods from Object class or other non-model methods
        if (method.getSimpleName().toString().equals("_build")
            || method.getSimpleName().toString().equals("_asBuilder")
            || method.getSimpleName().toString().equals("_newCopy")) {
          continue;
        }

        // Validate method has zero parameters
        if (!method.getParameters().isEmpty()) {
          continue;
        }

        modelMethods.add(method);
      }
    }

    return modelMethods;
  }
}
