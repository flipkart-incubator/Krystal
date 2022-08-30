package com.flipkart.krystal.vajram.validators;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.vajram.DependencySpec;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.DefaultInputResolver;
import com.flipkart.krystal.vajram.inputs.InputId;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.QualifiedInputId;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.Reflections;

@Beta
public class ResolutionValidator {
  public static void main(String[] args) {}

  public List<String> validateInputResolutions(
      Class<? extends Vajram> vajramDefinition, boolean mandatory) {
    Map<String, Class<? extends Vajram>> vajramsById = discoverVajrams();
    String vajramId =
        getVajramId(vajramDefinition)
            .orElseThrow(
                () -> new NoSuchElementException("Vajram id missing in " + vajramDefinition));
    Vajram vajram = createVajram(vajramDefinition);
    ImmutableList<VajramInputDefinition> inputDefinitions =
        ImmutableList.copyOf(vajram.getInputDefinitions());
    Map<QualifiedInputId, InputResolver> inputResolvers =
        getInputResolvers(vajramDefinition, vajram, inputDefinitions);
    List<String> result = new ArrayList<>();
    inputDefinitions.forEach(
        input -> {
          if (input instanceof Dependency resolvedInput) {
            DependencySpec dependencySpec = resolvedInput.dependencySpec();
            if (dependencySpec instanceof VajramID vajramID) {
              String dependencyVajramId = vajramID.vajramId();
              Class<? extends Vajram> dependency = vajramsById.get(dependencyVajramId);
              @SuppressWarnings("rawtypes")
              Stream<Input> unresolvedInputsOfDependencyStream =
                  createVajram(dependency).getInputDefinitions().stream()
                      .filter(i -> i instanceof Input<?>)
                      .map(i -> (Input) i)
                      .filter(
                          unresolvedInput ->
                              Set.of(ResolutionSources.REQUEST)
                                  .equals(unresolvedInput.resolvableBy()));
              if (mandatory) {
                unresolvedInputsOfDependencyStream =
                    unresolvedInputsOfDependencyStream
                        .filter(unresolvedInput -> unresolvedInput.defaultValue() == null)
                        .filter(Input::mandatory);
              }
              for (Input<?> unresolvedInput :
                  unresolvedInputsOfDependencyStream.toList()) {
                if (inputResolvers.get(
                        new QualifiedInputId(
                            resolvedInput.name(), vajramID.vajramId(), unresolvedInput.name()))
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

  private Vajram createVajram(Class<? extends Vajram> vajramDefinition) {
    Vajram vajram;
    try {
      vajram = vajramDefinition.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not create Vajram");
    }
    return vajram;
  }

  private Map<QualifiedInputId, InputResolver> getInputResolvers(
      Class<? extends Vajram> vajramDefinition,
      Vajram vajram,
      ImmutableList<VajramInputDefinition> inputDefinitions) {
    ImmutableMap<String, VajramInputDefinition> collect =
        inputDefinitions.stream()
            .collect(toImmutableMap(VajramInputDefinition::name, Function.identity()));
    Map<QualifiedInputId, InputResolver> result = new HashMap<>();
    vajram
        .getSimpleInputResolvers()
        .forEach(
            inputResolver -> {
              QualifiedInputId qualifiedInputId = inputResolver.resolutionTarget();
              if (qualifiedInputId.vajramId() == null) {
                VajramInputDefinition vajramInputDefinition =
                    collect.get(qualifiedInputId.dependencyName());
                if (vajramInputDefinition instanceof Dependency resolvedInput) {
                  DependencySpec dependencySpec = resolvedInput.dependencySpec();
                  if (dependencySpec instanceof VajramID vajramID) {
                    qualifiedInputId =
                        new QualifiedInputId(
                            qualifiedInputId.dependencyName(),
                            vajramID.vajramId(),
                            qualifiedInputId.inputName());
                  }
                }
              }
              result.put(qualifiedInputId, inputResolver);
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
                          if (vajramInputDefinition instanceof Dependency resolvedInput) {
                            DependencySpec dependencySpec = resolvedInput.dependencySpec();
                            if (dependencySpec instanceof VajramID vajramID) {
                              QualifiedInputId target =
                                  new QualifiedInputId(dependencyName, vajramID.vajramId(), input);
                              Set<InputId> sources =
                                  Arrays.stream(method.getParameters())
                                      .map(parameter -> parameter.getAnnotation(BindFrom.class))
                                      .filter(Objects::nonNull)
                                      .map(
                                          bindFrom ->
                                              new InputId(
                                                  getVajramId(vajramDefinition).orElseThrow(),
                                                  bindFrom.value()))
                                      .collect(Collectors.toSet());
                              result.put(
                                  target,
                                  new DefaultInputResolver(
                                      sources,
                                      target,
                                      args -> {
                                        try {
                                          return method.invoke(vajram, args);
                                        } catch (Exception e) {
                                          throw new RuntimeException(e);
                                        }
                                      }));
                            }
                          }
                        });
              }
            });

    return result;
  }

  private Map<String, Class<? extends Vajram>> discoverVajrams() {
    Map<String, Class<? extends Vajram>> result = new HashMap<>();
    new Reflections("com.flipkart")
        .getSubTypesOf(Vajram.class)
        .forEach(aClass -> getVajramId(aClass).ifPresent(s -> result.put(s, aClass)));
    return result;
  }

  private Optional<String> getVajramId(Class<? extends Vajram> aClass) {
    return Optional.ofNullable(aClass.getAnnotation(VajramDef.class)).map(VajramDef::value);
  }
}
