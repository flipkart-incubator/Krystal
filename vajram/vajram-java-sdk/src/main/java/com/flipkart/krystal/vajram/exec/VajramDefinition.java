package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.Vajrams.getVajramDefClass;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseInputResolvers;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicSources;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicTags;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramId;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class VajramDefinition {

  private final VajramDefRoot<Object> vajramDef;

  private final ImmutableMap<ResolverDefinition, InputResolver> inputResolvers;

  private final ElementTags outputLogicTags;
  private final ElementTags vajramTags;

  private final VajramMetadata vajramMetadata;

  private final Class<? extends VajramDefRoot<?>> vajramDefClass;

  private final VajramID vajramId;
  private final ImmutableSet<FacetSpec> outputLogicSources;

  private final ImmutableSet<FacetSpec> facetSpecs;
  private final ImmutableMap<String, FacetSpec> facetsByName;
  private final ImmutableMap<Integer, FacetSpec> facetsById;

  public VajramDefinition(VajramDefRoot<Object> vajramDef) {
    this.vajramDef = vajramDef;
    this.facetSpecs =
        vajramDef.facetsFromRequest(vajramDef.newRequestBuilder())._facets().stream()
            .map(facetDefinition -> (FacetSpec) facetDefinition)
            .collect(toImmutableSet());
    this.facetsByName =
        facetSpecs.stream().collect(toImmutableMap(Facet::name, Function.identity()));
    this.facetsById = facetSpecs.stream().collect(toImmutableMap(Facet::id, Function.identity()));
    this.vajramId = parseVajramId(vajramDef);
    this.vajramDefClass = getVajramDefClass(vajramDef.getClass());
    this.inputResolvers = parseInputResolvers(vajramDef);
    this.outputLogicTags = parseOutputLogicTags(vajramDef);
    this.vajramTags = parseVajramTags(vajramId, vajramDef);
    this.outputLogicSources =
        parseOutputLogicSources(vajramDef, facetSpecs, facetsByName, facetsById);
    this.vajramMetadata = new VajramMetadata(vajramDef, facetSpecs);
  }
}
