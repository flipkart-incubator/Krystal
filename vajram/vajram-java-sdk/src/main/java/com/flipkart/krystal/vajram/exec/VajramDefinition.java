package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.Vajrams.getVajramDefClass;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseComputeFieldEdges;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseInputResolvers;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicSources;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseOutputLogicTags;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramId;
import static com.flipkart.krystal.vajram.exec.Vajrams.parseVajramTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.utils.BatchingRules;
import com.flipkart.krystal.vajram.utils.FacetDependencyGraph;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

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

  private final ConcurrentMap<Class<?>, Object> customMetadata = new ConcurrentHashMap<>();

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

    this.outputLogicTags = parseOutputLogicTags(vajramDefRoot);
    this.vajramTags = parseVajramTags(vajramDefRoot);
    this.outputLogicSources = parseOutputLogicSources(vajramDefRoot, facetSpecs, facetsByName);
    this.inputResolvers = parseInputResolvers(vajramDefRoot);

    this.metadata = new VajramMetadata(facetSpecs, vajramTags);

    // Validate facet dependencies
    validate();
  }

  public boolean isTrait() {
    return vajramTags().getAnnotationByType(Trait.class).isPresent();
  }

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
  public <T> T getCustomMetadata(Class<T> clazz, Function<VajramDefinition, @NonNull T> computer) {
    return (T) customMetadata.computeIfAbsent(clazz, _c -> computer.apply(this));
  }

  /**
   * Validates facet dependencies for cycles and the {@link BatchingRules}. This covers dependencies
   * which the annotation processor cannot see, like the ones defined via {@code
   * getSimpleInputResolvers()}.
   */
  private void validate() {
    if (facetSpecs.isEmpty()) {
      return;
    }
    FacetDependencyGraph graph = buildFacetDependencyGraph();
    graph
        .findCycle()
        .ifPresent(
            cycle -> {
              throw new VajramDefinitionException(
                  "Cycle detected in facet dependencies for vajram '%s': %s"
                      .formatted(vajramId, String.join(" → ", cycle)));
            });
    validateBatchingRules(graph);
  }

  private FacetDependencyGraph buildFacetDependencyGraph() {
    FacetDependencyGraph graph = new FacetDependencyGraph();
    facetSpecs.forEach(facet -> graph.addFacet(facet.name()));
    inputResolvers
        .keySet()
        .forEach(
            resolver ->
                graph.addDependencies(
                    resolver.target().dependency().name(),
                    resolver.sources().stream().map(Facet::name).toList()));
    parseComputeFieldEdges(def).forEach(graph::addDependencies);
    return graph;
  }

  private void validateBatchingRules(FacetDependencyGraph graph) {
    List<BatchingRules.Violation> violations =
        BatchingRules.validate(
            facetSpecs.stream()
                .map(
                    facet ->
                        new BatchingRules.FacetInfo(
                            facet.name(),
                            facet.facetType(),
                            facet.isBatched(),
                            facet.tags().getAnnotationByType(BatchesGroupedBy.class).isPresent()))
                .toList(),
            graph);
    if (!violations.isEmpty()) {
      throw new VajramDefinitionException(
          "Invalid batching facets in vajram '%s': %s"
              .formatted(
                  vajramId,
                  violations.stream()
                      .map(BatchingRules.Violation::message)
                      .collect(joining("; "))));
    }
  }
}
