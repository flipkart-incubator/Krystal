package com.flipkart.krystal.vajram.utils;

import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The rules which the facets of a vajram which supports batching must follow (see {@link Batched}
 * and {@link BatchesGroupedBy}). This is shared by the annotation processor (compile time) and by
 * the vajram graph loader (runtime) so that both validate in exactly the same way.
 */
public final class BatchingRules {

  /** The properties of a facet which are relevant for the batching rules. */
  public record FacetInfo(String name, FacetType type, boolean batched, boolean groupsBatches) {
    boolean given() {
      return type.isGiven();
    }
  }

  /** A broken rule. {@code facet} is the facet which broke it. */
  public record Violation(String facet, String message) {}

  /**
   * Validates the batching rules.
   *
   * @param facets all the facets of a vajram
   * @param dependencies the dependencies between the facets of the vajram
   * @return all the violations. Empty if the rules are followed.
   */
  public static List<Violation> validate(
      Collection<FacetInfo> facets, FacetDependencyGraph dependencies) {
    List<Violation> violations = new ArrayList<>();
    for (FacetInfo facet : facets) {
      if (facet.groupsBatches() && !facet.given()) {
        violations.add(
            new Violation(
                facet.name(),
                "@BatchesGroupedBy can be placed only on input and injection facets. '%s' is a %s facet."
                    .formatted(facet.name(), facet.type())));
      }
      if (facet.groupsBatches() && facet.batched()) {
        violations.add(
            new Violation(
                facet.name(),
                "Facet '%s' cannot be annotated with both @Batched and @BatchesGroupedBy"
                    .formatted(facet.name())));
      }
    }

    Set<String> batchedFacets =
        facets.stream().filter(FacetInfo::batched).map(FacetInfo::name).collect(Collectors.toSet());
    if (batchedFacets.isEmpty()) {
      return violations; // The vajram does not support batching
    }
    for (FacetInfo facet : facets) {
      if (facet.given() && !facet.batched() && !facet.groupsBatches()) {
        violations.add(
            new Violation(
                facet.name(),
                "Facet '%s' of a vajram which supports batching must be annotated with either @Batched or @BatchesGroupedBy"
                    .formatted(facet.name())));
      }
    }
    dependencies
        .findDependencyOn(batchedFacets, batchedFacets)
        .ifPresent(
            dependency ->
                violations.add(
                    new Violation(
                        dependency.facet(),
                        "Facet '%s' cannot depend on @Batched facet '%s' (directly or transitively). Only @Batched facets can depend on @Batched facets."
                            .formatted(dependency.facet(), dependency.dependsOn()))));
    return violations;
  }

  private BatchingRules() {}
}
