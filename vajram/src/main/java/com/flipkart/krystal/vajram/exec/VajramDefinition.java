package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.DefaultInputResolver;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.InputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;

public final class VajramDefinition {

  @Getter private final Vajram<?> vajram;

  // TODO populate input resolvers from vajram
  @Getter private final ImmutableCollection<InputResolverDefinition> inputResolverDefinitions;

  public VajramDefinition(Vajram<?> vajram) {
    this.vajram = vajram;
    this.inputResolverDefinitions = ImmutableList.copyOf(parseInputResolvers(vajram));
  }

  private static Collection<InputResolverDefinition> parseInputResolvers(Vajram<?> vajram) {
    List<InputResolverDefinition> inputResolvers =
        new ArrayList<>(vajram.getSimpleInputResolvers());
    ImmutableSet<Method> resolverMethods =
        Arrays.stream(getVajramSourceClass(vajram.getClass()).getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Resolve.class))
            .collect(toImmutableSet());

    ImmutableMap<String, Dependency> inputDefinitions =
        vajram.getInputDefinitions().stream()
            .filter(vi -> vi instanceof Dependency)
            .map(vi -> (Dependency) vi)
            .collect(ImmutableMap.toImmutableMap(VajramInputDefinition::name, Function.identity()));

    for (Method resolverMethod : resolverMethods) {
      Resolve resolver = resolverMethod.getAnnotation(Resolve.class);
      String targetDependency = resolver.value();
      Dependency dependency = inputDefinitions.get(targetDependency);
      if (dependency == null) {
        throw new IllegalStateException(
            "Could not find dependency with name %s".formatted(targetDependency));
      }
      String[] targetInputs = resolver.inputs();
      ImmutableSet<String> sources =
          Arrays.stream(resolverMethod.getParameters())
              .map(Parameter::getAnnotations)
              .map(
                  annotations ->
                      Arrays.stream(annotations)
                          .filter(annotation -> annotation instanceof BindFrom)
                          .map(annotation -> (BindFrom) annotation)
                          .findAny())
              .filter(Optional::isPresent)
              .map(Optional::orElseThrow)
              .toList()
              .stream()
              .map(BindFrom::value)
              .collect(toImmutableSet());
      inputResolvers.add(
          new DefaultInputResolver(
              sources,
              new QualifiedInputs(
                  targetDependency,
                  dependency.dataAccessSpec(),
                  ImmutableSet.copyOf(targetInputs))));
    }
    return inputResolvers;
  }

  private static Class<?> getVajramSourceClass(Class<?> vajramClass) {
    Class<?> superclass = vajramClass.getSuperclass();
    if (Object.class.equals(superclass)) {
      throw new IllegalArgumentException();
    }
    if (IOVajram.class.equals(superclass) || ComputeVajram.class.equals(superclass)) {
      return vajramClass;
    }
    return getVajramSourceClass(superclass);
  }
}
