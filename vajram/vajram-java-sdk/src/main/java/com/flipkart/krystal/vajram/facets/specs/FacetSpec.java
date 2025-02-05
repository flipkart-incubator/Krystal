package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.facets.Mandatory;
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

  /**
   * Returns the default value for the facet based on its configuration. This is useful in cases
   * where a facet is tagged {@link Mandatory} and its {@link
   * Mandatory.IfNotSet#usePlatformDefault()} returns true. The platform must assign a default value
   * to the facet instead of failing. Which default value to use is returned by this method.
   *
   * @throws UnsupportedOperationException if this facet's configuration does not allow using a
   *     platform default value
   * @see DataType#getPlatformDefaultValue()
   */
  <D> D getPlatformDefaultValue() throws UnsupportedOperationException;

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
