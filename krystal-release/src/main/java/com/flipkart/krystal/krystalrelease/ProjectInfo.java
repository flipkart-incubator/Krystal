package com.flipkart.krystal.krystalrelease;

import lombok.Data;

@Data
public final class ProjectInfo {
  private String name;
  private String version = "0.0.0";
  private ReleaseStage releaseStage;
  private String commitId;
}
