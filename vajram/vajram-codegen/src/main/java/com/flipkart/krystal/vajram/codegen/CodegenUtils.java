package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_INPUTS;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUTS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUTS_NEEDING_MODULATION;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flipkart.krystal.datatypes.DataType;
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
  public static final String REQUEST_SUFFIX = "Request";
  public static final String IMPL = "Impl";
  public static final String INPUT_UTIL = "InputUtil";
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
    return vajramName + INPUT_UTIL;
  }

  public static String getRequestClassName(String vajramName) {
    return vajramName + REQUEST_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + IMPL;
  }

  public static String getAllInputsClassname(String vajramName) {
    return vajramName + INPUTS_CLASS_SUFFIX;
  }

  public static String getCommonInputsClassname(String vajramName) {
    return vajramName + COMMON_INPUTS;
  }

  public static String getInputModulationClassname(String vajramName) {
    return vajramName + INPUTS_NEEDING_MODULATION;
  }

  public static TypeName getMethodReturnType(Method method) {
    if (method.getGenericReturnType() instanceof ParameterizedType) {
      return toTypeName(method.getGenericReturnType());
    } else {
      return TypeName.get(method.getReturnType());
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

  public TypeMirror box(TypeMirror type) {
    if (type instanceof PrimitiveType p) {
      return typeUtils.boxedClass(p).asType();
    } else {
      return type;
    }
  }
}
