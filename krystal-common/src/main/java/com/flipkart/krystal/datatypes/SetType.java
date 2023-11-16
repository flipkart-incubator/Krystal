package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;

import com.google.common.primitives.Primitives;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SetType<T> implements DataType<Set<T>> {

  public static final Class<?> COLLECTION_TYPE = Set.class;

  private final DataType<?> typeParam;

  public static <T> SetType<T> set(DataType<?> type) {
    return new SetType<>(type);
  }

  @Override
  public Optional<Type> javaReflectType() {
    return typeParam
        .javaReflectType()
        .map(
            t -> {
              if (t instanceof Class<?> clazz) {
                return Primitives.wrap(clazz);
              } else {
                return t;
              }
            })
        .map(t -> getJavaType(COLLECTION_TYPE, t));
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    TypeElement typeElement =
        processingEnv.getElementUtils().getTypeElement(COLLECTION_TYPE.getName());
    return processingEnv
        .getTypeUtils()
        .getDeclaredType(typeElement, typeParam.javaModelType(processingEnv));
  }
}
