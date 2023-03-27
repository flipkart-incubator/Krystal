package com.flipkart.krystal.vajram.codegen.utils;

import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import lombok.SneakyThrows;
import org.gradle.internal.impldep.org.junit.Assert;
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

    Assert.assertNotNull(CodegenUtils.getMethodReturnType(containsKey));
    final TypeName methodReturnType1 = CodegenUtils.getMethodReturnType(keySet);
    Assert.assertTrue(
        ((ParameterizedTypeName) methodReturnType1).rawType.equals(ClassName.get(Set.class)));
    final TypeName methodReturnType = CodegenUtils.getMethodReturnType(entrySet);
    Assert.assertTrue(
        ((ParameterizedTypeName) methodReturnType).rawType.equals(ClassName.get(Set.class)));

    final TypeName methodReturnType2 = CodegenUtils.getMethodReturnType(getMethod);
    Assert.assertTrue(((ClassName) methodReturnType2).equals(ClassName.get(Object.class)));

    final TypeName classGenericArgumentsType =
        CodegenUtils.getClassGenericArgumentsType(ClassTest1.class);
    Assert.assertTrue(
        ((ParameterizedTypeName) classGenericArgumentsType)
            .rawType.equals(ClassName.get(Set.class)));

    //    final Type methodGenericType = CodegenUtils.getMethodGenericReturnType(g)

    final Class<?> aClass = Class.forName("com.flipkart.krystal.vajram.codegen.utils.ClassTest1");
    final Type genericSuperclass = aClass.getGenericSuperclass();
    if (genericSuperclass instanceof ParameterizedType parameterizedType) {
      final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    }
  }

  @Test
  public void testGenericTypeComparison() throws ClassNotFoundException, NoSuchMethodException {
    final Class<?> aClass = Class.forName("com.flipkart.krystal.vajram.codegen.utils.ClassTest1");
    Assert.assertTrue(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("fanoutMethod1"), EMPTY_INPUTS, Collections.emptyMap()));
    Assert.assertTrue(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("fanoutMethod2"), EMPTY_INPUTS, Collections.emptyMap()));
    Assert.assertFalse(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("fanoutMethod3"), EMPTY_INPUTS, Collections.emptyMap()));
    Assert.assertFalse(
        CodegenUtils.isDepResolverFanout(
            aClass, aClass.getMethod("method1"), EMPTY_INPUTS, Collections.emptyMap()));
  }

  @Test
  public void testFieldFetch() throws ClassNotFoundException {
    final Class<?> aClass = Class.forName("com.flipkart.krystal.vajram.codegen.utils.Test1$Test2");
    Arrays.stream(aClass.getDeclaredFields()).forEach(System.out::println);
  }
}

interface ITest<E> {}

class ClassTest<E> implements ITest<E> {}

class ClassTest1 extends ClassTest<Set<String>> {
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
    return DependencyCommand.multiExecuteWith(Collections.emptySet());
  }
}

final class Test1 {
  static final class Test2 {
    private String name;
  }
}
