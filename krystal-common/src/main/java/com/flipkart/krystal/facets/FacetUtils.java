package com.flipkart.krystal.facets;

import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.facets.FacetType.INPUT;

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
    FacetType facetType = facet.facetType();
    return INPUT.equals(facetType) || INJECTION.equals(facetType);
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
