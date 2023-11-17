package com.flipkart.krystal.vajram.codegen.utils;

import static com.flipkart.krystal.vajram.codegen.utils.Constants.COMMON_INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_NEEDING_MODULATION;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flipkart.krystal.datatypes.DataType;
import com.google.common.base.CaseFormat;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CodegenUtils {

  public static final String DOT = ".";
  public static final String COMMA = ",";
  private static final Pattern COMPILE = Pattern.compile(".");
  public static final String REQUEST_SUFFIX = "Request";
  public static final String IMPL = "Impl";
  public static final String INPUT_UTIL = "InputUtil";
  public static final String VAJRAM = "vajram";
  public static final String CONVERTER = "CONVERTER";
  private final Types typeUtils;
  private final Elements elementUtils;
  private final ProcessingEnvironment processingEnv;

  public CodegenUtils(ProcessingEnvironment processingEnv) {
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.processingEnv = processingEnv;
  }

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
    return getVajramBaseName(vajramName) + REQUEST_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + IMPL;
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
      return toTypeName(method.getGenericReturnType());
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
              (ClassName) toTypeName(((ParameterizedType) actualTypeArguments[0]).getRawType()),
              Arrays.stream(((ParameterizedType) actualTypeArguments[0]).getActualTypeArguments())
                  .map(CodegenUtils::toTypeName)
                  .toArray(TypeName[]::new));
        } else {
          return toTypeName(actualTypeArguments[0]);
        }
      } else {
        return ParameterizedTypeName.get(
            (ClassName) toTypeName(actualTypeArguments[0]),
            Arrays.stream(actualTypeArguments)
                .skip(1)
                .map(CodegenUtils::toTypeName)
                .toArray(TypeName[]::new));
      }
    } else {
      return ClassName.get(Primitives.wrap(method.getReturnType()));
    }
  }

  public TypeName toTypeName(DataType<?> dataType) {
    return TypeName.get(toTypeMirror(dataType));
  }

  public TypeMirror toTypeMirror(DataType<?> dataType) {
    return dataType.javaModelType(processingEnv);
  }

  public static TypeName toTypeName(Type typeArg) {
    if (typeArg instanceof ParameterizedType parameterizedType) {
      final Type rawType = parameterizedType.getRawType();
      final Type[] typeArgs = parameterizedType.getActualTypeArguments();
      return ParameterizedTypeName.get(
          (ClassName) toTypeName(rawType),
          Arrays.stream(typeArgs).map(CodegenUtils::toTypeName).toArray(TypeName[]::new));
    } else {
      if (typeArg instanceof Class<?>) {
        return ClassName.get(Primitives.wrap((Class<?>) typeArg));
      } else {
        return ClassName.bestGuess(typeArg.getTypeName());
      }
    }
  }

  public static List<? extends TypeMirror> getTypeParameters(TypeMirror returnType) {
    return returnType.accept(
        new SimpleTypeVisitor14<List<? extends TypeMirror>, Void>() {
          @Override
          public List<? extends TypeMirror> visitDeclared(DeclaredType t, Void unused) {
            return t.getTypeArguments();
          }
        },
        null);
  }

  /**
   * @return true of the raw type (without generics) of {@code from} can be assigned to the raw type
   *     of {@code to}
   */
  public boolean isRawAssignable(TypeMirror from, Class<?> to) {
    return typeUtils.isAssignable(
        typeUtils.erasure(from),
        typeUtils.erasure(elementUtils.getTypeElement(to.getName()).asType()));
  }

  public TypeMirror wrap(TypeMirror type) {
    if (type instanceof PrimitiveType p) {
      return typeUtils.boxedClass(p).asType();
    } else {
      return type;
    }
  }
}
