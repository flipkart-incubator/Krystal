package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.Vajrams.getVajramDefClass;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseInputResolvers;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicSources;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicTags;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors(fluent = true)
@Getter
public final class VajramDefinition {

  private final Vajram<Object> vajram;

  @Getter private final ImmutableMap<Integer, InputResolverDefinition> inputResolverDefinitions;

  private final ElementTags outputLogicTags;
  private final ElementTags vajramTags;

  private final VajramMetadata vajramMetadata;

  private final Class<? extends Vajram<?>> vajramDefClass;

  private final VajramID vajramId;
  private final ImmutableSet<Integer> outputLogicSources;

  private final ImmutableMap<String, VajramFacetDefinition> facetsByName;

  private final ImmutableMap<Integer, VajramFacetDefinition> facetsById;

  public VajramDefinition(Vajram<Object> vajram) {
    this.vajram = vajram;
    this.facetsByName =
        vajram.getFacetDefinitions().stream()
            .collect(toImmutableMap(VajramFacetDefinition::name, Function.identity()));
    this.facetsById =
        vajram.getFacetDefinitions().stream()
            .collect(toImmutableMap(VajramFacetDefinition::id, Function.identity()));
    this.vajramId = Vajrams.parseVajramId(vajram);
    this.vajramDefClass = getVajramDefClass(vajram.getClass());
    this.inputResolverDefinitions = parseInputResolvers(vajram);
    this.outputLogicTags = parseOutputLogicTags(vajram);
    this.vajramTags = parseVajramTags(vajramId, vajram);
    this.vajramMetadata = new VajramMetadata(vajram);
    this.outputLogicSources = parseOutputLogicSources(vajram, facetsByName);
  }
}
