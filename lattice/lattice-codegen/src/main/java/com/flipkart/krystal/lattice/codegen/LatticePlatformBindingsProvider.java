package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.lattice.codegen.spi.di.BindingScope.StandardBindingScope.LAZY_SINGLETON;

import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.LatticeAppConfig;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import java.util.List;

@AutoService(BindingsProvider.class)
public final class LatticePlatformBindingsProvider implements BindingsProvider {

  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    String latticeAppVarName = lowerCaseFirstChar(LatticeApplication.class.getSimpleName());
    String latticeAppBootstrapVarName =
        lowerCaseFirstChar(LatticeAppBootstrap.class.getSimpleName());
    return ImmutableList.of(
        new BindingsContainer(
            ImmutableList.of(
                new ProviderMethod(
                    LatticeAppConfig.class.getSimpleName(),
                    ClassName.get(LatticeAppConfig.class),
                    List.of(
                        ParameterSpec.builder(LatticeApplication.class, latticeAppVarName).build(),
                        ParameterSpec.builder(LatticeAppBootstrap.class, latticeAppBootstrapVarName)
                            .build()),
                    CodeBlock.of(
                        "return $L.loadConfig($L);", latticeAppVarName, latticeAppBootstrapVarName),
                    LAZY_SINGLETON))));
  }
}
