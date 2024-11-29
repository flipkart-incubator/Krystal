package com.flipkart.krystal.vajram;

import static java.lang.reflect.Modifier.isFinal;

import java.util.List;
import java.util.function.Function;
import org.reflections.Reflections;

public final class VajramLoader {

  @SuppressWarnings("rawtypes")
  public static List<? extends Vajram<?>> loadVajramsFromClassPath(String packagePrefix) {
    return new Reflections(packagePrefix)
        .getSubTypesOf(Vajram.class).stream()
            .filter(aClass -> isFinal(aClass.getModifiers()))
            .map(
                (Function<Class<? extends Vajram>, ? extends Vajram<?>>)
                    vajramImplClass -> {
                      try {
                        return vajramImplClass.getConstructor().newInstance();
                      } catch (Throwable e) {
                        throw new RuntimeException(e);
                      }
                    })
            .toList();
  }

  private VajramLoader() {}
}
