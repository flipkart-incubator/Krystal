package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.AbstractFacet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.tags.ElementTags;
import java.util.concurrent.Callable;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Represents a facet of the current vajram. This may represent an input of this vajram or a
 * depenedency of this vajram (See: {@link DependencySpec})
 *
 * @param <T> The data type of the facet.
 * @param <CV> The current vajram which has the facet
 */
@Getter
public abstract sealed class AbstractFacetSpec<T, CV extends Request> extends AbstractFacet
    implements FacetSpec<T, CV> permits DefaultFacetSpec, DependencySpec {

  private final DataType<T> type;
  private final boolean isBatched;
  private final Class<CV> ofVajram;
  private @MonotonicNonNull ElementTags tags;
  private final Callable<ElementTags> tagsParser;

  public AbstractFacetSpec(
      int id,
      String name,
      VajramID ofVajramID,
      DataType<T> type,
      FacetType facetType,
      Class<CV> ofVajram,
      String documentation,
      boolean isBatched,
      Callable<ElementTags> tagsParser) {
    super(id, name, ofVajramID, facetType, documentation);
    this.type = type;
    this.ofVajram = ofVajram;
    this.tagsParser = tagsParser;
    this.isBatched = isBatched;
  }

  @Override
  public ElementTags tags() {
    if (tags == null) {
      try {
        tags = tagsParser.call();
      } catch (Exception e) {
        tags = emptyTags();
      }
    }
    return tags;
  }
}
