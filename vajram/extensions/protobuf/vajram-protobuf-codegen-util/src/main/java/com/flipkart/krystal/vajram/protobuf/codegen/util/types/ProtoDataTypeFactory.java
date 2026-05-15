package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import static com.flipkart.krystal.vajram.protobuf.codegen.util.types.StandardProtoType.standardProtoTypesByCanonicalName;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.DataTypeFactory;
import com.google.auto.service.AutoService;
import com.google.protobuf.MessageLiteOrBuilder;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(DataTypeFactory.class)
public final class ProtoDataTypeFactory implements DataTypeFactory {

  @Override
  public @Nullable CodeGenType create(
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnv) {
    if ((typeMirror instanceof DeclaredType declaredType)
        && !declaredType.getTypeArguments().isEmpty()) {
      // Protobuf types do not support generics
      return null;
    }
    Element element = processingEnv.getTypeUtils().asElement(typeMirror);
    if (!(element instanceof TypeElement typeElement)) {
      // Protobuf types will always have type elements
      return null;
    }
    String canonicalClassName = typeElement.getQualifiedName().toString();
    CodeGenType standardType = standardProtoTypesByCanonicalName.get(canonicalClassName);
    if (standardType != null) {
      return standardType;
    }
    // Check if the type is a subtype of Message
    if (isProtoMessage(typeMirror, processingEnv)) {
      return ProtoMessageType.create(canonicalClassName);
    } else {
      // Not a protobuf message type
      return null;
    }
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
