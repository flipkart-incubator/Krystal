package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcService;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import com.flipkart.krystal.vajram.protobuf.util.ProtobufProtocol;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Resolves the {@link ProtobufProtocol} a {@code @GrpcServer} should use for codegen by inspecting
 * the response models of all RPC vajrams. Requires the response models to converge on a single
 * ProtobufProtocol.
 */
final class GrpcProtocolResolver {

  private GrpcProtocolResolver() {}

  /**
   * Returns the unique {@link ProtobufProtocol} declared by every RPC vajram's response model in
   * the given {@code grpcServer}. Errors out via {@code util} and returns null if zero or multiple
   * protocols are declared.
   */
  static @Nullable ProtobufProtocol resolveResponseProtocol(
      GrpcServer grpcServer, VajramCodeGenUtility util, Element errorAnchor) {
    CodeGenUtility codegenUtil = util.codegenUtil();
    Set<ProtobufProtocol> commonProtocols = null;
    boolean foundAny = false;
    for (GrpcService service : grpcServer.services()) {
      for (TypeMirror vajramMirror :
          codegenUtil.getTypesFromAnnotationMember(service::rpcVajrams)) {
        TypeElement vajramElement =
            (TypeElement)
                requireNonNull(util.processingEnv().getTypeUtils().asElement(vajramMirror));
        VajramInfoLite vajramInfo = util.computeVajramInfoLiteWithUpperBoundTypeArgs(vajramElement);
        TypeMirror responseType = vajramInfo.responseType().typeMirror(util.processingEnv());
        Element responseElement = util.processingEnv().getTypeUtils().asElement(responseType);
        if (!(responseElement instanceof TypeElement responseTypeElement)) {
          continue;
        }
        Set<ProtobufProtocol> protocols =
            codegenUtil.getModelProtocols(responseTypeElement).stream()
                .filter(ProtobufProtocol.class::isInstance)
                .map(ProtobufProtocol.class::cast)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        foundAny = true;
        if (commonProtocols == null) {
          commonProtocols = protocols;
        } else {
          commonProtocols.retainAll(protocols);
        }
      }
    }
    if (!foundAny) {
      return null;
    }
    if (commonProtocols == null || commonProtocols.isEmpty()) {
      codegenUtil.error(
          "Could not determine a common ProtobufProtocol across all rpc vajram response models. "
              + "All response models in a @GrpcServer must declare exactly one common "
              + "ProtobufProtocol (e.g. Protobuf3 or Protobuf2024e) in @SupportedModelProtocols.",
          errorAnchor);
      return null;
    }
    if (commonProtocols.size() > 1) {
      codegenUtil.error(
          "Multiple ProtobufProtocols are commonly supported across rpc vajram response models: "
              + List.copyOf(commonProtocols).stream()
                  .map(p -> p.getClass().getSimpleName())
                  .toList()
              + ". Each grpc app must commit to a single ProtobufProtocol.",
          errorAnchor);
      return null;
    }
    return commonProtocols.iterator().next();
  }
}
