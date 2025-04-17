package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.data.IfNull.IfNullThen;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
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
      Optional<IfNull> ifNull = tags().getAnnotationByType(IfNull.class);
      if (ifNull.isPresent()) {
        IfNullThen ifNullThen = ifNull.get().value();
        if (!ifNullThen.usePlatformDefault()) {
          throw new UnsupportedOperationException(
              "The @Mandatory facet '"
                  + name()
                  + "' is configured with ifNotSet strategy: "
                  + ifNullThen
                  + " which returns 'false' for usePlatformDefault(). Hence, platform default value is "
                  + "not supported. This method should not have been called. This seems to be krystal platform bug.");
        } else {
          try {
            platformDefaultValue = type().getPlatformDefaultValue();
          } catch (Throwable e) {
            throw new UnsupportedOperationException(e);
          }
        }
      } else {
        throw new AssertionError(
            "@Mandatory annotation missing on mandatory facet "
                + name()
                + ". This should not be possible. Something is wrong in platform code!");
      }
    }
    return platformDefaultValue;
  }
}
