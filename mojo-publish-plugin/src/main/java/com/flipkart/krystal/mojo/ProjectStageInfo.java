package com.flipkart.krystal.mojo;

import java.time.Instant;
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
}
