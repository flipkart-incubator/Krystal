package com.flipkart.krystal.lattice.ext.guice.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionFramework;
import com.flipkart.krystal.lattice.ext.guice.GuiceFramework;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import jakarta.inject.Singleton;
import java.util.List;

@AutoService(BindingsProvider.class)
public class GuiceBindingProvider implements BindingsProvider {

  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    return ImmutableList.of(
        new BindingsContainer(
            ImmutableList.of(
                new ProviderMethod(
                    lowerCaseFirstChar(GuiceFramework.class.getSimpleName()),
                    ClassName.get(GuiceFramework.class),
                    List.of(
                        ParameterSpec.builder(
                                ClassName.get(DependencyInjectionFramework.class), "framework")
                            .build()),
                    CodeBlock.of("return ($T)framework;", GuiceFramework.class),
                    AnnotationSpec.builder(Singleton.class).build()))));
  }
}
