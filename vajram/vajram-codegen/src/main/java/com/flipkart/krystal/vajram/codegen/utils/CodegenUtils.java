package com.flipkart.krystal.vajram.codegen.utils;

import static com.flipkart.krystal.vajram.codegen.utils.Constants.COMMON_INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_NEEDING_MODULATION;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.google.common.base.CaseFormat;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CodegenUtils {

  public static final String DOT = ".";
  public static final String COMMA = ",";
  private static final Pattern COMPILE = Pattern.compile(".");
  public static final String REQUEST = "Request";
  public static final String IMPL = "Impl";
  public static final String INPUT_UTIL = "InputUtil";
  public static final String VAJRAM = "vajram";
  public static final String CONVERTER = "CONVERTER";

  private CodegenUtils() {}

  public static String getPackageFromPath(Path filePath) {
    Path parentDir =
        checkNotNull(
            filePath.getParent(),
            "Cannot get package for a file %s as it does not have a parent directory",
            filePath);
    return IntStream.range(0, parentDir.getNameCount())
        .mapToObj(i -> parentDir.getName(i).toString())
        .collect(Collectors.joining(DOT));
  }

  public static String getInputUtilClassName(String vajramName) {
    return (vajramName.toLowerCase().endsWith(VAJRAM)
            ? vajramName.substring(0, vajramName.length() - 6)
            : vajramName)
        + INPUT_UTIL;
  }

  public static String getRequestClassName(String vajramName) {
    return getVajramBaseName(vajramName) + REQUEST;
  }

  public static String getVajramImplClassName(String vajramName) {
    return vajramName + IMPL;
  }

  public static String getVajramBaseName(String vajramName) {
    return (vajramName.toLowerCase().endsWith(VAJRAM)
        ? vajramName.substring(0, vajramName.length() - 6)
        : vajramName);
  }

  public static String getVajramNameFromClass(String vajramClassFullName) {
    String[] splits = COMPILE.split(vajramClassFullName);
    return splits[splits.length - 1];
  }

  public static String toJavaName(String input) {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, input);
  }

  public static String getAllInputsClassname(String vajramName) {
    return getVajramBaseName(vajramName) + INPUTS_CLASS_SUFFIX;
  }

  public static String getCommonInputsClassname(String vajramName) {
    return getVajramBaseName(vajramName) + COMMON_INPUTS;
  }

  public static String getInputModulationClassname(String vajramName) {
    return getVajramBaseName(vajramName) + INPUTS_NEEDING_MODULATION;
  }

  public static TypeName getMethodReturnType(Method method) {
    if (method.getGenericReturnType() instanceof ParameterizedType) {
      return getType(method.getGenericReturnType());
    } else {
      return TypeName.get(method.getReturnType());
    }
  }

  public static TypeName getMethodGenericReturnType(Method method) {
    ParameterizedTypeName parameterizedTypeName;
    if (method.getGenericReturnType() instanceof ParameterizedType genericReturnType) {
      final Type[] actualTypeArguments = genericReturnType.getActualTypeArguments();
      if (actualTypeArguments.length == 1) {
        if (actualTypeArguments[0] instanceof ParameterizedType) {
          return ParameterizedTypeName.get(
              (ClassName) getType(((ParameterizedType) actualTypeArguments[0]).getRawType()),
              Arrays.stream(((ParameterizedType) actualTypeArguments[0]).getActualTypeArguments())
                  .map(CodegenUtils::getType)
                  .toArray(TypeName[]::new));
        } else {
          return getType(actualTypeArguments[0]);
        }
      } else {
        return ParameterizedTypeName.get(
            (ClassName) getType(actualTypeArguments[0]),
            Arrays.stream(actualTypeArguments)
                .skip(1)
                .map(CodegenUtils::getType)
                .toArray(TypeName[]::new));
      }
    } else {
      return ClassName.get(Primitives.wrap(method.getReturnType()));
    }
  }

  public static TypeName getClassGenericArgumentsType(Class klass) {
    if (klass.getGenericSuperclass() instanceof ParameterizedType genericReturnType) {
      final Type typeArg = genericReturnType.getActualTypeArguments()[0];
      if (typeArg instanceof ParameterizedType parameterizedType) {
        return getType(typeArg);
      } else {
        if (typeArg instanceof Class<?>) {
          return ClassName.get(Primitives.wrap((Class<?>) typeArg));
        } else {
          return ClassName.bestGuess(typeArg.getTypeName());
        }
      }
    }
    return TypeName.VOID; // TODO : check if its correct or need to throw exception
  }

  public static TypeName getType(Type typeArg) {
    if (typeArg instanceof ParameterizedType parameterizedType) {
      final Type rawType = parameterizedType.getRawType();
      final Type[] typeArgs = parameterizedType.getActualTypeArguments();
      return ParameterizedTypeName.get(
          (ClassName) getType(rawType),
          Arrays.stream(typeArgs).map(CodegenUtils::getType).toArray(TypeName[]::new));
    } else {
      if (typeArg instanceof Class<?>) {
        return ClassName.get(Primitives.wrap((Class<?>) typeArg));
      } else {
        return ClassName.bestGuess(typeArg.getTypeName());
      }
    }
  }

  public static boolean isDepResolverFanout(
      Class dependencyVajram, Method resolverMethod, String[] inputs, Map<String, Field> fields) {
    final TypeName classGenericArgumentsType =
        CodegenUtils.getClassGenericArgumentsType(dependencyVajram);
    final TypeName methodGenericReturnType = CodegenUtils.getMethodReturnType(resolverMethod);
    if (methodGenericReturnType.equals(classGenericArgumentsType)) {
      return false;
    }
    // Method is of parameterized type.
    else if (methodGenericReturnType instanceof ParameterizedTypeName parameterizedTypeName) {
      try {
        Class<?> methodRawType = null;
        if (Objects.nonNull(parameterizedTypeName.rawType.enclosingClassName())) {
          methodRawType =
              Class.forName(
                  parameterizedTypeName.rawType.enclosingClassName()
                      + "$"
                      + parameterizedTypeName.rawType.simpleName());
        } else {
          methodRawType = Class.forName(parameterizedTypeName.rawType.canonicalName());
        }
        final TypeName typeName = parameterizedTypeName.typeArguments.get(0);
        if (DependencyCommand.class.isAssignableFrom(methodRawType)) {
          // TODO : return DependencyCommand.MultiExecute.class.isAssignableFrom(methodRawType);
          return false;
        } else if (Iterable.class.isAssignableFrom(methodRawType)) {
          if (typeName instanceof ParameterizedTypeName
              && typeName.equals(classGenericArgumentsType)) {
            return true;
          } else if (ClassName.get(VajramRequest.class).equals(typeName)
              && Iterable.class.isAssignableFrom(methodRawType)) {
            return true;
          } else {
            AtomicBoolean fanout = new AtomicBoolean(false);
            Stream.of(inputs)
                .forEach(
                    input -> {
                      String key = toJavaName(input);
                      if (fields.containsKey(key)) {
                        Field field = fields.get(key);
                        if (typeName.equals(getType(field.getType())) && !fanout.get()) {
                          fanout.set(true);
                        }
                      } else {
                        log.error("Field {} not found in {}", key, dependencyVajram.getName());
                        throw new RuntimeException(
                            String.format(
                                "field %s not found in %s vajram",
                                key, dependencyVajram.getName()));
                      }
                    });
            return fanout.get();
          }
        } else {
          assert typeName instanceof ClassName;
          final Class<?> typeClass = Class.forName(((ClassName) typeName).canonicalName());
          if (typeName.equals(classGenericArgumentsType)
              && Iterable.class.isAssignableFrom(methodRawType)) {
            return true;
          } else if (VajramRequest.class.isAssignableFrom(typeClass)
              && Iterable.class.isAssignableFrom(methodRawType)) {
            return true;
          }
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    // Dependent class response is of parameterized type
    //    else if (classGenericArgumentsType instanceof ParameterizedTypeName parameterizedType) {
    //      try {
    //        final Class<?> methodRawType =
    // Class.forName(parameterizedType.rawType.canonicalName());
    //        final TypeName typeName = parameterizedType.typeArguments.get(0);
    //        AtomicBoolean fanout = new AtomicBoolean(false);
    //        Stream.of(inputs)
    //            .forEach(
    //                input -> {
    //                  String key = toJavaName(input);
    //                  if (fields.containsKey(key)) {
    //                    Field field = fields.get(toJavaName(input));
    //                    if (Iterable.class.isAssignableFrom(methodRawType)
    //                        && typeName.equals(getType(field.getType()))
    //                        && !fanout.get()) {
    //                      fanout.set(true);
    //                    }
    //                  } else {
    //                    log.error("Field {} not found in {}", key, dependencyVajram.getName());
    //                    throw new RuntimeException(
    //                        String.format(
    //                            "field %s not found in %s vajram", key,
    // dependencyVajram.getName()));
    //                  }
    //                });
    //        return fanout.get();
    //      } catch (ClassNotFoundException e) {
    //        throw new RuntimeException(e);
    //      }
    //    }
    return false;
  }
}
