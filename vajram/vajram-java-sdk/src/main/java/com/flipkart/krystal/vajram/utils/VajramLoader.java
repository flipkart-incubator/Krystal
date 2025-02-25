package com.flipkart.krystal.vajram.utils;

import static java.lang.reflect.Modifier.isFinal;

import com.flipkart.krystal.vajram.VajramDef;
import java.util.List;
import org.reflections.Reflections;

public final class VajramLoader {

  public static List<? extends VajramDef<Object>> loadVajramsFromClassPath(String packagePrefix) {
    return new Reflections(packagePrefix)
        .getSubTypesOf(VajramDef.class).stream()
            .filter(c -> isFinal(c.getModifiers()))
            .map(VajramLoader::initVajram)
            .toList();
  }

  public static VajramDef<Object> loadVajramsFromClass(Class<? extends VajramDef> clazz) {
    if (!VajramDef.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException("Provided class is not a Vajram");
    }
    List<Class<? extends VajramDef>> impls =
        new Reflections(clazz.getPackageName())
            .getSubTypesOf(clazz).stream()
                .filter(subclass -> isFinal(subclass.getModifiers()))
                .<Class<? extends VajramDef>>map(subType -> subType.asSubclass(VajramDef.class))
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
  private static VajramDef<Object> initVajram(Class<? extends VajramDef> aClass) {
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
