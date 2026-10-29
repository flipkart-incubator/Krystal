package com.flipkart.krystal.vajram.codegen.common.models;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics.NoBatching;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics.WithBatching;
import com.flipkart.krystal.vajram.facets.ComputeField;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.utils.BatchingRules;
import com.flipkart.krystal.vajram.utils.FacetDependencyGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public record ParsedVajramData(
    @Nullable LogicMethods logicMethods, String packageName, VajramInfo vajramInfo) {

  public static Optional<ParsedVajramData> fromVajramInfo(
      VajramInfo vajramInfo, VajramCodeGenUtility util) {
    validate(vajramInfo, util);
    String packageName = vajramInfo.packageName();
    ImmutableList<ExecutableElement> allMethods = getAllMethods(vajramInfo.vajramClassElem());
    if (vajramInfo.lite().isTrait()) {
      if (!allMethods.isEmpty()) {
        util.codegenUtil()
            .error("A trait definition must not have any methods.", vajramInfo.vajramClassElem());
      }
    }
    for (ExecutableElement method : allMethods) {
      if ((isNonBatchedOutputLogic(method)
              || isResolver(method)
              || hasAnnotation(method, Output.Batched.class)
              || hasAnnotation(method, Output.Unbatch.class))
          && !isStatic(method)) {
        util.codegenUtil().error("@Resolve methods and @Output methods must be static", method);
      }
    }
    LogicMethods logicMethods = null;
    if (vajramInfo.lite().isVajram()) {
      logicMethods = getOutputLogicAndResolverMethods(vajramInfo, util);
    }
    return Optional.of(new ParsedVajramData(logicMethods, packageName, vajramInfo));
  }

  private static void validate(VajramInfo vajramInfo, VajramCodeGenUtility util) {
    if (vajramInfo.lite().isTrait()) {
      TypeElement typeElement = vajramInfo.lite().vajramOrReqClass();
      CallGraphDelegationMode callGraphDelegationMode =
          typeElement.getAnnotation(CallGraphDelegationMode.class);
      if (callGraphDelegationMode == null) {
        util.codegenUtil().error("A trait must specify a @CallGraphDelegationMode.", typeElement);
      }
    }
  }

  public static void validateNoDuplicateResolvers(
      List<ExecutableElement> methods, VajramInfo vajramInfo, VajramCodeGenUtility util) {
    Map<String, Map<String, Boolean>> lookUpMap = new HashMap<>();
    for (ExecutableElement method : methods) {
      Resolve resolve = method.getAnnotation(Resolve.class);
      String depName =
          util.extractFacetName(
              vajramInfo.lite().vajramId().id(), checkNotNull(resolve).dep(), method);
      @SuppressWarnings("method.invocation")
      DependencyModel dependency =
          vajramInfo.dependencies().stream()
              .filter(d -> d.name().equals(depName))
              .findFirst()
              .orElseThrow();
      for (String depInputName : getDepInputNames(method, dependency, resolve, util)) {
        if (TRUE.equals(
            lookUpMap
                .computeIfAbsent(depName, k -> new LinkedHashMap<>())
                .put(depInputName, true))) {
          String errorMessage =
              "Two Resolver resolving same input (%s) for dependency name (%s)"
                  .formatted(depInputName, depName);
          throw util.codegenUtil().errorAndThrow(errorMessage, method);
        }
      }
    }
  }

  /**
   * Returns the {@code @ComputeField} methods by the name of the field they compute. Fails if a
   * field is computed by multiple methods, or if a field does not have a compute method.
   */
  private static ImmutableMap<String, ExecutableElement> computeFieldsByName(
      List<ExecutableElement> computeFieldMethods,
      VajramInfo vajramInfo,
      VajramCodeGenUtility util) {
    Map<String, ExecutableElement> methodsByField = new LinkedHashMap<>();
    for (ExecutableElement method : computeFieldMethods) {
      String field =
          util.extractFacetName(
              vajramInfo.lite().vajramId().id(),
              checkNotNull(method.getAnnotation(ComputeField.class)).value(),
              method);
      ExecutableElement existingMethod = methodsByField.put(field, method);
      if (existingMethod != null) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Duplicate @ComputeField methods for field '%s': %s and %s"
                    .formatted(field, existingMethod.getSimpleName(), method.getSimpleName()),
                method);
      }
    }
    for (DefaultFacetModel field : vajramInfo.fields()) {
      if (!methodsByField.containsKey(field.name())) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Field facet '%s' is declared but has no corresponding @ComputeField method"
                    .formatted(field.name()),
                field.facetElement());
      }
    }
    return ImmutableMap.copyOf(methodsByField);
  }

  /**
   * Validates the dependencies between the facets which can be inferred from {@code @Resolve} and
   * {@code @ComputeField} methods: there should be no cycles and the {@link BatchingRules} should
   * be followed. Dependencies defined in {@code getSimpleInputResolvers()} cannot be inferred here
   * and are validated at runtime by {@code VajramDefinition}.
   */
  private static void validateFacetDependencies(
      VajramInfo vajramInfo,
      List<ExecutableElement> resolverMethods,
      Map<String, ExecutableElement> computeFieldMethods,
      VajramCodeGenUtility util) {
    FacetDependencyGraph graph = new FacetDependencyGraph();
    Set<String> facetNames =
        vajramInfo.facetStream().map(FacetGenModel::name).collect(toImmutableSet());
    facetNames.forEach(graph::addFacet);
    computeFieldMethods.forEach(
        (field, method) -> graph.addDependencies(field, facetParams(method, facetNames)));
    for (ExecutableElement method : resolverMethods) {
      String dependency =
          util.extractFacetName(
              vajramInfo.lite().vajramId().id(),
              checkNotNull(method.getAnnotation(Resolve.class)).dep(),
              method);
      graph.addDependencies(dependency, facetParams(method, facetNames));
    }

    graph
        .findCycle()
        .ifPresent(
            cycle -> {
              throw util.codegenUtil()
                  .errorAndThrow(
                      "Cycle detected in facet dependencies: %s"
                          .formatted(String.join(" → ", cycle)),
                      elementOf(cycle.get(0), vajramInfo));
            });

    List<BatchingRules.Violation> violations =
        BatchingRules.validate(
            vajramInfo
                .facetStream()
                .map(
                    facet ->
                        new BatchingRules.FacetInfo(
                            facet.name(),
                            facet.facetType(),
                            facet.isBatched(),
                            facet.isUsedToGroupBatches()))
                .toList(),
            graph);
    for (BatchingRules.Violation violation : violations) {
      util.codegenUtil().error(violation.message(), elementOf(violation.facet(), vajramInfo));
    }
  }

  private static List<String> facetParams(ExecutableElement method, Set<String> facetNames) {
    return method.getParameters().stream()
        .map(param -> param.getSimpleName().toString())
        .filter(facetNames::contains)
        .toList();
  }

  private static Element elementOf(String facetName, VajramInfo vajramInfo) {
    return vajramInfo
        .facetStream()
        .filter(facet -> facet.name().equals(facetName))
        .findFirst()
        .<Element>map(FacetGenModel::facetElement)
        .orElse(vajramInfo.vajramClassElem());
  }

  private static LogicMethods getOutputLogicAndResolverMethods(
      VajramInfo vajramInfo, VajramCodeGenUtility util) {
    List<ExecutableElement> resolverMethods = new ArrayList<>();
    List<ExecutableElement> computeFieldMethods = new ArrayList<>();

    boolean vajramSupportsBatching = vajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);
    ExecutableElement nonBatchedOutputLogic = null;
    ExecutableElement batchedOutputLogic = null;
    ExecutableElement unbatchOutputLogic = null;
    OutputLogics outputLogics;
    TypeElement vajramClass = vajramInfo.vajramClassElem();
    List<ExecutableElement> methods = getStaticMethods(vajramClass);
    for (ExecutableElement method : methods) {
      if (isResolver(method)) {
        resolverMethods.add(method);
      } else if (isComputeField(method)) {
        computeFieldMethods.add(method);
      } else if (isNonBatchedOutputLogic(method)) {
        if (nonBatchedOutputLogic != null) {
          String message =
              "Duplicate @Output annotated method %s".formatted(method.getSimpleName());
          util.codegenUtil().error(message, method);
        } else if (vajramSupportsBatching) {
          util.codegenUtil()
              .error(
                  "Vajram which support batching must use @Output.Batched and @Output.Unbatch instead of @Output",
                  method);
        } else {
          nonBatchedOutputLogic = method;
        }
      } else if (hasAnnotation(method, Output.Batched.class)) {
        if (batchedOutputLogic != null) {
          String message =
              "Duplicate @Output.Batched annotated method %s".formatted(method.getSimpleName());
          util.codegenUtil().error(message, method);
        } else if (!vajramSupportsBatching) {
          util.codegenUtil()
              .error(
                  "Vajram which does not support batching must use @Output instead of @Output.Batched",
                  method);
        } else {
          batchedOutputLogic = method;
        }
      } else if (hasAnnotation(method, Output.Unbatch.class)) {
        if (unbatchOutputLogic != null) {
          util.codegenUtil().error("Duplicate @Output.Unbatch annotated method", method);
        } else if (!vajramSupportsBatching) {
          util.codegenUtil()
              .error(
                  "Vajram which does not support batching must use @Output instead of @Output.Unbatch",
                  method);
        } else {
          unbatchOutputLogic = method;
        }
      }
    }
    if (!vajramSupportsBatching) {
      if (nonBatchedOutputLogic == null) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Vajram which does not support batching must have @Output annotated method",
                vajramClass);
      }
      outputLogics = new NoBatching(nonBatchedOutputLogic);
    } else {
      if (batchedOutputLogic == null || unbatchOutputLogic == null) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Vajram which supports batching must have @Output.Unbatch annotated method",
                vajramClass);
      }
      outputLogics = new WithBatching(batchedOutputLogic, unbatchOutputLogic);
    }
    validateNoDuplicateResolvers(resolverMethods, vajramInfo, util);
    ImmutableMap<String, ExecutableElement> computeFieldsByName =
        computeFieldsByName(computeFieldMethods, vajramInfo, util);
    validateFacetDependencies(vajramInfo, resolverMethods, computeFieldsByName, util);
    return new LogicMethods(outputLogics, resolverMethods, computeFieldsByName);
  }

  private static boolean isResolver(ExecutableElement method) {
    return hasAnnotation(method, Resolve.class);
  }

  private static boolean isComputeField(ExecutableElement method) {
    return hasAnnotation(method, ComputeField.class);
  }

  private static boolean isNonBatchedOutputLogic(ExecutableElement method) {
    return hasAnnotation(method, Output.class);
  }

  private static boolean hasAnnotation(
      ExecutableElement method, Class<? extends Annotation> annotationType) {
    return method.getAnnotationsByType(annotationType).length == 1;
  }

  private static List<ExecutableElement> getStaticMethods(TypeElement vajramClass) {
    return getAllMethods(vajramClass).stream()
        .filter(element -> element.getModifiers().contains(Modifier.STATIC))
        .toList();
  }

  private static ImmutableList<ExecutableElement> getAllMethods(TypeElement vajramClass) {
    return vajramClass.getEnclosedElements().stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(element -> (ExecutableElement) element)
        .collect(toImmutableList());
  }

  private static boolean isStatic(Element element) {
    return element.getModifiers().contains(Modifier.STATIC);
  }

  public static Set<String> getDepInputNames(
      ExecutableElement method,
      DependencyModel dependencyModel,
      Resolve resolve,
      VajramCodeGenUtility util) {
    VajramInputsInfo depVajramInfo = dependencyModel.depVajramInfo();
    String[] depInputs = resolve.depInputs();
    if (depInputs.length == 0) {
      // This means all depInputs need to be resolved. so infer the same
      return depVajramInfo.inputs().stream()
          .map(DefaultFacetModel::name)
          .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    } else {
      return stream(depInputs)
          .map(di -> util.extractFacetName(depVajramInfo.vajramId().id(), di, method))
          .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }
  }
}
