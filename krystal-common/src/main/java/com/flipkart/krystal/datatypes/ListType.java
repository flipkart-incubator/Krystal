package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.box;
import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;

import com.google.common.primitives.Primitives;
import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListType<T> extends AbstractDataType<List<T>> {

  public static final Class<?> COLLECTION_TYPE = List.class;

  private final DataType<?> typeParam;

  public static <T> ListType<T> list(DataType<?> type) {
    return new ListType<>(type);
  }

  @Override
  public Type javaReflectType() throws ClassNotFoundException {
    Type t = typeParam.javaReflectType();
    if (t instanceof Class<?> clazz) {
      return getJavaType(COLLECTION_TYPE, Primitives.wrap(clazz));
    } else {
      return getJavaType(COLLECTION_TYPE, t);
    }
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    TypeElement typeElement =
        processingEnv.getElementUtils().getTypeElement(COLLECTION_TYPE.getName());
    return processingEnv
        .getTypeUtils()
        .getDeclaredType(typeElement, box(typeParam.javaModelType(processingEnv), processingEnv));
  }
}
