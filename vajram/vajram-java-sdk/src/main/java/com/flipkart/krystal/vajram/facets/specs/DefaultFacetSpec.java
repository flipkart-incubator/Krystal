package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
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

  private final Function<Facets, @Nullable T> getFromFacets;
  private final BiConsumer<Facets, @Nullable T> setToFacets;

  public DefaultFacetSpec(
      int id,
      String name,
      DataType<T> type,
      ImmutableSet<FacetType> facetTypes,
      Class<CV> ofVajram,
      String documentation,
      boolean isBatched,
      ElementTags tags,
      Function<Facets, @Nullable T> getFromFacets,
      BiConsumer<Facets, @Nullable T> setToFacets) {
    super(id, name, type, facetTypes, ofVajram, documentation, isBatched, tags);
    this.getFromFacets = getFromFacets;
    this.setToFacets = setToFacets;
  }

  public Errable<@NonNull T> getFacetValue(Facets facets) {
    return errableFrom(() -> getValue(facets));
  }

  public @Nullable T getValue(Facets facets) {
    return getFromFacets.apply(facets);
  }

  @Override
  public final void setFacetValue(FacetsBuilder facets, FacetValue value) {
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

  public void setValue(FacetsBuilder facets, T value) {
    setToFacets.accept(facets, value);
  }
}
