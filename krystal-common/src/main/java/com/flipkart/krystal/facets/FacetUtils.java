package com.flipkart.krystal.facets;

import com.flipkart.krystal.tags.ElementTags;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Callable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FacetUtils {

  public static boolean isGiven(Facet facet) {
    return facet.facetTypes().contains(FacetType.INPUT)
        || facet.facetTypes().contains(FacetType.INJECTION);
  }

    public static Callable<ElementTags> fieldTagsParser(Callable<Field> facetFieldSupplier) {
    return () -> ElementTags.of(Arrays.stream(facetFieldSupplier.call().getAnnotations()).toList());
  }
}
