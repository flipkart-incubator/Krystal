package com.flipkart.krystal.krystex.epochs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of one or more logics which have sources - and a source ordinal can be computed considering
 * these sources
 */
sealed interface LogicSet {
  Set<? extends Facet> sources(VajramGraph graph);

  /**
   * Represents all the resolvers which resolve the given {@code dependency}
   *
   * @param dependency the dependency whose resolvers together form a logic set
   */
  record DepResolvers(Dependency dependency) implements LogicSet {

    @Override
    public Set<Facet> sources(VajramGraph graph) {
      VajramDefinition vajramDef = graph.tryGetVajramDefinition(dependency.ofVajramID());
      if (vajramDef == null) {
        return Set.of();
      }
      List<ResolverDefinition> resolvers =
          vajramDef.inputResolvers().keySet().stream()
              .filter(r -> r.target().dependency().equals(dependency))
              .toList();
      Set<Facet> set = new LinkedHashSet<>();
      for (ResolverDefinition resolver : resolvers) {
        set.addAll(resolver.sources());
      }
      return set;
    }
  }

  /**
   * Represents the output logic(s) (multiple in case there is @Output.Batched and @Output.Unbatch)
   * of a vajram
   *
   * @param vajramID whose output logic(s) are considered a logic set
   */
  record OutputLogics(VajramID vajramID) implements LogicSet {

    @Override
    public Set<FacetSpec> sources(VajramGraph graph) {
      VajramDefinition vajramDefinition = graph.tryGetVajramDefinition(vajramID());
      if (vajramDefinition == null) {
        return Set.of();
      }
      return vajramDefinition.outputLogicSources();
    }
  }
}
