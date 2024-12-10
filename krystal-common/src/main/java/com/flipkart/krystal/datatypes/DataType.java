package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public sealed interface DataType<T> permits JavaType {

  /**
   * Returns the java {@link Type} corresponding to this data type. Or {@link Optional#empty()} if
   * the type is not present, or not available to the current {@link ClassLoader}
   */
  Type javaReflectType() throws ClassNotFoundException;

  TypeMirror javaModelType(ProcessingEnvironment processingEnv);
}
