package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.inputs.Resolve;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ParsedVajramData(
    String vajramName,
    List<Method> resolveMethods,
    Method vajramLogic,
    Class vajramClass,
    String packageName) {

  public static final String DOT_SEPARATOR = ".";

  public static Optional<ParsedVajramData> fromVajram(
      ClassLoader classLoader, VajramInputFile inputFile) {
    String packageName =
        CodegenUtils.getPackageFromPath(inputFile.inputFilePath().relativeFilePath());
    Class<? extends Vajram> result = null;
    ClassLoader systemClassLoader = VajramID.class.getClassLoader();
    try {
      result =
          (Class<? extends Vajram>)
              classLoader.loadClass(packageName + DOT_SEPARATOR + inputFile.vajramName());
      String inputUtilClass =
          packageName + DOT_SEPARATOR + CodegenUtils.getInputUtilClassName(inputFile.vajramName());
      classLoader.loadClass(inputUtilClass);
      String requestClass =
          packageName + DOT_SEPARATOR + CodegenUtils.getRequestClassName(inputFile.vajramName());
      classLoader.loadClass(requestClass);
    } catch (ClassNotFoundException e) {
      log.warn("Vajram class not found for {}", inputFile.vajramName());
      return Optional.empty();
    }

    List<Method> resolveMethods = new ArrayList<>();
    Method vajramLogic = null;
    for (Method method : result.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Resolve.class)) {
        resolveMethods.add(method);
      } else if (method.isAnnotationPresent(VajramLogic.class)) {
        if (vajramLogic == null) {
          vajramLogic = method;
        } else {
          throw new RuntimeException(
              "Multiple VajramLogic annotated methods found in "
                  + result.getClass().getSimpleName());
        }
      }
    }
    return Optional.of(
        new ParsedVajramData(
            inputFile.vajramName(), resolveMethods, vajramLogic, result, packageName));
  }
}
