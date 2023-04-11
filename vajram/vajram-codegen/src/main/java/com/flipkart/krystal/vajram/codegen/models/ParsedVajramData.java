package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.codegen.utils.Constants;
import com.flipkart.krystal.vajram.inputs.ResolveDep;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ParsedVajramData(
    String vajramName,
    List<Method> resolveMethods,
    Method vajramLogic,
    Class vajramClass,
    String packageName,
    Map<String, Field> fields) {

  public static final String DOT_SEPARATOR = ".";

  public static Optional<ParsedVajramData> fromVajram(
      ClassLoader classLoader, VajramInputFile inputFile) {
    String packageName =
        CodegenUtils.getPackageFromPath(inputFile.inputFilePath().relativeFilePath());
    Class<? extends Vajram> result = null;
    ClassLoader systemClassLoader = VajramID.class.getClassLoader();
    Map<String, Field> fields = new HashMap<>();
    try {
      result =
          (Class<? extends Vajram>)
              classLoader.loadClass(packageName + DOT_SEPARATOR + inputFile.vajramName());
      String inputUtilClass =
          packageName + DOT_SEPARATOR + CodegenUtils.getInputUtilClassName(inputFile.vajramName());
      final Class<?> inputUtilCls = classLoader.loadClass(inputUtilClass);
      boolean needsModulation =
          inputFile.vajramInputsDef().inputs().stream().anyMatch(InputDef::isNeedsModulation);
      if (needsModulation) {
        final Class<?> inputsCls =
            classLoader.loadClass(
                packageName
                    + DOT_SEPARATOR
                    + CodegenUtils.getInputUtilClassName(inputFile.vajramName())
                    + Constants.DOLLAR
                    + CodegenUtils.getInputModulationClassname(inputFile.vajramName()));
        Arrays.stream(inputsCls.getDeclaredFields())
            .forEach(field -> fields.put(field.getName(), field));
      } else {
        final Class<?> allInputsCls =
            classLoader.loadClass(
                packageName
                    + DOT_SEPARATOR
                    + CodegenUtils.getInputUtilClassName(inputFile.vajramName())
                    + Constants.DOLLAR
                    + CodegenUtils.getAllInputsClassname(inputFile.vajramName()));
        Arrays.stream(allInputsCls.getDeclaredFields())
            .forEach(field -> fields.put(field.getName(), field));
      }
      String requestClass =
          packageName + DOT_SEPARATOR + CodegenUtils.getRequestClassName(inputFile.vajramName());
      classLoader.loadClass(requestClass);
    } catch (ClassNotFoundException e) {
      log.warn("Vajram class not found for {}", inputFile.vajramName(), e);
      return Optional.empty();
    }

    List<Method> resolveMethods = new ArrayList<>();
    Method vajramLogic = null;
    for (Method method : result.getDeclaredMethods()) {
      if (method.isAnnotationPresent(ResolveDep.class)) {
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
            inputFile.vajramName(), resolveMethods, vajramLogic, result, packageName, fields));
  }
}
