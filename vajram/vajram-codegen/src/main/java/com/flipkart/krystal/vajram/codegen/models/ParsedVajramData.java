package com.flipkart.krystal.vajram.codegen.models;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.vajram.codegen.Utils;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ParsedVajramData(
    List<ExecutableElement> resolvers,
    ExecutableElement outputLogic,
    String packageName,
    VajramInfo vajramInfo) {

  public static Optional<ParsedVajramData> fromVajram(VajramInfo vajramInfo, Utils util) {
    String packageName = vajramInfo.packageName();
    for (ExecutableElement method : getAllMethods(vajramInfo.vajramClass())) {
      String errorMessage =
          "Vajram class %s has non-static method %s"
              .formatted(vajramInfo.vajramId(), method.getSimpleName());
      if ((isOutputLogic(method) || isResolver(method)) && !isStatic(method)) {
        util.error(errorMessage, method);
        throw new VajramValidationException(errorMessage);
      }
    }

    List<ExecutableElement> resolverMethods = new ArrayList<>();
    ExecutableElement outputLogic =
        getOutputLogicAndResolverMethods(vajramInfo, resolverMethods, util);
    return Optional.of(new ParsedVajramData(resolverMethods, outputLogic, packageName, vajramInfo));
  }

  public static void validateNoDuplicateResolvers(
      List<ExecutableElement> methods, VajramInfo vajramInfo, Utils util) {
    // add comments
    Map<Integer, Map<Integer, Boolean>> lookUpMap = new HashMap<>();
    for (ExecutableElement method : methods) {
      int depId = Optional.ofNullable(method.getAnnotation(Resolve.class)).orElseThrow().dep();
      int[] depInputs = method.getAnnotation(Resolve.class).depInputs();
      for (int depinput : depInputs) {
        if (lookUpMap.getOrDefault(depId, Map.of()).getOrDefault(depinput, false)) {
          String errorMessage =
              "Two Resolver resolving same input (%s) for dependency name (%s)"
                  .formatted(depinput, depId);
          util.error(errorMessage, method);
          throw new VajramValidationException(errorMessage);
        }
        lookUpMap.computeIfAbsent(depId, k -> new HashMap<>()).put(depinput, true);
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
