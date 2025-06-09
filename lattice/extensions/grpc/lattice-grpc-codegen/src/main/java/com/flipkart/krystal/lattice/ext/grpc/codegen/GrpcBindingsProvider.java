package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;

@AutoService(BindingsProvider.class)
public final class GrpcBindingsProvider implements BindingsProvider {
  public ImmutableList<Binding> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    GrpcServer grpcServer = latticeAppElem.getAnnotation(GrpcServer.class);
    if (grpcServer == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(getDopantBinding(context));
  }

  private static @NonNull DopantBinding getDopantBinding(LatticeCodegenContext context) {
    LatticeCodegenUtils latticeCodegenUtils =
        new LatticeCodegenUtils(context.codeGenUtility().codegenUtil());
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    String packageName =
        requireNonNull(
                context
                    .codeGenUtility()
                    .processingEnv()
                    .getElementUtils()
                    .getPackageOf(latticeAppElem))
            .getQualifiedName()
            .toString();
    return new DopantBinding(
        ClassName.get(GrpcServerDopant.class),
        ClassName.get(
            packageName,
            latticeCodegenUtils.getDopantImplName(latticeAppElem, GrpcServerDopant.class)));
  }
}
