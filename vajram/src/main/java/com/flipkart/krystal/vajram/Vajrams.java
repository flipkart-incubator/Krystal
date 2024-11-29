package com.flipkart.krystal.vajram;

import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.tags.Tag;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

@Slf4j
public final class Vajrams {

  private static final ConcurrentHashMap<Class<? extends Vajram<?>>, Vajram<?>> VAJRAM_CACHE =
      new ConcurrentHashMap<>();

  public static String getVajramIdString(Class<? extends Vajram<?>> aClass) {
    return getVajram(aClass).getId().vajramId();
  }

  public static Vajram<?> getVajram(Class<? extends Vajram<?>> aClass) {
    return VAJRAM_CACHE.computeIfAbsent(aClass, Vajrams::newInstance);
  }

  public static ElementTags parseFacetTags(Field facetField) {
    return ElementTags.of(
        Arrays.stream(facetField.getAnnotations())
            .map(Tag::from)
            .collect(Collectors.toMap(Tag::tagKey, Function.identity())));
  }

  public static Class<?> getVajramSourceClass(Class<?> aClass) {
    Annotation annotation = aClass.getAnnotation(VajramDef.class);
    if (annotation != null) {
      return aClass;
    }
    Class<?> superclass = aClass.getSuperclass();
    if (Object.class.equals(superclass) || superclass == null) {
      throw new IllegalArgumentException();
    }
    return getVajramSourceClass(superclass);
  }

  private Vajrams() {}

  static Vajram<?> newInstance(@SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    try {
      return aClass.getConstructor().newInstance();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
