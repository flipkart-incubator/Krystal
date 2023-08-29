package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.codegen.utils.Constants;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
    Class<? extends Vajram<?>> vajramClass,
    String packageName,
    Map<String, Field> fields) {

  public static final String DOT_SEPARATOR = ".";

  public static Optional<ParsedVajramData> fromVajram(
      ClassLoader classLoader, VajramInputFile inputFile) {
    String packageName =
        CodegenUtils.getPackageFromPath(inputFile.inputFilePath().relativeFilePath());
    Class<? extends Vajram<?>> result;
    Map<String, Field> fields = new HashMap<>();
    try {
      //noinspection unchecked
      result =
          (Class<? extends Vajram<?>>)
              classLoader.loadClass(packageName + DOT_SEPARATOR + inputFile.vajramName());

      for (Method method : result.getDeclaredMethods()) {
        if ((method.isAnnotationPresent(VajramLogic.class)
                || method.isAnnotationPresent(Resolve.class))
            && !Modifier.isStatic(method.getModifiers()))
          throw new VajramValidationException(
              "Vajram class %s has non-static method %s"
                  .formatted(inputFile.vajramName(), method.getName()));
      }

      boolean needsModulation = false;
      for (InputDef inputDef : inputFile.vajramInputsDef().inputs()) {
        if (inputDef.isNeedsModulation()) {
          needsModulation = true;
          break;
        }
      }
      if (needsModulation) {
        final Class<?> inputsCls =
            classLoader.loadClass(
                packageName
                    + DOT_SEPARATOR
                    + CodegenUtils.getInputUtilClassName(inputFile.vajramName())
                    + Constants.DOLLAR
                    + CodegenUtils.getInputModulationClassname(inputFile.vajramName()));
        for (Field field : inputsCls.getDeclaredFields()) {
          fields.put(field.getName(), field);
        }
      } else {
        final Class<?> allInputsCls =
            classLoader.loadClass(
                packageName
                    + DOT_SEPARATOR
                    + CodegenUtils.getInputUtilClassName(inputFile.vajramName())
                    + Constants.DOLLAR
                    + CodegenUtils.getAllInputsClassname(inputFile.vajramName()));
        for (Field field : allInputsCls.getDeclaredFields()) {
          fields.put(field.getName(), field);
        }
      }
      String requestClass =
          packageName + DOT_SEPARATOR + CodegenUtils.getRequestClassName(inputFile.vajramName());
      classLoader.loadClass(requestClass);
    } catch (ClassNotFoundException e) {
      log.warn("Vajram class not found for {}", inputFile.vajramName(), e);
      return Optional.empty();
    }

    List<Method> resolveMethods = new ArrayList<>();
    Method vajramLogic = getVajramLogicAndResolverMethods(result, resolveMethods);
    return Optional.of(
        new ParsedVajramData(
            inputFile.vajramName(), resolveMethods, vajramLogic, result, packageName, fields));
  }

  public static Method getVajramLogicAndResolverMethods(
      Class<? extends Vajram<?>> vajramCalss, List<Method> resolveMethods) {
    Method vajramLogic = null;
    for (Method method : vajramCalss.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Resolve.class)) {
        resolveMethods.add(method);
      } else if (method.isAnnotationPresent(VajramLogic.class)) {
        if (vajramLogic == null) {
          vajramLogic = method;
        } else {
          throw new VajramValidationException(
              "Multiple VajramLogic annotated methods found in " + vajramCalss.getSimpleName());
        }
      }
    }
    if (vajramLogic == null) {
      throw new VajramValidationException("Missing vajram logic method");
    }
    return vajramLogic;
  }
}
