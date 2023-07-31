package com.flipkart.krystal.mojo;

import java.time.Instant;
import java.util.Collection;
import java.util.TreeSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class ProjectStageInfo {
  private PublishStage stage;
  private String version;
  private String commitId;
  private Instant publishTime;

  /*TreeSet: so that the ordering is standard and predictable.*/
  private TreeSet<PublishTarget> pendingTargets;

  public void setPendingTargets(Collection<PublishTarget> pendingTargets) {
    this.pendingTargets = new TreeSet<>(pendingTargets);
  }
}
