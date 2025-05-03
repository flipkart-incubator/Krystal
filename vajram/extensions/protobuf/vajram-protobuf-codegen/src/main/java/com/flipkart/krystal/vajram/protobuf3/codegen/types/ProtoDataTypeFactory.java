package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.DataTypeFactory;
import com.google.auto.service.AutoService;
import com.google.protobuf.MessageLiteOrBuilder;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DataTypeFactory.class)
public final class ProtoDataTypeFactory implements DataTypeFactory {

  @Override
  public @Nullable <T> DataType<T> create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      DataType<?>... typeParameters) {
    if (typeParameters.length > 0) {
      // Protobuf types do not support generics
      return null;
    }
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    TypeMirror type = typeElement.asType();
    if (processingEnv.getTypeUtils().isAssignable(type, messageType(processingEnv))) {
      // Check if the type is a subtype of Message
      return Proto3DataType.create(canonicalClassName);
    } else {
      // Not a protobuf message type
      return null;
    }
  }

  private static TypeMirror messageType(ProcessingEnvironment processingEnv) {
    return processingEnv
        .getElementUtils()
        .getTypeElement(MessageLiteOrBuilder.class.getCanonicalName())
        .asType();
  }

  @Override
  public @Nullable <T> DataType<T> create(Class<?> clazz, DataType<?>... typeParams) {
    return null;
  }
}
