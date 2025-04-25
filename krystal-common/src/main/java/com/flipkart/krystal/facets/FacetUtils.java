package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.IfAbsent;
import com.flipkart.krystal.data.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.tags.ElementTags;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.NonNull;

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

  public static <T> @NonNull T computePlatformDefaultValue(
      BasicFacetInfo facetInfo, DataType<T> type) {
    Optional<IfAbsent> ifNull = facetInfo.tags().getAnnotationByType(IfAbsent.class);
    if (ifNull.isPresent()) {
      IfAbsentThen ifAbsentThen = ifNull.get().value();
      if (!ifAbsentThen.usePlatformDefault()) {
        throw new UnsupportedOperationException(
            "The @IfNull(...) facet '"
                + facetInfo.name()
                + "' is configured with strategy: "
                + ifAbsentThen
                + " which returns 'false' for usePlatformDefault(). Hence, platform default value is "
                + "not supported. This method should not have been called. This seems to be krystal platform bug.");
      } else {
        try {
          return type.getPlatformDefaultValue();
        } catch (Throwable e) {
          throw new UnsupportedOperationException(e);
        }
      }
    } else {
      throw new AssertionError(
          "@IfNull annotation missing on facet "
              + facetInfo.name()
              + ". This should not be possible as this method should not have been. Something is wrong in platform code!");
    }
  }
}
