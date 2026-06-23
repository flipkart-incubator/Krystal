package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.function.Function;

public sealed interface KryonDefinition permits AbstractKryonDefinition {

  ImmutableSet<Facet> facetsByType(FacetType facetType);

  VajramID vajramID();

  ImmutableSet<Facet> facets();

  LogicDefinition<CreateNewRequest> createNewRequest();

  KryonDefinitionRegistry kryonDefinitionRegistry();

  /** Includes tags on this vajram and those transitively induced from dependencies */
  ElementTags allTags();

  /**
   * A convenience method for clients to compute a cached metadata object which derives data from
   * this vajram definition.
   *
   * @param clazz The class representing the type of the metadata object
   * @param computer A function which computes the metadata from this vajram definition. This
   *     function is only called once for the lifecycle of this vajram definition and is then
   *     cached. This means that this function should compute values which are valid throughout the
   *     lifecycle of this vajram definition.
   * @param <T> The type of the metadata object
   */
  @SuppressWarnings("unchecked")
  <T> T getCustomMetadata(Class<T> clazz, Function<KryonDefinition, T> computer);
}
