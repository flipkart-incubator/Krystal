package com.flipkart.krystal.krystalrelease;

import com.vdurmont.semver4j.Semver;
import java.util.Optional;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum ReleaseStage {
  DEV,
  PRODUCTION;

  public static final String SNAPSHOT = "SNAPSHOT";
  private static final Pattern NON_ALPHANUMERIC_CHARS = Pattern.compile("[^a-zA-Z0-9]");

  public Semver decorateVersion(Semver nextVersion, @Nullable String featureName) {
    return (switch (this) {
      case DEV -> Optional.ofNullable(featureName)
          .map(
              feature ->
                  nextVersion.withSuffix(
                      NON_ALPHANUMERIC_CHARS.matcher(feature).replaceAll(".") + '.' + SNAPSHOT))
          .orElse(nextVersion.withSuffix(SNAPSHOT));
      case PRODUCTION -> nextVersion;
    });
  }
}
