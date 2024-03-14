package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.codegen.Utils;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ParsedVajramData(
    String vajramName,
    List<ExecutableElement> resolvers,
    ExecutableElement outputLogic,
    TypeElement vajramClass,
    String packageName,
    DataType<?> responseType) {

  public static Optional<ParsedVajramData> fromVajram(VajramInfo vajramInfo, Utils util) {
    TypeElement vajramClass = vajramInfo.vajramClass();
    String packageName = vajramInfo.packageName();
    for (ExecutableElement method : iter(getAllMethods(vajramClass))) {
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
        getOutputLogicAndResolverMethods(vajramClass, resolverMethods, util);
    return Optional.of(
        new ParsedVajramData(
            vajramInfo.vajramId().vajramId(),
            resolverMethods,
            outputLogic,
            vajramClass,
            packageName,
            vajramInfo.responseType()));
  }

  public static void validateNoDuplicateResolvers(
      List<ExecutableElement> methods, Utils util) {
    //add comments
    Map<String, Map<String, Boolean>> lookUpMap = new HashMap<>();
    for (ExecutableElement method : methods) {
      String depName = method.getAnnotation(Resolve.class).depName();
      String[] depInputs = method.getAnnotation(Resolve.class).depInputs();
      for (String depinput : depInputs) {
        if (lookUpMap.getOrDefault(depName, Map.of()).getOrDefault(depinput, false)) {
          String errorMessage =
              "Two Resolver resolving same input (%s) for dependency name (%s)"
                  .formatted(depinput, depName);
          util.error(errorMessage, method);
          throw new VajramValidationException(errorMessage);
        }
        lookUpMap
            .computeIfAbsent(depName, k -> Map.of(depinput, true))
            .computeIfAbsent(depinput, k -> true);
      }
    }
  }

  public static ExecutableElement getOutputLogicAndResolverMethods(
      TypeElement vajramClass, List<ExecutableElement> resolveMethods, Utils util) {
    ExecutableElement outputLogic = null;
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
    validateNoDuplicateResolvers(resolveMethods, util);
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
    return getAllMethods(vajramClass)
        .filter(element -> element.getModifiers().contains(Modifier.STATIC))
        .toList();
  }

  private static Stream<ExecutableElement> getAllMethods(TypeElement vajramCalss) {
    return vajramCalss.getEnclosedElements().stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(element -> (ExecutableElement) element);
  }

  private static <T> Iterable<T> iter(Stream<T> elementStream) {
    return elementStream::iterator;
  }

  private static boolean isStatic(Element element) {
    return element.getModifiers().contains(Modifier.STATIC);
  }
}
