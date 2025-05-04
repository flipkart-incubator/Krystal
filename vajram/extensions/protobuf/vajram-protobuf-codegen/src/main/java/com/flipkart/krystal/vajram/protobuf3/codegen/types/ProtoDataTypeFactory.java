package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import static com.flipkart.krystal.vajram.protobuf3.codegen.types.StandardProto3Type.standardProto3TypesByCanonicalName;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.DataTypeFactory;
import com.google.auto.service.AutoService;
import com.google.protobuf.MessageLiteOrBuilder;
import java.lang.reflect.Type;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DataTypeFactory.class)
public final class ProtoDataTypeFactory implements DataTypeFactory {

  @Override
  public @Nullable CodeGenType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      CodeGenType... typeParameters) {
    if (typeParameters.length > 0) {
      // Protobuf types do not support generics
      return null;
    }
    CodeGenType standardType = standardProto3TypesByCanonicalName.get(canonicalClassName);
    if (standardType != null) {
      return standardType;
    }
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    if (typeElement == null) {
      return null;
    }
    TypeMirror type = typeElement.asType();
    // Check if the type is a subtype of Message
    if (isProtoMessage(type, processingEnv)) {
      return Proto3MessageType.create(canonicalClassName);
    } else {
      // Not a protobuf message type
      return null;
    }
  }

  @Override
  public @Nullable CodeGenType create(ProcessingEnvironment processingEnv, Type type) {
    if (!(type instanceof Class<?> clazz)) {
      // All custom proto data types will be of type class
      return null;
    }
    return create(processingEnv, requireNonNull(clazz.getCanonicalName()));
  }

  private static boolean isProtoMessage(TypeMirror type, ProcessingEnvironment processingEnv) {
    return processingEnv
        .getTypeUtils()
        .isAssignable(
            type,
            requireNonNull(
                    processingEnv
                        .getElementUtils()
                        .getTypeElement(
                            requireNonNull(MessageLiteOrBuilder.class.getCanonicalName())))
                .asType());
  }
}
