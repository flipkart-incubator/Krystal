package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Mandatory.IfNotSet;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MandatoryFacetDefaultSpec<T, CV extends Request> extends DefaultFacetSpec<T, CV>
    implements MandatoryFacetSpec<T, CV> {

  private @MonotonicNonNull T platformDefaultValue;

  public MandatoryFacetDefaultSpec(
      int id,
      String name,
      VajramID vajramID,
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
        vajramID,
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
  @SuppressWarnings("unchecked")
  public @NonNull T getPlatformDefaultValue() throws UnsupportedOperationException {
    if (platformDefaultValue == null) {
      Optional<Mandatory> mandatoryOpt = tags().getAnnotationByType(Mandatory.class);
      if (mandatoryOpt.isPresent()) {
        IfNotSet ifNotSet = mandatoryOpt.get().ifNotSet();
        if (!ifNotSet.usePlatformDefault()) {
          throw new UnsupportedOperationException(
              "The @Mandatory facet '"
                  + name()
                  + "' is configured with ifNotSet strategy: "
                  + ifNotSet
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
