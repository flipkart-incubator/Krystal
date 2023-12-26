package com.flipkart.krystal.datatypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class StringType extends AbstractDataType<String> {

  private static final StringType INSTANCE = new StringType();

  public static StringType string() {
    return INSTANCE;
  }

  private StringType() {}

  @Override
  public Class<String> javaReflectType() {
    return String.class;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(String.class.getName()).asType();
  }
}
