package com.flipkart.krystal.mojo;

import com.vdurmont.semver4j.Semver;

public enum PublishLevel {
  MAJOR(SemVerPart.MAJOR),
  MINOR(SemVerPart.MINOR),
  PATCH(SemVerPart.PATCH);

  private final SemVerPart semVerPart;

  PublishLevel(SemVerPart semVerPart) {
    this.semVerPart = semVerPart;
  }

  public Semver toNextVersion(Semver semVer) {
    return semVerPart.toNextVersion(semVer);
  }
}
