package com.flipkart.krystal.vajram.codegen.utils;

import com.flipkart.krystal.vajram.codegen.Utils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Method;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodegenUtilsTest {

  @SneakyThrows
  @Test
  public void testMethodReturnTypeComputation() {
    Class<?> klass = Class.forName("java.util.HashMap");
    final Method getMethod = klass.getMethod("get", Object.class);
    final Method containsKey = klass.getMethod("containsKey", Object.class);
    final Method keySet = klass.getMethod("keySet");
    final Method entrySet = klass.getMethod("entrySet");

    Assertions.assertNotNull(Utils.getMethodReturnType(containsKey));
    final TypeName methodReturnType1 = Utils.getMethodReturnType(keySet);
    Assertions.assertEquals(
        ((ParameterizedTypeName) methodReturnType1).rawType, ClassName.get(Set.class));
    final TypeName methodReturnType = Utils.getMethodReturnType(entrySet);
    Assertions.assertEquals(
        ((ParameterizedTypeName) methodReturnType).rawType, ClassName.get(Set.class));

    final TypeName methodReturnType2 = Utils.getMethodReturnType(getMethod);
    Assertions.assertEquals(methodReturnType2, ClassName.get(Object.class));
  }
}
