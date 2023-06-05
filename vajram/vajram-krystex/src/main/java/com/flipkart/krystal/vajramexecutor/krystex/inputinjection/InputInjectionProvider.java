package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

public interface InputInjectionProvider {

  Object getInstance(Class<?> clazz);

  Object getInstance(Class<?> clazz, String injectionName);
}
