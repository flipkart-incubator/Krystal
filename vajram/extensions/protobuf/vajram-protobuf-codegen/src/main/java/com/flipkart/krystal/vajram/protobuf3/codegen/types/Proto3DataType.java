package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.datatypes.DataType;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.MessageLiteOrBuilder;
import java.lang.reflect.Type;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class Proto3DataType<T> implements DataType<T> {

  @Getter private final String canonicalClassName;
  private @MonotonicNonNull Class<? extends MessageLiteOrBuilder> clazz;

  private Proto3DataType(String canonicalClassName) {
    this.canonicalClassName = canonicalClassName;
  }

  private Proto3DataType(Class<? extends MessageLiteOrBuilder> clazz) {
    this.canonicalClassName = clazz.getCanonicalName();
    this.clazz = clazz;
  }

  static <T> Proto3DataType<T> create(String canonicalClassName) {
    return new Proto3DataType<>(canonicalClassName);
  }

  @Override
  public Type javaReflectType() throws ClassNotFoundException {
    if (clazz != null) {
      return clazz;
    }
    throw new ClassNotFoundException();
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(canonicalClassName).asType();
  }

  @Override
  public DataType<T> rawType() {
    return this;
  }

  @Override
  public ImmutableList<DataType<?>> typeParameters() {
    return ImmutableList.of();
  }

  @Override
  public @NonNull T getPlatformDefaultValue()
      throws ClassNotFoundException, IllegalArgumentException {
    return null;
  }

  @Override
  public boolean hasPlatformDefaultValue(ProcessingEnvironment processingEnv) {
    return false;
  }
}
