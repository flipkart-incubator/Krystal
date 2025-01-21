package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.tags.ElementTags;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Callable;

public sealed interface FacetSpec<T, CV extends Request> extends Facet
    permits AbstractFacetSpec, DefaultFacetSpec, MandatoryFacetSpec, OptionalFacetSpec {

  boolean isMandatory();

  boolean isBatched();

  boolean canFanout();

  DataType<T> type();

  Class<CV> ofVajram();

  ElementTags tags();

  static ElementTags parseFacetTags(Field facetField) {
    return ElementTags.of(Arrays.stream(facetField.getAnnotations()).toList());
  }

  static ElementTags parseFacetTags(Callable<Field> facetFieldSupplier) {
    Field facetField;
    try {
      facetField = facetFieldSupplier.call();
    } catch (Exception e) {
      return emptyTags();
    }
    return ElementTags.of(Arrays.stream(facetField.getAnnotations()).toList());
  }
}
