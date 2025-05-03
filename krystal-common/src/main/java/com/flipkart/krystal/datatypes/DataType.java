package com.flipkart.krystal.datatypes;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface DataType<T> {

  /**
   * Returns the java {@link Type} corresponding to this data type.
   *
   * @throws ClassNotFoundException if the reflection type is not present, or not available to the
   *     current {@link ClassLoader}
   */
  Type javaReflectType() throws ClassNotFoundException;

  String canonicalClassName();

  ImmutableList<DataType<?>> typeParameters();
}
