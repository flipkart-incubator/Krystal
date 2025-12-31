package com.flipkart.krystal.lattice.ext.dropwizard.codegen;

import static com.flipkart.krystal.lattice.ext.dropwizard.codegen.DwCodegenUtil.getDwAppClassName;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.flipkart.krystal.lattice.ext.rest.dropwizard.DropwizardRestApplication;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import jakarta.inject.Singleton;
import java.util.List;

@AutoService(BindingsProvider.class)
public class DropWizardBindingsProvider implements BindingsProvider {

  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    ClassName dwAppClassName = getDwAppClassName(context);
    return ImmutableList.of(
        new BindingsContainer(
            ImmutableList.of(
                new ProviderMethod(
                    dwAppClassName.simpleName(),
                    ClassName.get(DropwizardRestApplication.class),
                    List.of(ParameterSpec.builder(dwAppClassName, "app").build()),
                    CodeBlock.of("return app;"),
                    AnnotationSpec.builder(Singleton.class).build()))));
  }
}
