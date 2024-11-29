package com.flipkart.krystal.vajram;

import static java.lang.reflect.Modifier.isFinal;

import java.util.List;
import org.reflections.Reflections;

public final class VajramLoader {

  public static List<? extends Vajram<?>> loadVajramsFromClassPath(String packagePrefix) {
    return new Reflections(packagePrefix)
        .getSubTypesOf(Vajram.class).stream()
            .filter(aClass -> isFinal(aClass.getModifiers()))
            .map(Vajrams::newInstance)
            .toList();
  }

  private VajramLoader() {}
}
