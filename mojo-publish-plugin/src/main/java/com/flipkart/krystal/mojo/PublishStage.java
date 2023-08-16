package com.flipkart.krystal.mojo;

import com.vdurmont.semver4j.Semver;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum PublishStage {
  DEV,
  PRODUCTION;

  public static final String SNAPSHOT = "SNAPSHOT";

  public Semver decorateVersion(Semver nextVersion, @Nullable String featureName) {
    return (switch (this) {
      case DEV -> Optional.ofNullable(featureName)
          .map(feature -> nextVersion.withSuffix(feature + '-' + SNAPSHOT))
          .orElse(nextVersion.withSuffix(SNAPSHOT));
      case PRODUCTION -> nextVersion;
    });
  }
}
