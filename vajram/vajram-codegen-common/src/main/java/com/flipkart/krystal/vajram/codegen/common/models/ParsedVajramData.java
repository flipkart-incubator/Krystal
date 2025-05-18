package com.flipkart.krystal.vajram.codegen.common.models;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Boolean.TRUE;

import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics.NoBatching;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics.WithBatching;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    String packageName = vajramInfo.lite().packageName();
    ImmutableList<ExecutableElement> allMethods = getAllMethods(vajramInfo.vajramClass());
    if (vajramInfo.lite().isTrait()) {
      if (!allMethods.isEmpty()) {
        util.error("A trait definition must not have any methods.", vajramInfo.vajramClass());
      }
    }
    for (ExecutableElement method : allMethods) {
      if ((isNonBatchedOutputLogic(method)
              || isResolver(method)
              || hasAnnotation(method, Output.Batched.class)
              || hasAnnotation(method, Output.Unbatch.class))
          && !isStatic(method)) {
        util.error("@Resolve methods and @Output methods must be static", method);
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
        util.error("A trait must specify a @CallGraphDelegationMode.", typeElement);
      }
    }
  }

  public static void validateNoDuplicateResolvers(
      List<ExecutableElement> methods, VajramInfo vajramInfo, VajramCodeGenUtility util) {
    Map<String, Map<String, Boolean>> lookUpMap = new HashMap<>();
    for (ExecutableElement method : methods) {
      Resolve resolve = method.getAnnotation(Resolve.class);
      String dep =
          util.extractFacetName(
              vajramInfo.lite().vajramId().id(), checkNotNull(resolve).dep(), method);
      @SuppressWarnings("method.invocation")
      String depVajramId =
          vajramInfo.dependencies().stream()
              .filter(d -> d.name().equals(dep))
              .findFirst()
              .orElseThrow()
              .depVajramInfo()
              .vajramId()
              .id();
      List<String> depInputNames =
          Arrays.stream(resolve.depInputs())
              .map(di -> util.extractFacetName(depVajramId, di, method))
              .toList();
      for (String depInputName : depInputNames) {
        if (TRUE.equals(
            lookUpMap.computeIfAbsent(dep, k -> new LinkedHashMap<>()).put(depInputName, true))) {
          String errorMessage =
              "Two Resolver resolving same input (%s) for dependency name (%s)"
                  .formatted(depInputName, dep);
          util.error(errorMessage, method);
          throw new VajramValidationException(errorMessage);
        }
      }
    }
  }

  private static LogicMethods getOutputLogicAndResolverMethods(
      VajramInfo vajramInfo, VajramCodeGenUtility util) {
    List<ExecutableElement> resolverMethods = new ArrayList<>();

    boolean vajramSupportsBatching = vajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);
    ExecutableElement nonBatchedOutputLogic = null;
    ExecutableElement batchedOutputLogic = null;
    ExecutableElement unbatchOutputLogic = null;
    OutputLogics outputLogics;
    TypeElement vajramClass = vajramInfo.vajramClass();
    List<ExecutableElement> methods = getStaticMethods(vajramClass);
    for (ExecutableElement method : methods) {
      if (isResolver(method)) {
        resolverMethods.add(method);
      } else if (isNonBatchedOutputLogic(method)) {
        if (nonBatchedOutputLogic != null) {
          util.error(
              "Duplicate @Output annotated method %s".formatted(method.getSimpleName()), method);
        } else if (vajramSupportsBatching) {
          util.error(
              "Vajram which support batching must use @Output.Batched and @Output.Unbatch instead of @Output",
              method);
        } else {
          nonBatchedOutputLogic = method;
        }
      } else if (hasAnnotation(method, Output.Batched.class)) {
        if (batchedOutputLogic != null) {
          util.error(
              "Duplicate @Output.Batched annotated method %s".formatted(method.getSimpleName()),
              method);
        } else if (!vajramSupportsBatching) {
          util.error(
              "Vajram which does not support batching must use @Output instead of @Output.Batched",
              method);
        } else {
          batchedOutputLogic = method;
        }
      } else if (hasAnnotation(method, Output.Unbatch.class)) {
        if (unbatchOutputLogic != null) {
          util.error("Duplicate @Output.Unbatch annotated method", method);
        } else if (!vajramSupportsBatching) {
          util.error(
              "Vajram which does not support batching must use @Output instead of @Output.Unbatch",
              method);
        } else {
          unbatchOutputLogic = method;
        }
      }
    }
    if (!vajramSupportsBatching) {
      if (nonBatchedOutputLogic == null) {
        throw util.errorAndThrow(
            "Vajram which does not support batching must have @Output annotated method",
            vajramClass);
      }
      outputLogics = new NoBatching(nonBatchedOutputLogic);
    } else {
      if (batchedOutputLogic == null || unbatchOutputLogic == null) {
        throw util.errorAndThrow(
            "Vajram which supports batching must have @Output.Unbatch annotated method",
            vajramClass);
      }
      outputLogics = new WithBatching(batchedOutputLogic, unbatchOutputLogic);
    }
    validateNoDuplicateResolvers(resolverMethods, vajramInfo, util);
    return new LogicMethods(outputLogics, resolverMethods);
  }

  private static boolean isResolver(ExecutableElement method) {
    return hasAnnotation(method, Resolve.class);
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

  private static ImmutableList<ExecutableElement> getAllMethods(TypeElement vajramCalss) {
    return vajramCalss.getEnclosedElements().stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(element -> (ExecutableElement) element)
        .collect(toImmutableList());
  }

  private static boolean isStatic(Element element) {
    return element.getModifiers().contains(Modifier.STATIC);
  }
}
