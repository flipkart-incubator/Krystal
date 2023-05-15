package com.flipkart.krystal.krystex.node;

import java.util.Objects;
import java.util.Optional;

// TODO : Need to make Node ID mandatory, so that record equals and hashcode
//  can be used as it will be more performant
public record DefaultDependantChain(
    Optional<NodeId> nodeId, String dependencyName, DependantChain dependantChain)
    implements DependantChain {

  public DefaultDependantChain(String dependencyName, DependantChain dependantChain) {
    this(Optional.empty(), dependencyName, dependantChain);
  }

  @Override
  public String toString() {
    return "%s.%s".formatted(dependantChain().toString(), dependencyName());
  }

  @Override
  public boolean contains(NodeId nodeId) {
    return this.nodeId.map(n -> n.equals(nodeId)).orElse(false) || dependantChain.contains(nodeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.dependantChain, this.dependencyName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DefaultDependantChain other) {
      return other.dependantChain.equals(this.dependantChain)
          && other.dependencyName.equals(this.dependencyName);
    }
    return false;
  }
}
