package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoGenUtility;
import com.flipkart.krystal.vajram.protobuf.util.ProtobufProtocol;
import com.flipkart.krystal.vajram.protobuf.util.SerializableProtoModel;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.MessageLite;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

@AutoService(LatticeCodeGeneratorProvider.class)
public class GrpcDopantImplGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new GrpcDopantImplGen(latticeCodegenContext);
  }

  private static final class GrpcDopantImplGen implements CodeGenerator {

    private final LatticeCodegenContext context;
    private final VajramCodeGenUtility util;

    public GrpcDopantImplGen(LatticeCodegenContext context) {
      this.context = context;
      this.util = context.codeGenUtility();
    }

    @Override
    public void generate() {
      TypeElement latticeAppElem = context.latticeAppTypeElement();
      String packageName =
          util.processingEnv()
              .getElementUtils()
              .getPackageOf(latticeAppElem)
              .getQualifiedName()
              .toString();
      GrpcServer grpcServer = latticeAppElem.getAnnotation(GrpcServer.class);
      if (!isApplicable(grpcServer)) {
        return;
      }
      LatticeCodegenUtils latticeCodegenUtils = new LatticeCodegenUtils(util.codegenUtil());
      ClassName dopantImplName =
          latticeCodegenUtils.getDopantImplName(latticeAppElem, GrpcServerDopant.class);
      TypeSpec.Builder classBuilder =
          util.codegenUtil()
              .classBuilder(
                  dopantImplName.simpleName(), latticeAppElem.getQualifiedName().toString())
              .addModifiers(Modifier.FINAL)
              .superclass(GrpcServerDopant.class);

      classBuilder.addMethod(
          latticeCodegenUtils
              .constructorOverride(GrpcServerDopant.class)
              .addModifiers(PUBLIC)
              .build());
      serviceDefinitions(grpcServer, classBuilder);

      util.codegenUtil()
          .generateSourceFile(
              dopantImplName.canonicalName(),
              JavaFile.builder(packageName, classBuilder.build()).build(),
              latticeAppElem);
    }

    @SneakyThrows
    private void serviceDefinitions(GrpcServer grpcServer, TypeSpec.Builder classBuilder) {
      TypeElement latticeAppElem = context.latticeAppTypeElement();
      String latticePackageName =
          requireNonNull(util.processingEnv().getElementUtils().getPackageOf(latticeAppElem))
              .getQualifiedName()
              .toString();

      List<CodeBlock> servicesCode = new ArrayList<>();
      for (GrpcService service : grpcServer.services()) {
        String serviceName = service.serviceName();
        ClassName grpcImplBaseClass =
            ClassName.get(latticePackageName, serviceName + "Grpc", serviceName + "ImplBase");
        List<CodeBlock> rpcMethodsCode = new ArrayList<>();
        for (TypeMirror vajram :
            util.codegenUtil().getTypesFromAnnotationMember(service::rpcVajrams)) {
          VajramInfoLite vajramInfoLite =
              util.computeVajramInfoLite(
                  (TypeElement)
                      requireNonNull(util.processingEnv().getTypeUtils().asElement(vajram)));
          ClassName requestType = vajramInfoLite.requestInterfaceClassName();
          TypeElement requestTypeElement =
              requireNonNull(
                  util.codegenUtil()
                      .processingEnv()
                      .getElementUtils()
                      .getTypeElement(requestType.canonicalName()));
          ProtobufProtocol requestProtocol =
              requireProtobufProtocol(requestTypeElement, vajramInfoLite);
          ClassName reqProtoModelType =
              util.codegenUtil().getImmutClassName(requestTypeElement, requestProtocol);

          // The protoc-generated class name strips underscores from the model simple name (proto
          // messages must be TitleCase under editions 2024+) and appends the protocol's class
          // suffix (e.g. "Proto3", "Proto"). E.g. Foo_Req -> FooReqProto3.
          ClassName protoReqMsgType =
              ClassName.get(
                  requestType.packageName(),
                  ProtoGenUtility.toTitleCaseProtoName(requestType.simpleName())
                      + requestProtocol.protoMsgSuffix());
          TypeMirror responseType =
              vajramInfoLite.responseType().javaModelType(util.processingEnv());
          TypeName protoRespMsgType;
          boolean isModel = isProtoModel(responseType);
          if (isModel) {
            TypeElement responseTypeElem =
                (TypeElement)
                    requireNonNull(util.processingEnv().getTypeUtils().asElement(responseType));
            ProtobufProtocol responseProtocol =
                requireProtobufProtocol(responseTypeElem, vajramInfoLite);
            protoRespMsgType =
                ClassName.get(
                    requireNonNull(
                            util.processingEnv().getElementUtils().getPackageOf(responseTypeElem))
                        .getQualifiedName()
                        .toString(),
                    ProtoGenUtility.toTitleCaseProtoName(
                            responseTypeElem.getSimpleName().toString())
                        + responseProtocol.protoMsgSuffix());
          } else {
            protoRespMsgType = TypeName.get(responseType);
            if (!util.codegenUtil().isRawAssignable(responseType, MessageLite.class)) {
              util.codegenUtil()
                  .error(
                      "Response type of a vajram added to a grpc service must either be a '"
                          + Model.class
                          + "' with annotation @SupportedModelProtocols({..., <a ProtobufProtocol>.class, ...}) or it must be a "
                          + MessageLite.class,
                      vajramInfoLite.vajramOrReqClass());
            }
          }
          rpcMethodsCode.add(
              CodeBlock.builder()
                  .addNamed(
"""
          @$override:T
          public void $rpcName:L(
              $protoReqMsgType:T request,
              $streamObserver:T<$protoRespMsgType:T> responseObserver) {
            executeRpc(
                /*request= */ new $reqProtoModelType:T(request),
                /*responseObserver= */ responseObserver,
                /*responseMapper= */ response -> $responseMapper:L);
          }
""",
                      Map.ofEntries(
                          entry("override", Override.class),
                          entry("rpcName", lowerCaseFirstChar(vajramInfoLite.vajramId().id())),
                          entry("protoReqMsgType", protoReqMsgType),
                          entry("reqProtoModelType", reqProtoModelType),
                          entry("streamObserver", StreamObserver.class),
                          entry("protoRespMsgType", protoRespMsgType),
                          entry("responseType", responseType),
                          entry("grpcDopant", GrpcServerDopant.class),
                          entry(
                              "responseMapper",
                              isModel
                                  ? CodeBlock.of(
"""
                    response == null
                        ? null
                        : (($T<$T>) response)
                            ._proto()
""",
                                      SerializableProtoModel.class,
                                      protoRespMsgType)
                                  : CodeBlock.of("response"))))
                  .build());
        }

        servicesCode.add(
            CodeBlock.of(
"""
        new $T(){
          $L
        }
""",
                grpcImplBaseClass,
                rpcMethodsCode.stream().collect(CodeBlock.joining("\n\n"))));
      }

      classBuilder.addMethod(
          MethodSpec.overriding(
                  util.codegenUtil()
                      .getMethod(
                          GrpcServerDopant.class,
                          GrpcServerDopant.class.getDeclaredMethod("serviceDefinitions").getName(),
                          0))
              .addCode(
"""
    return $T.of(
        $L
    );
""",
                  ImmutableList.class,
                  servicesCode.stream().collect(CodeBlock.joining(",\n")))
              .build());
    }

    private boolean isProtoModel(TypeMirror responseType) {
      return util.codegenUtil().isRawAssignable(responseType, Model.class);
    }

    /**
     * Returns the {@link ProtobufProtocol} declared in the given model's @SupportedModelProtocols.
     * Errors out if zero or multiple are declared - a grpc-exposed model must commit to exactly one
     * protobuf protocol so the dopant can wire request/response types unambiguously.
     */
    private ProtobufProtocol requireProtobufProtocol(
        TypeElement modelType, VajramInfoLite vajramInfoLite) {
      List<ProtobufProtocol> matches =
          util.codegenUtil().getModelProtocols(modelType).stream()
              .filter(ProtobufProtocol.class::isInstance)
              .map(ProtobufProtocol.class::cast)
              .toList();
      if (matches.isEmpty()) {
        util.codegenUtil()
            .error(
                "Model '"
                    + modelType.getQualifiedName()
                    + "' used in a grpc service must declare a ProtobufProtocol "
                    + "(e.g. Protobuf3 or Protobuf2024e) in @SupportedModelProtocols",
                vajramInfoLite.vajramOrReqClass());
      }
      if (matches.size() > 1) {
        util.codegenUtil()
            .error(
                "Model '"
                    + modelType.getQualifiedName()
                    + "' declares multiple ProtobufProtocols ("
                    + matches.stream().map(p -> p.getClass().getSimpleName()).toList()
                    + ") in @SupportedModelProtocols; grpc-exposed models must use exactly one",
                vajramInfoLite.vajramOrReqClass());
      }
      return matches.isEmpty() ? noOpProtobufProtocol() : matches.get(0);
    }

    /**
     * Sentinel returned only after we've already emitted an error for missing or multiple
     * ProtobufProtocols, so codegen can proceed to flush diagnostics without NPE.
     */
    private static ProtobufProtocol noOpProtobufProtocol() {
      return new ProtobufProtocol() {
        @Override
        public String modelClassesSuffix() {
          return "Proto";
        }

        @Override
        public String defaultContentType() {
          return "application/protobuf";
        }

        @Override
        public boolean modelsNeedToBePure() {
          return true;
        }

        @Override
        public String schemaHeader() {
          return "";
        }

        @Override
        public String protoFileSuffix() {
          return ".models.proto";
        }
      };
    }

    @EnsuresNonNullIf(expression = "#1", result = true)
    private boolean isApplicable(GrpcServer grpcServer) {
      CodegenPhase codegenPhase = context.codegenPhase();
      if (!FINAL.equals(codegenPhase)) {
        util.codegenUtil()
            .note(
                "Skipping Grpc Dopant Impl generation because this is not codegen phase: " + FINAL);
        return false;
      }
      if (grpcServer == null) {
        CharSequence message =
            "Skipping Grpc Dopant Impl gen because @GrpcServer missing on lattice app: "
                + context.latticeAppTypeElement().getSimpleName();
        util.codegenUtil().note(message);
        return false;
      }
      return true;
    }
  }
}
