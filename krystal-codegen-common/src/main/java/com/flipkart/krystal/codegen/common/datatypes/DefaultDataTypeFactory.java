package com.flipkart.krystal.codegen.common.datatypes;

import static com.flipkart.krystal.codegen.common.datatypes.StandardJavaType.standardTypesByCanonicalName;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DefaultDataTypeFactory implements DataTypeFactory {

  public DefaultDataTypeFactory() {}

  @Override
  public CodeGenType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      CodeGenType... typeParameters) {
    if (typeParameters.length == 0) {
      CodeGenType standardTypeInfo = standardTypesByCanonicalName.get(canonicalClassName);
      if (standardTypeInfo != null) {
        return standardTypeInfo;
      }
    }
    return JavaCodeGenType.create(canonicalClassName, ImmutableList.copyOf(typeParameters));
  }

  @Override
  public @Nullable CodeGenType create(ProcessingEnvironment processingEnv, Type type) {
    if (type instanceof Class<?> clazz) {
      return create(processingEnv, requireNonNull(clazz.getCanonicalName()));
    }
    if (type instanceof ParameterizedType parameterizedType) {
      return create(
          processingEnv,
          requireNonNull(create(processingEnv, parameterizedType.getRawType()))
              .canonicalClassName(),
          Arrays.stream(parameterizedType.getActualTypeArguments())
              .map(argType -> create(processingEnv, argType))
              .toArray(CodeGenType[]::new));
    }
    return null;
  }
}
