package com.flipkart.krystal.vajram.exec;

import static java.lang.reflect.Modifier.isAbstract;

import com.flipkart.krystal.vajram.Vajram;
import java.util.List;
import org.reflections.Reflections;

final class VajramLoader {

  static List<? extends Vajram<?>> loadVajramsFromClassPath(String packagePrefix) {
    //noinspection unchecked
    return (List<? extends Vajram<?>>)
        new Reflections(packagePrefix)
            .getSubTypesOf(Vajram.class).stream()
                .filter(aClass -> !isAbstract(aClass.getModifiers()))
                .map(
                    aClass -> {
                      try {
                        return aClass.getConstructor().newInstance();
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    })
                .toList();
  }
}
