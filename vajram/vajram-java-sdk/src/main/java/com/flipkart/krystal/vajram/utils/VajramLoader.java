package com.flipkart.krystal.vajram.utils;

import static java.lang.reflect.Modifier.isFinal;

import com.flipkart.krystal.vajram.Vajram;
import java.util.List;
import org.reflections.Reflections;

public final class VajramLoader {

  public static List<? extends Vajram<Object>> loadVajramsFromClassPath(String packagePrefix) {
    //noinspection unchecked
    return (List<? extends Vajram<Object>>)
        new Reflections(packagePrefix)
            .getSubTypesOf(Vajram.class).stream()
                .filter(aClass -> isFinal(aClass.getModifiers()))
                .map(
                    aClass -> {
                      try {
                        return aClass.getConstructor().newInstance();
                      } catch (Throwable e) {
                        throw new RuntimeException(e);
                      }
                    })
                .toList();
  }

  private VajramLoader() {}
}
