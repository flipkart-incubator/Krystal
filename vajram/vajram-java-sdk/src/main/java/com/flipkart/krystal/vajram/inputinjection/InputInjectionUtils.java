package com.flipkart.krystal.vajram.inputinjection;

import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InputInjectionUtils {
  public static <T> Annotation[] getQualifiers(FacetSpec<T, ?> facetDef) {
    Annotation[] qualifierAnnotations =
        facetDef.tags().annotations().stream()
            .<Annotation>mapMulti(
                (tag, consumer) -> {
                  boolean isQualifierAnno =
                      tag.annotationType().getAnnotation(Qualifier.class) != null;
                  if (isQualifierAnno) {
                    consumer.accept(tag);
                  }
                })
            .toArray(Annotation[]::new);
    return qualifierAnnotations;
  }
}
