package com.flipkart.krystal.lattice.codegen.spi.di;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

public record BindingsContainer(String identifier, ImmutableList<Binding> bindings) {

  public BindingsContainer(ImmutableList<Binding> bindings) {
    this("", bindings);
  }

  public static Map<String, List<BindingsContainer>> getBindingContainers(
      LatticeCodegenContext context) {
    return ServiceLoader.load(BindingsProvider.class, BindingsContainer.class.getClassLoader())
        .stream()
        .map(Provider::get)
        .map(bindingsProvider -> bindingsProvider.bindings(context))
        .flatMap(List::stream)
        .collect(Collectors.groupingBy(BindingsContainer::identifier));
  }
}
