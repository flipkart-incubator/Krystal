package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface InputInjectionProvider {

  Object getInstance(Type type);

  Object getInstance(Type type, Annotation annotation);
}
