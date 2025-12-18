package com.flipkart.krystal.lattice.ext.grpc.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.di.Binding;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ImplTypeBinding;
import com.flipkart.krystal.lattice.codegen.spi.di.NullBinding;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant;
import com.flipkart.krystal.tags.Names;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import javax.lang.model.element.TypeElement;

@AutoService(BindingsProvider.class)
public final class GrpcBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    GrpcServer grpcServer = latticeAppElem.getAnnotation(GrpcServer.class);
    if (grpcServer == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(
        new BindingsContainer(
            ImmutableList.of(getDopantBinding(context), bindAcceptHeaderInRequestScope())));
  }

  private static ImplTypeBinding getDopantBinding(LatticeCodegenContext context) {
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    LatticeCodegenUtils latticeCodegenUtils = new LatticeCodegenUtils(util);
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    return new ImplTypeBinding(
        ClassName.get(GrpcServerDopant.class),
        latticeCodegenUtils.getDopantImplName(latticeAppElem, GrpcServerDopant.class),
        AnnotationSpec.builder(ApplicationScoped.class).build());
  }

  private static Binding bindAcceptHeaderInRequestScope() {
    return new NullBinding(
        ClassName.get(Header.class),
        CodeBlock.of("$T.$L($T.$L)", Names.class, "named", StandardHeaderNames.class, "ACCEPT"),
        AnnotationSpec.builder(Named.class)
            .addMember("value", "$L", CodeBlock.of("$T.$L", StandardHeaderNames.class, "ACCEPT"))
            .build(),
        AnnotationSpec.builder(RequestScoped.class).build());
  }
}
