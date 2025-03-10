package com.flipkart.krystal.vajram.utils;

import static java.lang.reflect.Modifier.isFinal;

import com.flipkart.krystal.vajram.VajramDefRoot;
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

  public static VajramDefRoot<Object> loadVajramsFromClass(
      Class<? extends VajramDefRoot<?>> clazz) {
    List<Class<? extends VajramDefRoot>> impls =
        new Reflections(clazz.getPackageName())
            .getSubTypesOf(clazz).stream()
                .filter(subclass -> isFinal(subclass.getModifiers()))
                .<Class<? extends VajramDefRoot>>map(
                    subType -> subType.asSubclass(VajramDefRoot.class))
                .toList();
    if (impls.size() > 1) {
      throw new IllegalArgumentException(
          "Multiple Vajram Impl found in the package '%s' of the provided class: %s"
              .formatted(clazz.getPackageName(), impls));
    } else if (impls.isEmpty()) {
      throw new IllegalArgumentException(
          "No Vajram Impl found in the package '%s' of the provided class"
              .formatted(clazz.getPackageName()));
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
