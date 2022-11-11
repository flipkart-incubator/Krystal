package com.flipkart.krystal.vajram.validators;

import static com.flipkart.krystal.vajram.Vajrams.getVajramId;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.DefaultInputResolver;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.Reflections;

@Beta
public class ResolutionValidator {
  public static void main(String[] args) {
    Map<String, Class<? extends Vajram<?>>> vajramsById = discoverVajrams();
    ResolutionValidator resolutionValidator = new ResolutionValidator();
    Set<String> failures =
        vajramsById.values().stream()
            .map(aClass -> resolutionValidator.validateInputResolutions(aClass, true))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    if (failures.isEmpty()) {
      System.out.println("No errors in mandatory dependencies");
      failures =
          vajramsById.values().stream()
              .map(aClass -> resolutionValidator.validateInputResolutions(aClass, false))
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
      if (failures.isEmpty()) {
        System.out.println("No errors in optional dependencies");
      } else {
        System.out.println("Errors in optional dependencies");
        failures.forEach(System.out::println);
      }
    } else {
      System.out.println("Errors in mandatory dependencies:");
      failures.forEach(System.out::println);
    }
  }

  public List<String> validateInputResolutions(
      Class<? extends Vajram<?>> vajramDefinition, boolean valdiateOnlyMandatory) {
    Map<String, Class<? extends Vajram<?>>> vajramsById = discoverVajrams();
    String vajramId =
        getVajramId(vajramDefinition)
            .orElseThrow(
                () -> new NoSuchElementException("Vajram id missing in " + vajramDefinition));
    Vajram<?> vajram = createVajram(vajramDefinition);
    ImmutableList<VajramInputDefinition> inputDefinitions =
        ImmutableList.copyOf(vajram.getInputDefinitions());
    Map<QualifiedInputs, InputResolver> inputResolvers =
        getInputResolvers(vajramDefinition, vajram, inputDefinitions);
    List<String> result = new ArrayList<>();
    inputDefinitions.forEach(
        input -> {
          if (input instanceof Dependency resolvedInput) {
            DataAccessSpec dataAccessSpec = resolvedInput.dataAccessSpec();
            if (dataAccessSpec instanceof VajramID vajramID) {
              String dependencyVajramId = vajramID.vajramId();
              Class<? extends Vajram<?>> dependency = vajramsById.get(dependencyVajramId);
              @SuppressWarnings("rawtypes")
              Stream<Input> unresolvedInputsOfDependencyStream =
                  createVajram(dependency).getInputDefinitions().stream()
                      .filter(i -> i instanceof Input<?>)
                      .map(i -> (Input) i)
                      .filter(
                          unresolvedInput ->
                              Set.of(ResolutionSources.REQUEST)
                                  .equals(unresolvedInput.resolvableBy()));
              if (valdiateOnlyMandatory) {
                unresolvedInputsOfDependencyStream =
                    unresolvedInputsOfDependencyStream
                        .filter(unresolvedInput -> unresolvedInput.defaultValue() == null)
                        .filter(Input::mandatory);
              }
              for (Input<?> unresolvedInput : unresolvedInputsOfDependencyStream.toList()) {
                if (inputResolvers.get(
                        new QualifiedInputs(resolvedInput.name(), vajramID, unresolvedInput.name()))
                    == null) {
                  result.add(
                      "%s: Input Resolver missing for Unresolved input %s of input named %s of type %s"
                          .formatted(
                              vajramId,
                              unresolvedInput.name(),
                              resolvedInput.name(),
                              dependencyVajramId));
                }
              }
            }
          }
        });
    return result;
  }

  private Vajram<?> createVajram(Class<? extends Vajram<?>> vajramDefinition) {
    Vajram<?> vajram;
    try {
      vajram = vajramDefinition.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not create Vajram");
    }
    return vajram;
  }

  private Map<QualifiedInputs, InputResolver> getInputResolvers(
      Class<? extends Vajram<?>> vajramDefinition,
      Vajram<?> vajram,
      ImmutableList<VajramInputDefinition> inputDefinitions) {
    ImmutableMap<String, VajramInputDefinition> collect =
        inputDefinitions.stream()
            .collect(toImmutableMap(VajramInputDefinition::name, Function.identity()));
    Map<QualifiedInputs, InputResolver> result = new HashMap<>();
    vajram
        .getSimpleInputResolvers()
        .forEach(
            inputResolver -> {
              QualifiedInputs qualifiedInputs = inputResolver.resolutionTarget();
              if (qualifiedInputs.spec() == null) {
                VajramInputDefinition vajramInputDefinition =
                    collect.get(qualifiedInputs.dependencyName());
                if (vajramInputDefinition instanceof Dependency resolvedInput) {
                  DataAccessSpec dataAccessSpec = resolvedInput.dataAccessSpec();
                  if (dataAccessSpec instanceof VajramID vajramID) {
                    qualifiedInputs =
                        new QualifiedInputs(
                            qualifiedInputs.dependencyName(),
                            vajramID,
                            qualifiedInputs.inputNames());
                  }
                }
              }
              result.put(qualifiedInputs, inputResolver);
            });

    Arrays.stream(vajramDefinition.getMethods())
        .forEach(
            method -> {
              Resolve resolveDef = method.getAnnotation(Resolve.class);
              if (resolveDef != null && resolveDef.inputs().length > 0) {
                String dependencyName = resolveDef.value();
                String[] inputs = resolveDef.inputs();
                Arrays.stream(inputs)
                    .forEach(
                        input -> {
                          VajramInputDefinition vajramInputDefinition = collect.get(dependencyName);
                          if (vajramInputDefinition instanceof Dependency dependency) {
                            DataAccessSpec dataAccessSpec = dependency.dataAccessSpec();
                            if (dataAccessSpec instanceof VajramID vajramID) {
                              QualifiedInputs target =
                                  new QualifiedInputs(dependencyName, vajramID, input);
                              ImmutableSet<String> sources =
                                  Arrays.stream(method.getParameters())
                                      .map(parameter -> parameter.getAnnotation(BindFrom.class))
                                      .filter(Objects::nonNull)
                                      .map(BindFrom::value)
                                      .collect(toImmutableSet());
                              result.put(target, new DefaultInputResolver(sources, target));
                            }
                          }
                        });
              }
            });

    return result;
  }

  private static Map<String, Class<? extends Vajram<?>>> discoverVajrams() {
    Map<String, Class<? extends Vajram<?>>> result = new HashMap<>();
    //noinspection unchecked
    new Reflections("com.flipkart")
        .getSubTypesOf(Vajram.class)
        .forEach(
            aClass ->
                getVajramId(aClass)
                    .ifPresent(s -> result.put(s, (Class<? extends Vajram<?>>) aClass)));
    return result;
  }
}
