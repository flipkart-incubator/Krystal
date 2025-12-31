package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTOBUF_3;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_MSG_SUFFIX;
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
import com.flipkart.krystal.vajram.protobuf3.SerializableProtoModel;
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
          ClassName reqProtoModelType =
              ClassName.get(
                  vajramInfoLite.packageName(),
                  requestType.simpleName() + IMMUT_SUFFIX + PROTOBUF_3.modelClassesSuffix());

          ClassName protoReqMsgType =
              ClassName.get(
                  requestType.packageName(), requestType.simpleName() + MODELS_PROTO_MSG_SUFFIX);
          TypeMirror responseType =
              vajramInfoLite.responseType().javaModelType(util.processingEnv());
          TypeName protoRespMsgType;
          boolean isModel = isProtoModel(responseType);
          if (isModel) {
            TypeElement responseTypeElem =
                (TypeElement)
                    requireNonNull(util.processingEnv().getTypeUtils().asElement(responseType));
            protoRespMsgType =
                ClassName.get(
                    requireNonNull(
                            util.processingEnv().getElementUtils().getPackageOf(responseTypeElem))
                        .getQualifiedName()
                        .toString(),
                    responseTypeElem.getSimpleName() + MODELS_PROTO_MSG_SUFFIX);
          } else {
            protoRespMsgType = TypeName.get(responseType);
            if (!util.codegenUtil().isRawAssignable(responseType, MessageLite.class)) {
              util.codegenUtil()
                  .error(
                      "Response type of a vajram added to a grpc service must either be a '"
                          + Model.class
                          + "' with annotation @SupportedModelProtocols({..., Protobuf3.class, ...}) or it must be a "
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
