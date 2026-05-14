package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.createOutputDirectory;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.getPackageName;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.getSimpleClassName;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.toLowerSnakeCasePackage;
import static com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility.toTitleCaseProtoName;
import static java.util.Arrays.deepToString;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility.AnnotationInfo;
import com.flipkart.krystal.codegen.common.models.CodeValidationException;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import com.flipkart.krystal.vajram.protobuf.util.ProtobufProtocol;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.tools.Diagnostic.Kind;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Code generator which generates the protobuf schema describing a grpc service. Detects which
 * {@link ProtobufProtocol} the rpc vajrams use and emits a schema in that protocol's syntax.
 */
@Slf4j
class ProtoServiceSchemaGen implements CodeGenerator {

  private final LatticeCodegenContext context;
  private final VajramCodeGenUtility util;

  public ProtoServiceSchemaGen(LatticeCodegenContext context) {
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
    AnnotationInfo<GrpcServer> grpcServer =
        context
            .codeGenUtility()
            .codegenUtil()
            .getAnnotationInfo(context.latticeAppTypeElement(), GrpcServer.class);
    if (grpcServer == null) {
      util.codegenUtil().note("gRPC Server is null");
      return;
    }
    validate(grpcServer);
    ProtobufProtocol protocol =
        GrpcProtocolResolver.resolveResponseProtocol(
            grpcServer.annotation(), util, context.latticeAppTypeElement());
    if (protocol == null) {
      // Resolver already emitted an error.
      return;
    }
    generateServerFile(grpcServer.annotation(), protocol);
  }

  /** Validates the Vajram for protobuf compatibility. Throws exceptions if validations fail. */
  void validate(@MonotonicNonNull AnnotationInfo<GrpcServer> anno) throws CodeValidationException {
    Map<String, ? extends AnnotationValue> elementValues =
        anno.mirror().getElementValues().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getSimpleName().toString(), Entry::getValue));
    if (anno.annotation().serverName().isEmpty()) {
      String message = "gRPC serverName cannot be empty";
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

    GrpcService[] grpcServices = anno.annotation().services();
    if (grpcServices.length == 0) {
      String message = "No Services registered with the gRPC server. This is not allowed";
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
        String message = "gRPC Service name cannot be empty";
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
        String message = "Duplicate gRPC service name";
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
          util.codegenUtil().getTypesFromAnnotationMember(grpcService::rpcVajrams);
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
   * @param grpcServer The gRPC server annotation
   * @param protocol The {@link ProtobufProtocol} all rpc vajram response models share
   */
  private void generateServerFile(GrpcServer grpcServer, ProtobufProtocol protocol) {
    GrpcService[] services = grpcServer.services();
    String packageName =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(context.latticeAppTypeElement())
            .getQualifiedName()
            .toString();
    String reqProtoFileSuffix = "_Req" + protocol.protoFileSuffix();
    // The vajram-id is the bare vajram name (without `_Req`) so the message suffix bundles
    // `Req` (no leading underscore - proto messages must be TitleCase).
    String reqProtoMsgSuffix = "Req" + protocol.protoMsgSuffix();
    String modelsProtoFileSuffix = protocol.protoFileSuffix();
    String modelsProtoMsgSuffix = protocol.protoMsgSuffix();
    try {
      // Create output directory if it doesn't exist
      String serviceProtoFileName =
          grpcServer.serverName() + ProtoGenConstants.PROTO_SERVER_FILE_SUFFIX;
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

      // Add the protocol-specific header (syntax/edition), package, and options
      protoBuilder.append(protocol.schemaHeader()).append("\n\n");

      protoBuilder.append("option java_package = \"").append(packageName).append("\";\n");
      if (protocol.emitJavaMultipleFiles()) {
        protoBuilder.append("option java_multiple_files = true;\n");
      }
      protoBuilder.append("\n");
      // Add documentation for the service
      protoBuilder.append(
          "// Service definition for the services defined by lattice application  %s\n"
              .formatted(context.latticeAppTypeElement().getQualifiedName().toString()));
      Set<String> imports = new LinkedHashSet<>();
      Arrays.stream(services)
          .forEach(
              serviceAnno -> {
                for (TypeMirror vajramType :
                    util.codegenUtil().getTypesFromAnnotationMember(serviceAnno::rpcVajrams)) {
                  VajramInfoLite vajramInfo =
                      util.computeVajramInfoLiteWithUpperBoundTypeArgs(
                          requireNonNull(
                              (TypeElement)
                                  util.processingEnv().getTypeUtils().asElement(vajramType)));

                  // Get the response type name (without package) to use in the service definition
                  String responseTypeName =
                      getSimpleClassName(vajramInfo.responseType().canonicalClassName());
                  // Add imports for the request and response messages
                  imports.add(
                      vajramInfo.packageName().replace('.', '/')
                          + "/"
                          + vajramInfo.vajramId().id()
                          + reqProtoFileSuffix);
                  imports.add(
                      String.join(
                          "/",
                          getPackageName(vajramInfo.responseType().canonicalClassName())
                              .orElse("")
                              .replace('.', '/'),
                          responseTypeName + modelsProtoFileSuffix));
                }
              });
      for (String anImport : imports) {
        protoBuilder.append("import \"").append(anImport).append("\";\n");
      }
      protoBuilder.append("\n");
      // Proto package is lower_snake_case (required by editions 2024+); java_package keeps the
      // original Java package above so downstream Java consumers are unaffected.
      protoBuilder
          .append("package ")
          .append(toLowerSnakeCasePackage(packageName))
          .append(";\n\n\n");

      Arrays.stream(services)
          .forEach(
              serviceAnno -> {
                List<VajramInfoLite> vajramInfos =
                    util
                        .codegenUtil()
                        .getTypesFromAnnotationMember(serviceAnno::rpcVajrams)
                        .stream()
                        .map(
                            typeMirror ->
                                util.computeVajramInfoLiteWithUpperBoundTypeArgs(
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
                      .append(reqProtoMsgSuffix)
                      .append(") \n    returns (")
                      .append(toTitleCaseProtoName(responseTypeName))
                      .append(modelsProtoMsgSuffix)
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
        util.codegenUtil()
            .error("Failed to generate service proto file %s".formatted(serviceProtoFilePath));
      }
    } catch (IOException e) {
      String message =
          String.format(
              "Error generating protobuf service definition for %s: %s",
              deepToString(services), e.getMessage());
      util.codegenUtil().error(message);
    } finally {
      util.codegenUtil().note("Create service proto file");
    }
  }
}
