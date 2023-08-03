package com.flipkart.krystal.vajram.codegen.utils;

import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;

import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodegenUtilsTest {

  public static final String[] EMPTY_INPUTS = new String[0];

  @SneakyThrows
  @Test
  public void testMethodReturnTypeComputation() throws Exception {
    Class<?> klass = Class.forName("java.util.HashMap");
    final Method getMethod = klass.getMethod("get", Object.class);
    final Method containsKey = klass.getMethod("containsKey", Object.class);
    final Method keySet = klass.getMethod("keySet");
    final Method entrySet = klass.getMethod("entrySet");

    Assertions.assertNotNull(CodegenUtils.getMethodReturnType(containsKey));
    final TypeName methodReturnType1 = CodegenUtils.getMethodReturnType(keySet);
    Assertions.assertEquals(
        ((ParameterizedTypeName) methodReturnType1).rawType, ClassName.get(Set.class));
    final TypeName methodReturnType = CodegenUtils.getMethodReturnType(entrySet);
    Assertions.assertEquals(
        ((ParameterizedTypeName) methodReturnType).rawType, ClassName.get(Set.class));

    final TypeName methodReturnType2 = CodegenUtils.getMethodReturnType(getMethod);
    Assertions.assertEquals(methodReturnType2, ClassName.get(Object.class));

    final TypeName classGenericArgumentsType =
        CodegenUtils.getClassGenericArgumentsType(ClassTest1.class);
    Assertions.assertEquals(
        ((ParameterizedTypeName) classGenericArgumentsType).rawType, ClassName.get(Set.class));

    //    final Type methodGenericType = CodegenUtils.getMethodGenericReturnType(g)

    final Class<?> aClass =
        Class.forName("com.flipkart.krystal.vajram.codegen.utils.CodegenUtilsTest$ClassTest1");
    final Type genericSuperclass = aClass.getGenericSuperclass();
    if (genericSuperclass instanceof ParameterizedType parameterizedType) {
      final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    }
  }

  @Test
  public void testGenericTypeComparison() throws ClassNotFoundException, NoSuchMethodException {
    final Class<?> aClass =
        Class.forName("com.flipkart.krystal.vajram.codegen.utils.CodegenUtilsTest$ClassTest1");
    Assertions.assertTrue(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("fanoutMethod1"), EMPTY_INPUTS, Collections.emptyMap()));
    Assertions.assertTrue(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("fanoutMethod2"), EMPTY_INPUTS, Collections.emptyMap()));
    Assertions.assertFalse(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("fanoutMethod3"), EMPTY_INPUTS, Collections.emptyMap()));
    Assertions.assertFalse(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("method1"), EMPTY_INPUTS, Collections.emptyMap()));
  }

  interface ITest<E> {}

  static class ClassTest<E> implements ITest<E> {}

  static class ClassTest1 extends ClassTest<Set<String>> {
    public Set<String> method1() {
      return Collections.emptySet();
    }

    public Set<Set<String>> fanoutMethod1() {
      return Collections.emptySet();
    }

    public Set<VajramRequest> fanoutMethod2() {
      return Collections.emptySet();
    }

    public DependencyCommand<String> fanoutMethod3() {
      return executeFanoutWith(Collections.emptySet());
    }
  }

  static final class Test1 {
    static final class Test2 {
      private String name;
    }
  }
}
