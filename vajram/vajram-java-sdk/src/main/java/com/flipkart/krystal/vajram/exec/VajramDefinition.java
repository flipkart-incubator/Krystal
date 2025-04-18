package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.Vajrams.getVajramDefClass;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseInputResolvers;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicSources;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicTags;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramId;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.VajramDef;
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

  private final VajramDefRoot<Object> def;
  private final Class<? extends VajramDefRoot<?>> defType;
  private final Class<? extends Request<?>> reqRootType;

  private final ImmutableMap<ResolverDefinition, InputResolver> inputResolvers;

  private final ElementTags outputLogicTags;
  private final ElementTags vajramTags;

  private final VajramMetadata metadata;

  private final VajramID vajramId;
  private final ImmutableSet<FacetSpec> outputLogicSources;

  private final ImmutableSet<FacetSpec> facetSpecs;
  private final ImmutableSet<InputMirror> inputMirrors;
  private final ImmutableMap<String, FacetSpec> facetsByName;
  private final ImmutableMap<Integer, FacetSpec> facetsById;

  public VajramDefinition(VajramDefRoot<Object> vajramDefRoot) {
    this.vajramId = parseVajramId(vajramDefRoot);

    this.def = vajramDefRoot;
    this.defType = getVajramDefClass(vajramDefRoot.getClass());
    this.reqRootType = vajramDefRoot.requestRootType();

    this.facetSpecs =
        vajramDefRoot instanceof VajramDef<?> v
            ? v.facetsFromRequest(vajramDefRoot.newRequestBuilder())._facets().stream()
                .map(facetDefinition -> (FacetSpec) facetDefinition)
                .collect(toImmutableSet())
            : ImmutableSet.of();
    this.inputMirrors = ImmutableSet.copyOf(vajramDefRoot.newRequestBuilder()._facets());
    this.facetsByName =
        facetSpecs.stream().collect(toImmutableMap(Facet::name, Function.identity()));
    this.facetsById = facetSpecs.stream().collect(toImmutableMap(Facet::id, Function.identity()));

    this.outputLogicTags = parseOutputLogicTags(vajramDefRoot);
    this.vajramTags = parseVajramTags(vajramId, vajramDefRoot);
    this.outputLogicSources = parseOutputLogicSources(vajramDefRoot, facetSpecs, facetsByName);
    this.inputResolvers = parseInputResolvers(vajramDefRoot);

    this.metadata = new VajramMetadata(facetSpecs);
  }

  public boolean isTrait() {
    return def instanceof TraitDef<?>;
  }
}
