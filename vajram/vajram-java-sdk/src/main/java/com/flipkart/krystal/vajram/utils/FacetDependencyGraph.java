package com.flipkart.krystal.vajram.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Graph of the facets of a vajram where an edge {@code a -> b} means "facet a depends on facet b".
 * Used both by the annotation processor (compile time) and by the vajram graph loader (runtime) so
 * that both validate facet dependencies in exactly the same way.
 */
public final class FacetDependencyGraph {

  /** A facet which (directly or transitively) depends on {@code dependsOn}. */
  public record Dependency(String facet, String dependsOn) {}

  private final Map<String, Set<String>> dependencies = new LinkedHashMap<>();

  public void addFacet(String facet) {
    dependencies.computeIfAbsent(facet, _k -> new LinkedHashSet<>());
  }

  public void addDependencies(String facet, Collection<String> dependsOn) {
    dependencies.computeIfAbsent(facet, _k -> new LinkedHashSet<>()).addAll(dependsOn);
  }

  /**
   * Returns the path of a dependency cycle if there is one. The path contains only the facets of
   * the cycle, and its first and last elements are the same facet.
   */
  public Optional<List<String>> findCycle() {
    Set<String> visited = new LinkedHashSet<>();
    for (String facet : dependencies.keySet()) {
      List<String> path = new ArrayList<>();
      if (!visited.contains(facet) && findCycle(facet, visited, path)) {
        return Optional.of(path);
      }
    }
    return Optional.empty();
  }

  private boolean findCycle(String facet, Set<String> visited, List<String> path) {
    visited.add(facet);
    path.add(facet);
    for (String dependency : dependencies.getOrDefault(facet, Set.of())) {
      if (path.contains(dependency)) {
        path.add(dependency);
        path.subList(0, path.indexOf(dependency)).clear(); // Drop the acyclic prefix
        return true;
      }
      if (!visited.contains(dependency) && findCycle(dependency, visited, path)) {
        return true;
      }
    }
    path.remove(path.size() - 1);
    return false;
  }

  /**
   * Finds a facet, which is not one of the {@code allowedDependents}, that directly or transitively
   * depends on one of the {@code targets}.
   */
  public Optional<Dependency> findDependencyOn(Set<String> targets, Set<String> allowedDependents) {
    for (String facet : dependencies.keySet()) {
      if (allowedDependents.contains(facet)) {
        continue;
      }
      Set<String> transitive = new LinkedHashSet<>();
      collectTransitiveDependencies(facet, transitive);
      for (String dependency : transitive) {
        if (targets.contains(dependency)) {
          return Optional.of(new Dependency(facet, dependency));
        }
      }
    }
    return Optional.empty();
  }

  private void collectTransitiveDependencies(String facet, Set<String> result) {
    for (String dependency : dependencies.getOrDefault(facet, Set.of())) {
      if (result.add(dependency)) {
        collectTransitiveDependencies(dependency, result);
      }
    }
  }
}
