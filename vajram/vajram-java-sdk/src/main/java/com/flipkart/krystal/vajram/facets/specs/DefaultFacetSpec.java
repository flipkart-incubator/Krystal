package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract sealed class DefaultFacetSpec<T, CV extends Request>
    extends AbstractFacetSpec<T, CV> implements FacetSpec<T, CV>
    permits MandatoryFacetDefaultSpec, OptionalFacetDefaultSpec {

  private final Function<FacetValues, @Nullable T> getFromFacets;
  private final BiConsumer<FacetValues, @Nullable T> setToFacets;

  public DefaultFacetSpec(
      int id,
      String name,
      VajramID ofVajramID,
      DataType<T> type,
      ImmutableSet<FacetType> facetTypes,
      Class<CV> ofVajram,
      String documentation,
      boolean isBatched,
      ElementTags tags,
      Function<FacetValues, @Nullable T> getFromFacets,
      BiConsumer<FacetValues, @Nullable T> setToFacets) {
    super(id, name, ofVajramID, type, facetTypes, ofVajram, documentation, isBatched, tags);
    this.getFromFacets = getFromFacets;
    this.setToFacets = setToFacets;
  }

  @Override
  public Errable<@NonNull T> getFacetValue(FacetValues facetValues) {
    return errableFrom(() -> getValue(facetValues));
  }

  public @Nullable T getValue(FacetValues facetValues) {
    return getFromFacets.apply(facetValues);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void setFacetValue(FacetValuesBuilder facets, FacetValue value) {
    if (value instanceof Errable<?> errable) {
      Optional<?> o = errable.valueOpt();
      if (o.isPresent()) {
        setValue(facets, (T) o.get());
      }
    } else {
      throw new RuntimeException(
          "Expecting facet value type 'Errable' for default facet spec, but found: "
              + value.getClass());
    }
  }

  public void setValue(FacetValuesBuilder facets, T value) {
    setToFacets.accept(facets, value);
  }
}
