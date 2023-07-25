package com.flipkart.krystal.krystalrelease;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.options.Option;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
@Getter
@Setter
public class ReleaseProjectTask extends DefaultTask {
  @Option(description = "Is this a MAJOR/MINOR/PATCH release?")
  private ReleaseLevel releaseLevel = ReleaseLevel.MINOR;

  @Option(
      description =
          "What is the current feature you are building? Example, git branch, jiraId, commit id etc.")
  @Optional
  private String featureName;

  @Input
  public String getFeatureName() {
    return featureName;
  }

  @Input
  public ReleaseLevel getReleaseLevel() {
    return releaseLevel;
  }
}
