package com.flipkart.krystal.vajram.protobuf.codegen.util;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.createOutputDirectory;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.getProtobufType;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.isProtoTypeMap;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.isProtoTypeRepeated;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.toLowerSnakeCasePackage;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.toSnakeCase;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.toTitleCaseProtoName;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static javax.lang.model.element.ElementKind.ENUM;
import static javax.lang.model.element.ElementKind.INTERFACE;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.array.PrimitiveArray;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.codegen.common.generators.SerdeModelValidator;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoFieldType;
import com.google.common.base.Splitter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates a protobuf schema (a {@code .proto} file) for models derived from a {@code @ModelRoot}
 * interface or enum. Protocol-specific text (header, suffixes, explicit-presence handling) comes
 * from a {@link ProtoSchemaConfig}.
 */
@Slf4j
public abstract class BaseProtoSchemaGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;
  private final ModelRoot modelRoot;
  private final ProtoSchemaConfig config;

  protected BaseProtoSchemaGen(ModelsCodeGenContext codeGenContext, ProtoSchemaConfig config) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
    this.modelRoot = requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
    this.config = config;
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

    return util.getModelProtocols(codeGenContext.modelRootType())
        .contains(config.protocolInstance());
  }

  private void validate() {
    TypeElement modelRootType = codeGenContext.modelRootType();
    validateModelType(modelRootType, util);
    if (!util.isEnumModel(modelRootType)) {
      List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);
      new SerdeModelValidator(util, modelRootType, config.protocolInstance())
          .validate(modelMethods);
    }
  }

  public static void validateModelType(TypeElement modelRootType, CodeGenUtility util) {
    if (ENUM.equals(modelRootType.getKind())) {
      if (!util.isRawAssignable(modelRootType.asType(), EnumModel.class)) {
        util.error(
            "Enum model root '%s' must implement EnumModel interface".formatted(modelRootType),
            modelRootType);
      }
      return;
    }
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
    String protoFileName = modelRootName + config.fileSuffix();

    try {
      Path outputDir = createOutputDirectory(util.detectSourceOutputPath(modelRootType), util);

      String protoContent;
      if (util.isEnumModel(modelRootType)) {
        protoContent = generateEnumProtoFileContent(modelRootType, packageName);
      } else {
        protoContent = generateProtoFileContent(modelRootType, packageName);
      }

      Path packageDir = outputDir;

      for (String packageElem : Splitter.on('.').omitEmptyStrings().split(packageName)) {
        packageDir = packageDir.resolve(packageElem);
      }

      //noinspection ResultOfMethodCallIgnored
      Files.createDirectories(packageDir);
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

  private String generateEnumProtoFileContent(TypeElement enumType, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String enumName = enumType.getSimpleName().toString();

    protoBuilder
        .append("// AUTOMATICALLY GENERATED - DO NOT EDIT!\n")
        .append("// This schema is auto-generated by Krystal's code generator.\n")
        .append("// It models the proto enum for Model Root: ")
        .append(enumName)
        .append("\n")
        .append("// Source: ")
        .append(enumType.getQualifiedName())
        .append("\n")
        .append("// Any manual edits to this file will be overwritten.\n\n");

    protoBuilder.append(config.schemaHeader()).append("\n\n");

    // Proto package is lower_snake_case (required by editions 2024+); java_package keeps the
    // original Java package so downstream Java consumers are unaffected.
    protoBuilder.append("package ").append(toLowerSnakeCasePackage(packageName)).append(";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    if (config.emitJavaMultipleFiles()) {
      protoBuilder.append("option java_multiple_files = true;\n");
    }
    //noinspection SpellCheckingInspection: java_outer_classname
    protoBuilder
        .append("option java_outer_classname = \"")
        .append(enumName)
        .append(config.outerClassSuffix())
        .append("\";\n\n");

    String enumDoc =
        requireNonNullElse(util.processingEnv().getElementUtils().getDocComment(enumType), "");
    if (!enumDoc.trim().isEmpty()) {
      Iterable<String> docLines = Splitter.on('\n').split(enumDoc);
      for (String line : docLines) {
        protoBuilder.append("// ").append(line.trim()).append("\n");
      }
    }

    // Strip underscores from the model name so the proto enum name is TitleCase (required by
    // editions 2024+).
    String protoEnumName = toTitleCaseProtoName(enumName) + config.messageSuffix();
    protoBuilder.append("enum ").append(protoEnumName).append(" {\n");

    List<VariableElement> enumConstants =
        ElementFilter.fieldsIn(enumType.getEnclosedElements()).stream()
            .filter(field -> field.getKind() == ElementKind.ENUM_CONSTANT)
            .toList();

    boolean anyHasSerialId =
        enumConstants.stream().anyMatch(c -> c.getAnnotation(SerialId.class) != null);

    for (int i = 0; i < enumConstants.size(); i++) {
      VariableElement constant = enumConstants.get(i);
      String constantName = constant.getSimpleName().toString();

      int protoIndex;
      if (anyHasSerialId) {
        SerialId serialId = constant.getAnnotation(SerialId.class);
        if (serialId != null) {
          protoIndex = serialId.value();
        } else {
          protoIndex = i;
        }
      } else {
        protoIndex = i;
      }

      String constDoc =
          requireNonNullElse(util.processingEnv().getElementUtils().getDocComment(constant), "");
      if (!constDoc.trim().isEmpty()) {
        Iterable<String> docLines = Splitter.on('\n').split(constDoc);
        for (String line : docLines) {
          protoBuilder.append("  // ").append(line.trim()).append("\n");
        }
      }

      protoBuilder.append("  ").append(constantName).append(" = ").append(protoIndex).append(";\n");
    }

    protoBuilder.append("}\n");

    return protoBuilder.toString();
  }

  private String generateProtoFileContent(TypeElement modelRootType, String packageName) {
    StringBuilder protoBuilder = new StringBuilder();
    String modelRootName = modelRootType.getSimpleName().toString();

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

    protoBuilder.append(config.schemaHeader()).append("\n\n");

    // Proto package is lower_snake_case (required by editions 2024+); java_package keeps the
    // original Java package so downstream Java consumers are unaffected.
    protoBuilder.append("package ").append(toLowerSnakeCasePackage(packageName)).append(";\n\n");

    protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
    if (config.emitJavaMultipleFiles()) {
      protoBuilder.append("option java_multiple_files = true;\n");
    }
    //noinspection SpellCheckingInspection: java_outer_classname
    protoBuilder
        .append("option java_outer_classname = \"")
        .append(modelRootName)
        .append(config.outerClassSuffix())
        .append("\";\n\n");

    Set<String> imports = new LinkedHashSet<>();

    StringBuilder protoMessageBody = generateMessageBodyAndCollectImports(modelRootType, imports);

    for (String anImport : imports) {
      protoBuilder.append("import ").append('"').append(anImport).append('"').append(";\n");
    }

    String messageDoc =
        requireNonNullElse(util.processingEnv().getElementUtils().getDocComment(modelRootType), "");
    if (!messageDoc.trim().isEmpty()) {
      Iterable<String> docLines = Splitter.on('\n').split(messageDoc);
      for (String line : docLines) {
        protoBuilder.append("// ").append(line.trim()).append("\n");
      }
    }
    // Strip underscores from the model name so the proto message name is TitleCase (required by
    // editions 2024+). The corresponding protoc-generated Java class will also lose the
    // underscores - BaseProtoModelsGen mirrors this when computing class references.
    protoBuilder
        .append("message ")
        .append(toTitleCaseProtoName(modelRootName))
        .append(config.messageSuffix())
        .append(" {\n");
    protoBuilder.append(protoMessageBody);
    protoBuilder.append("}\n");

    return protoBuilder.toString();
  }

  private StringBuilder generateMessageBodyAndCollectImports(
      TypeElement modelRootType, Set<String> imports) {
    String modelRootName = modelRootType.getSimpleName().toString();
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    Set<Integer> usedFieldNumbers = new HashSet<>();
    StringBuilder protoMessageBody = new StringBuilder();

    for (ExecutableElement method : modelMethods) {
      SerialId serialId = method.getAnnotation(SerialId.class);
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

      if (fieldNumber <= 0) {
        util.error(
            String.format(
                "Invalid SerialId %d for method '%s' in Model Root '%s'. SerialId must be positive.",
                fieldNumber, method.getSimpleName(), modelRootName),
            method);
      }

      if (!usedFieldNumbers.add(fieldNumber)) {
        util.error(
            String.format(
                "Duplicate SerialId %d for method '%s' in Model Root '%s'",
                fieldNumber, method.getSimpleName(), modelRootName),
            method);
      }

      CodeGenType dataType = new DeclaredTypeVisitor(util, method).visit(method.getReturnType());

      boolean needsExplicitPresence = true;

      IfAbsentThen ifAbsentThen = util.getIfAbsent(method, modelRoot).value();

      TypeMirror type = dataType.javaModelType(util.processingEnv());
      boolean isPrimitiveArray = util.isPrimitiveArray(type);
      boolean isByteArray = util.isRawAssignable(type, PrimitiveArray.class);
      boolean isRepeated = isProtoTypeRepeated(dataType);
      boolean isMap = isProtoTypeMap(dataType);
      if ((isRepeated
              || isMap
              || (isPrimitiveArray && !isByteArray)) // proto supports presence check for bytes
          && !ifAbsentThen.usePlatformDefault()) {
        // Repeated and map fields always default to empty in protobuf - presence is implicit.
        util.error(
            String.format(
                "Method '%s' in Model Root '%s' is a %s field, and has @IfAbsent(%s) which is not supported in protobuf. "
                    + "Please use a @IfAbsent(%s) as that is the only strategy supported for repeated and map fields in protobuf.",
                method.getSimpleName(),
                modelRootName,
                isRepeated ? "list" : isPrimitiveArray ? "array" : "map",
                ifAbsentThen,
                ASSUME_DEFAULT_VALUE),
            method);
      } else if (ifAbsentThen.usePlatformDefault()) {
        needsExplicitPresence = false;
      }

      String documentation = util.processingEnv().getElementUtils().getDocComment(method);
      if (documentation != null && !documentation.trim().isEmpty()) {
        Iterable<String> docLines = Splitter.on('\n').split(documentation);
        for (String line : docLines) {
          protoMessageBody.append("  // ").append(line.trim()).append("\n");
        }
      }

      protoMessageBody.append("  ");

      ProtoFieldType protobufType = getProtobufType(dataType, util, method, config);

      imports.addAll(protobufType.imports());

      if (needsExplicitPresence) {
        protobufType = config.presenceWrapper().wrap(protobufType, util, method);
      }
      // Field names are emitted in lower_snake_case (proto convention, required by editions
      // 2024+ naming-style enforcement). Krystal model methods are camelCase, so we convert.
      // The protoc-generated Java getters/setters are derived from the proto field name and
      // happen to round-trip back to the original camelCase, so downstream Java code is
      // unaffected by this rename.
      protoMessageBody
          .append(protobufType.typeInProtoFile())
          .append(" ")
          .append(toSnakeCase(method.getSimpleName().toString()))
          .append(" = ")
          .append(fieldNumber)
          .append(";\n");
    }
    return protoMessageBody;
  }

  protected ProtoSchemaConfig config() {
    return config;
  }
}
