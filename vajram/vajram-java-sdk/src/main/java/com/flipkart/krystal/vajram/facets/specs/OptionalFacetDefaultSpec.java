package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class OptionalFacetDefaultSpec<T, CV extends Request> extends DefaultFacetSpec<T, CV>
    implements OptionalFacetSpec<T, CV> {

  public OptionalFacetDefaultSpec(
      int id,
      String name,
      DataType<T> type,
      ImmutableSet<FacetType> facetTypes,
      Class<CV> ofVajram,
      String documentation,
      boolean isBatched,
      ElementTags tags,
      Function<FacetValues, @Nullable T> getFromFacets,
      BiConsumer<FacetValues, @Nullable T> setToFacets) {
    super(
        id,
        name,
        type,
        facetTypes,
        ofVajram,
        documentation,
        isBatched,
        tags,
        getFromFacets,
        setToFacets);
  }

  @Override
  public @NonNull Void getPlatformDefaultValue() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Optional facets do not have a platform default value");
  }
}
