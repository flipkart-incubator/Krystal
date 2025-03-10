package com.flipkart.krystal.vajram.codegen;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Boolean.TRUE;

import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.google.common.collect.ImmutableList;
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
    List<ExecutableElement> resolvers,
    @Nullable ExecutableElement outputLogic,
    String packageName,
    VajramInfo vajramInfo) {

  public static Optional<ParsedVajramData> fromVajramInfo(VajramInfo vajramInfo, Utils util) {
    validate(vajramInfo, util);
    String packageName = vajramInfo.lite().packageName();
    ImmutableList<ExecutableElement> allMethods = getAllMethods(vajramInfo.vajramClass());
    if (vajramInfo.lite().isTrait()) {
      if (!allMethods.isEmpty()) {
        throw util.errorAndThrow("A trait must not have any methods.", vajramInfo.vajramClass());
      }
    }
    for (ExecutableElement method : allMethods) {
      if ((isOutputLogic(method) || isResolver(method)) && !isStatic(method)) {
        throw util.errorAndThrow(
            "Vajram class %s has non-static method %s"
                .formatted(vajramInfo.lite().vajramId(), method.getSimpleName()),
            method);
      }
    }
    ExecutableElement outputLogic = null;
    List<ExecutableElement> resolverMethods = new ArrayList<>();
    if (vajramInfo.lite().isVajram()) {
      outputLogic = getOutputLogicAndResolverMethods(vajramInfo, resolverMethods, util);
    }
    return Optional.of(new ParsedVajramData(resolverMethods, outputLogic, packageName, vajramInfo));
  }

  private static void validate(VajramInfo vajramInfo, Utils util) {
    if (vajramInfo.lite().isTrait()) {
      TypeElement typeElement = vajramInfo.lite().vajramOrReqClass();
      CallGraphDelegationMode callGraphDelegationMode =
          typeElement.getAnnotation(CallGraphDelegationMode.class);
      if (callGraphDelegationMode == null) {
        throw util.errorAndThrow("A trait must specify a @CallGraphDelegationMode.", typeElement);
      }
    }
  }

  public static void validateNoDuplicateResolvers(
      List<ExecutableElement> methods, VajramInfo vajramInfo, Utils util) {
    Map<String, Map<String, Boolean>> lookUpMap = new HashMap<>();
    for (ExecutableElement method : methods) {
      Resolve resolve = method.getAnnotation(Resolve.class);
      String dep =
          util.extractFacetName(
              vajramInfo.lite().vajramId().vajramId(), checkNotNull(resolve).dep(), method);
      @SuppressWarnings("method.invocation")
      String depVajramId =
          vajramInfo.dependencies().stream()
              .filter(d -> d.name().equals(dep))
              .findFirst()
              .orElseThrow()
              .depVajramInfo()
              .vajramId()
              .vajramId();
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

  public static ExecutableElement getOutputLogicAndResolverMethods(
      VajramInfo vajramInfo, List<ExecutableElement> resolveMethods, Utils util) {
    ExecutableElement outputLogic = null;
    TypeElement vajramClass = vajramInfo.vajramClass();
    List<ExecutableElement> methods = getStaticMethods(vajramClass);
    for (ExecutableElement method : methods) {
      if (isResolver(method)) {
        resolveMethods.add(method);
      } else if (isOutputLogic(method)) {
        if (outputLogic == null) {
          outputLogic = method;
        } else {
          String errorMessage =
              "Multiple @Output annotated methods (%s, %s) found in %s"
                  .formatted(
                      outputLogic.getSimpleName(),
                      method.getSimpleName(),
                      vajramClass.getSimpleName());
          util.error(errorMessage, outputLogic);
          util.error(errorMessage, method);
          throw new VajramValidationException(errorMessage);
        }
      }
    }
    validateNoDuplicateResolvers(resolveMethods, vajramInfo, util);
    if (outputLogic == null) {
      String errorMessage = "Missing output logic method";
      util.error(errorMessage, vajramClass);
      throw new VajramValidationException(errorMessage);
    }
    return outputLogic;
  }

  private static boolean isResolver(ExecutableElement method) {
    return method.getAnnotationsByType(Resolve.class).length == 1;
  }

  private static boolean isOutputLogic(ExecutableElement method) {
    return method.getAnnotationsByType(Output.class).length == 1;
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
