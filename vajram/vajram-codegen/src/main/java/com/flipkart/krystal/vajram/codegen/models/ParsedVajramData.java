package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.codegen.Utils;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import java.util.ArrayList;
import java.util.List;
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

    List<ExecutableElement> resolveMethods = new ArrayList<>();
    ExecutableElement vajramLogic =
        getVajramLogicAndResolverMethods(vajramClass, resolveMethods, util);
    return Optional.of(
        new ParsedVajramData(
            vajramInfo.vajramId().vajramId(),
            resolveMethods,
            vajramLogic,
            vajramClass,
            packageName,
            vajramInfo.responseType()));
  }

  public static ExecutableElement getVajramLogicAndResolverMethods(
      TypeElement vajramClass, List<ExecutableElement> resolveMethods, Utils util) {
    ExecutableElement vajramLogic = null;
    for (ExecutableElement method : getStaticMethods(vajramClass)) {
      if (isResolver(method)) {
        resolveMethods.add(method);
      } else if (isOutputLogic(method)) {
        if (vajramLogic == null) {
          vajramLogic = method;
        } else {
          String errorMessage =
              "Multiple VajramLogic annotated methods (%s, %s) found in %s"
                  .formatted(
                      vajramLogic.getSimpleName(),
                      method.getSimpleName(),
                      vajramClass.getSimpleName());
          util.error(errorMessage, vajramLogic);
          util.error(errorMessage, method);
          throw new VajramValidationException(errorMessage);
        }
      }
    }
    if (vajramLogic == null) {
      String errorMessage = "Missing vajram logic method";
      util.error(errorMessage, vajramClass);
      throw new VajramValidationException(errorMessage);
    }
    return vajramLogic;
  }

  private static boolean isResolver(ExecutableElement method) {
    return method.getAnnotationsByType(Resolve.class).length == 1;
  }

  private static boolean isOutputLogic(ExecutableElement method) {
    return method.getAnnotationsByType(VajramLogic.class).length == 1;
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
