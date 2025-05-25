package com.flipkart.krystal.lattice.ext.protobuf.codegen;

import static com.flipkart.krystal.lattice.ext.protobuf.codegen.LatticeProtoGenConstants.PROTO_SERVER_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.createOutputDirectory;
import static com.flipkart.krystal.vajram.protobuf3.codegen.ProtoGenUtils.getSimpleClassName;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.VAJRAM_REQ_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.VAJRAM_REQ_PROTO_MSG_SUFFIX;
import static java.util.Arrays.deepToString;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeValidationException;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.tools.Diagnostic.Kind;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Code generator which generates protobuf schema for a request proto containing the input facets of
 * a vajram
 */
@Slf4j
class LatticeProto3ServiceSchemaGen implements CodeGenerator {

  private final LatticeCodegenContext context;
  private final VajramCodeGenUtility util;

  public LatticeProto3ServiceSchemaGen(LatticeCodegenContext context) {
    this.context = context;
    this.util = context.codeGenUtility();
  }

  @Override
  public void generate() {
    if (!CodegenPhase.MODELS.equals(context.codegenPhase())) {
      util.codegenUtil()
          .note("Skipping proto service schema codegen since current phase is not MODELS");
      return;
    }
    util.codegenUtil().note("Starting proto service schema gen");
    GrpcServerAnno grpcServer = getGrpcServer();
    if (grpcServer == null) {
      util.codegenUtil().note("Grpc Server is null");
      return;
    }
    validate(grpcServer);
    generateServerFile(grpcServer.grpcServer());
  }

  private @Nullable GrpcServerAnno getGrpcServer() {
    TypeElement latticeAppType = context.latticeAppTypeElement();
    GrpcServer grpcServer = latticeAppType.getAnnotation(GrpcServer.class);
    Optional<? extends AnnotationMirror> mirror =
        latticeAppType.getAnnotationMirrors().stream()
            .filter(
                annotationMirror ->
                    annotationMirror.getAnnotationType().asElement() instanceof QualifiedNameable q
                        && q.getQualifiedName().contentEquals(GrpcServer.class.getCanonicalName()))
            .findAny();
    if (grpcServer != null && mirror.isPresent()) {
      return new GrpcServerAnno(grpcServer, mirror.get());
    }

    return null;
  }

  record GrpcServerAnno(GrpcServer grpcServer, AnnotationMirror mirror) {}

  /** Validates the Vajram for protobuf compatibility. Throws exceptions if validations fail. */
  void validate(GrpcServerAnno anno) throws CodeValidationException {
    Map<String, ? extends AnnotationValue> elementValues =
        anno.mirror().getElementValues().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getSimpleName().toString(), Entry::getValue));
    if (anno.grpcServer().serverName().isEmpty()) {
      String message = "Grpc serverName cannot be empty";
      util.processingEnv()
          .getMessager()
          .printMessage(
              Kind.ERROR,
              message,
              context.latticeAppTypeElement(),
              anno.mirror(),
              elementValues.get("serverName"));
      throw new CodeValidationException(message);
    }

    GrpcService[] grpcServices = anno.grpcServer().services();
    if (grpcServices.length == 0) {
      String message = "No Services registered with the Grpc server. This is not allowed";
      util.processingEnv()
          .getMessager()
          .printMessage(
              Kind.ERROR,
              message,
              context.latticeAppTypeElement(),
              anno.mirror(),
              elementValues.get("services"));
      throw new CodeValidationException(message);
    }

    var annoMember =
        new SimpleAnnotationValueVisitor14<AnnotationMirror, Void>() {
          @Override
          public AnnotationMirror visitAnnotation(AnnotationMirror a, Void unused) {
            return a;
          }
        };

    var nestedAnnotations =
        new SimpleAnnotationValueVisitor14<List<AnnotationMirror>, Void>() {
          @Override
          public List<AnnotationMirror> visitArray(
              List<? extends AnnotationValue> vals, Void unused) {
            return vals.stream().map(annoMember::visit).toList();
          }
        };
    List<AnnotationMirror> list =
        nestedAnnotations.visit(requireNonNull(elementValues.get("services")));

    Set<String> serviceNames = new LinkedHashSet<>();
    for (int i = 0; i < grpcServices.length; i++) {
      GrpcService grpcService = grpcServices[i];
      String serviceName = grpcService.serviceName();
      AnnotationMirror serviceAnnoMirror = list.get(i);
      if (serviceName.isEmpty()) {
        String message = "Grpc Service name cannot be empty";
        util.processingEnv()
            .getMessager()
            .printMessage(
                Kind.ERROR,
                message,
                context.latticeAppTypeElement(),
                serviceAnnoMirror,
                serviceAnnoMirror.getElementValues().entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("serviceName"))
                    .map(Entry::getValue)
                    .findAny()
                    .orElse(null));
        throw new CodeValidationException(message);
      }
      if (!serviceNames.add(serviceName)) {
        String message = "Duplicate grpc service name";
        util.processingEnv()
            .getMessager()
            .printMessage(
                Kind.ERROR,
                message,
                context.latticeAppTypeElement(),
                serviceAnnoMirror,
                serviceAnnoMirror.getElementValues().entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("serviceName"))
                    .map(Entry::getValue)
                    .findAny()
                    .orElse(null));
      }

      List<? extends TypeMirror> vajrams =
          util.codegenUtil().getTypesFromAnnotationMember(grpcService::vajrams);
      if (vajrams.isEmpty()) {
        String message = "No vajrams registered with service grpcService. This is not allowed";
        util.processingEnv()
            .getMessager()
            .printMessage(
                Kind.ERROR,
                message,
                context.latticeAppTypeElement(),
                serviceAnnoMirror,
                serviceAnnoMirror.getElementValues().entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("vajrams"))
                    .map(Entry::getValue)
                    .findAny()
                    .orElse(null));
      }
    }
  }

  /**
   * Generates the content for the service proto file that contains the service definition for the
   * remotely invocable Vajram.
   *
   * @param grpcServer The grpc server annotation
   */
  private void generateServerFile(GrpcServer grpcServer) {
    GrpcService[] services = grpcServer.services();
    String packageName =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(context.latticeAppTypeElement())
            .getQualifiedName()
            .toString();
    try {
      // Create output directory if it doesn't exist
      String serviceProtoFileName = grpcServer.serverName() + PROTO_SERVER_FILE_SUFFIX;
      Path outputDir = createOutputDirectory(util.detectSourceOutputPath(null), util.codegenUtil());
      StringBuilder protoBuilder = new StringBuilder();
      // Add auto-generated comment
      protoBuilder
          .append("// AUTOMATICALLY GENERATED - DO NOT EDIT!\n")
          .append("// This schema is auto-generated by Krystal's code generator.\n")
          .append("// It defines the service for Vajrams inside the package ")
          .append(packageName)
          .append("\n")
          .append("// Any manual edits to this file will be overwritten.\n\n");

      // Add syntax, package, and options
      protoBuilder.append("syntax = \"proto3\";\n\n");

      protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
      protoBuilder.append("option java_multiple_files = true;\n\n");
      // Add documentation for the service
      protoBuilder.append(
          "// Service definition for the services defined by lattice application  %s\n"
              .formatted(context.latticeAppTypeElement().getQualifiedName().toString()));
      Set<String> imports = new LinkedHashSet<>();
      Arrays.stream(services)
          .forEach(
              serviceAnno -> {
                for (TypeMirror vajramType :
                    util.codegenUtil().getTypesFromAnnotationMember(serviceAnno::vajrams)) {
                  VajramInfoLite vajramInfo =
                      util.computeVajramInfoLite(
                          requireNonNull(
                              (TypeElement)
                                  util.processingEnv().getTypeUtils().asElement(vajramType)));

                  // Get the response type name (without package) to use in the service definition
                  String responseTypeName =
                      getSimpleClassName(vajramInfo.responseType().canonicalClassName());

                  // Add imports for the request and response messages
                  imports.add(vajramInfo.vajramId().id() + VAJRAM_REQ_PROTO_FILE_SUFFIX);
                  imports.add(responseTypeName + MODELS_PROTO_FILE_SUFFIX);
                }
              });
      for (String anImport : imports) {
        protoBuilder.append("import \"").append(anImport).append("\";\n");
      }
      protoBuilder.append("\n");
      protoBuilder.append("package ").append(packageName).append(";\n\n\n");

      Arrays.stream(services)
          .forEach(
              serviceAnno -> {
                List<VajramInfoLite> vajramInfos =
                    util.codegenUtil().getTypesFromAnnotationMember(serviceAnno::vajrams).stream()
                        .map(
                            typeMirror ->
                                util.computeVajramInfoLite(
                                    requireNonNull(
                                        (TypeElement)
                                            util.processingEnv()
                                                .getTypeUtils()
                                                .asElement(typeMirror))))
                        .toList();

                String doc = serviceAnno.doc();
                if (!doc.isBlank()) {
                  protoBuilder.append("// ").append(doc.replace("\n", "\n  //")).append("\n");
                }

                // Create the service definition
                protoBuilder.append("service ").append(serviceAnno.serviceName()).append(" {\n");

                for (VajramInfoLite vajramInfo : vajramInfos) {
                  String vajramId = vajramInfo.vajramId().id();

                  // Get the response type name (without package) to use in the service definition
                  String responseTypeName =
                      getSimpleClassName(vajramInfo.responseType().canonicalClassName());

                  // Add the RPC method
                  String vajramDocString = vajramInfo.docString();
                  if (vajramDocString != null && !vajramDocString.isBlank()) {
                    protoBuilder
                        .append("  //")
                        .append(vajramDocString.replace("\n", "\n  //"))
                        .append("\n");
                  }
                  protoBuilder
                      .append("  rpc ")
                      .append(vajramId)
                      .append("(")
                      .append(vajramId)
                      .append(VAJRAM_REQ_PROTO_MSG_SUFFIX)
                      .append(") \n    returns (")
                      .append(responseTypeName)
                      .append(MODELS_PROTO_MSG_SUFFIX)
                      .append(");\n\n");
                }
                // Close the service definition
                protoBuilder.append("}\n\n");
              });
      // Write server proto file
      Path serviceProtoFilePath = outputDir.resolve(serviceProtoFileName);
      log.info("Generated service protobuf schema file: {}", serviceProtoFilePath);

      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(serviceProtoFilePath))) {
        out.println(protoBuilder);
      } catch (IOException e) {
        util.error("Failed to generate service proto file %s".formatted(serviceProtoFilePath));
      }
    } catch (IOException e) {
      util.error(
          String.format(
              "Error generating protobuf service definition for %s: %s",
              deepToString(services), e.getMessage()));
    } finally {
      util.codegenUtil().note("Create service proto file");
    }
  }
}
