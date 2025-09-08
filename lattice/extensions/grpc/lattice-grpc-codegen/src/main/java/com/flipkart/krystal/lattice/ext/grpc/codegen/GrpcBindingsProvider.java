package com.flipkart.krystal.lattice.ext.grpc.codegen;

import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.REQUEST;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServer;
import com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant;
import com.flipkart.krystal.tags.Names;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import javax.lang.model.element.TypeElement;

@AutoService(BindingsProvider.class)
public final class GrpcBindingsProvider implements BindingsProvider {
  @Override
  public ImmutableList<Binding> bindings(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    GrpcServer grpcServer = latticeAppElem.getAnnotation(GrpcServer.class);
    if (grpcServer == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(getDopantBinding(context), bindAcceptHeaderInRequestScope());
  }

  private static DopantBinding getDopantBinding(LatticeCodegenContext context) {
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    LatticeCodegenUtils latticeCodegenUtils = new LatticeCodegenUtils(util);
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    return new DopantBinding(
        ClassName.get(GrpcServerDopant.class),
        latticeCodegenUtils.getDopantImplName(latticeAppElem, GrpcServerDopant.class));
  }

  private static Binding bindAcceptHeaderInRequestScope() {
    return new SimpleBinding(
        ClassName.get(Header.class),
        CodeBlock.of("$T.$L($T.$L)", Names.class, "named", StandardHeaderNames.class, "ACCEPT"),
        new BindTo.Provider(CodeBlock.of("null"), REQUEST));
  }
}
