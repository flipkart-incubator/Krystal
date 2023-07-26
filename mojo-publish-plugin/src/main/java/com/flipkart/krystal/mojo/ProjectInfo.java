package com.flipkart.krystal.mojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public final class ProjectInfo {
  private String name;
  private String version = "0.0.0";
  private PublishStage publishStage = PublishStage.DEV;
  /** This is the base commit id over which the next DEV snapshot needs to be published. */
  private String devBaseCommitId;
  /** This is the base commit id over which the next PRODUCTION release needs to be published. */
  private String productionBaseCommitId;

  @JsonIgnore
  public String getBaseCommitId(PublishStage publishStage) {
    return switch (publishStage) {
      case DEV -> devBaseCommitId;
      case PRODUCTION -> productionBaseCommitId;
    };
  }
}
