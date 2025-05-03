package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenDataType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.DataTypeFactory;
import com.google.auto.service.AutoService;
import com.google.protobuf.MessageLiteOrBuilder;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DataTypeFactory.class)
public final class ProtoDataTypeFactory implements DataTypeFactory {

  @Override
  public @Nullable CodeGenDataType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      CodeGenDataType... typeParameters) {
    if (typeParameters.length > 0) {
      // Protobuf types do not support generics
      return null;
    }
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    TypeMirror type = typeElement.asType();
    // Check if the type is a subtype of Message
    if (processingEnv.getTypeUtils().isAssignable(type, messageType(processingEnv))) {
      return Proto3DataType.create(processingEnv, canonicalClassName);
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
}
