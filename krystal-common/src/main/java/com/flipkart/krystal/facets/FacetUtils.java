package com.flipkart.krystal.facets;

import com.flipkart.krystal.tags.ElementTags;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FacetUtils {
  public static boolean isGiven(Facet facet) {
    return facet.facetTypes().contains(FacetType.INPUT)
        || facet.facetTypes().contains(FacetType.INJECTION);
  }

  @SafeVarargs
  public static Callable<ElementTags> fieldTagsParser(Callable<Field>... facetFieldSuppliers) {
    return () -> {
      List<Annotation> annotationList = new ArrayList<>();
      for (Callable<Field> facetFieldSupplier : facetFieldSuppliers) {
        annotationList.addAll(Arrays.asList(facetFieldSupplier.call().getAnnotations()));
      }
      return ElementTags.of(annotationList);
    };
  }
}
