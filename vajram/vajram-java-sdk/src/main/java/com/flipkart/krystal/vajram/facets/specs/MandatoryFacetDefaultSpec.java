package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.facets.FacetUtils.computePlatformDefaultValue;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.FacetUtils;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MandatoryFacetDefaultSpec<T, CV extends Request> extends DefaultFacetSpec<T, CV>
    implements MandatorySingleValueFacetSpec<T, CV> {

  private @MonotonicNonNull T platformDefaultValue;

  public MandatoryFacetDefaultSpec(
      int id,
      String name,
      VajramID ofVajramID,
      DataType<T> type,
      ImmutableSet<FacetType> facetTypes,
      Class<CV> ofVajram,
      String documentation,
      boolean isBatched,
      Callable<ElementTags> tagsParser,
      Function<FacetValues, @Nullable T> getFromFacets,
      BiConsumer<FacetValues, @Nullable T> setToFacets) {
    super(
        id,
        name,
        ofVajramID,
        type,
        facetTypes,
        ofVajram,
        documentation,
        isBatched,
        tagsParser,
        getFromFacets,
        setToFacets);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NonNull T getPlatformDefaultValue() throws UnsupportedOperationException {
    if (platformDefaultValue == null) {
      platformDefaultValue = computePlatformDefaultValue(this, type());
    }
    return platformDefaultValue;
  }
}
