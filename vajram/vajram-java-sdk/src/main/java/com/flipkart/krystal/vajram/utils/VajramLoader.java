package com.flipkart.krystal.vajram.utils;

import static java.lang.reflect.Modifier.isFinal;

import com.flipkart.krystal.vajram.Vajram;
import java.util.List;
import org.reflections.Reflections;

public final class VajramLoader {

  public static List<? extends Vajram<Object>> loadVajramsFromClassPath(String packagePrefix) {
    return new Reflections(packagePrefix)
        .getSubTypesOf(Vajram.class).stream()
            .filter(c -> isFinal(c.getModifiers()))
            .map(VajramLoader::initVajram)
            .toList();
  }

  public static Vajram<Object> loadVajramsFromClass(Class<? extends Vajram> clazz) {
    if (!Vajram.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException("Provided class is not a Vajram");
    }
    List<Class<? extends Vajram>> impls =
        new Reflections(clazz.getPackageName())
            .getSubTypesOf(clazz).stream()
                .filter(subclass -> isFinal(subclass.getModifiers()))
                .<Class<? extends Vajram>>map(subType -> subType.asSubclass(Vajram.class))
                .toList();
    if (impls.size() > 1) {
      throw new IllegalArgumentException(
          "Multiple Vajram Impl found in the package '%s' of the provided class"
              .formatted(clazz.getPackageName()));
    } else if (impls.isEmpty()) {
      throw new IllegalArgumentException(
          "No Vajram Impl found in the package '%s' of the provided class"
              .formatted(clazz.getPackageName()));
    }
    return initVajram(impls.get(0));
  }

  @SuppressWarnings("unchecked")
  private static Vajram<Object> initVajram(Class<? extends Vajram> aClass) {
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
