package com.flipkart.krystal.vajram.utils;

import static java.lang.reflect.Modifier.isFinal;

import com.flipkart.krystal.vajram.VajramDefRoot;
import java.util.Arrays;
import java.util.List;
import org.reflections.Reflections;

public final class VajramLoader {

  public static List<? extends VajramDefRoot<Object>> loadVajramsFromClassPath(
      String packagePrefix) {
    return new Reflections(packagePrefix)
        .getSubTypesOf(VajramDefRoot.class).stream()
            .filter(c -> isFinal(c.getModifiers()))
            .map(VajramLoader::initVajram)
            .toList();
  }

  public static VajramDefRoot<Object> createVajramObjectForClass(
      Class<? extends VajramDefRoot<?>> clazz) {
    List<Class<? extends VajramDefRoot>> impls =
        new Reflections(clazz.getPackageName())
            .getSubTypesOf(clazz).stream()
                .filter(subclass -> isFinal(subclass.getModifiers()))
                .filter(
                    // Since multiple vajrams can implement a Trait, we need to pick only those
                    // class which are immediate subtypes of the requested class to get the actual
                    // wrapper class which can used to create the vajram object
                    subType ->
                        clazz.equals(subType.getSuperclass())
                            || Arrays.asList(subType.getInterfaces()).contains(clazz))
                .<Class<? extends VajramDefRoot>>map(
                    subType -> subType.asSubclass(VajramDefRoot.class))
                .toList();
    if (impls.size() > 1) {
      throw new IllegalArgumentException(
          "Multiple Vajram Impl found in the package '%s' of the provided class: %s"
              .formatted(clazz.getPackageName(), impls));
    } else if (impls.isEmpty()) {
      throw new IllegalArgumentException(
          "No Vajram Impl found in the package '%s' of the provided class: '%s'"
              .formatted(clazz.getPackageName(), clazz));
    }
    return initVajram(impls.get(0));
  }

  @SuppressWarnings("unchecked")
  private static VajramDefRoot<Object> initVajram(Class<? extends VajramDefRoot> aClass) {
    if (!isFinal(aClass.getModifiers())) {
      throw new RuntimeException("Provided Vajram impl class should be final");
    }
    try {
      return aClass.getConstructor().newInstance();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private VajramLoader() {}
}
