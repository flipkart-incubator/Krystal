package com.flipkart.krystal.krystalrelease;

import com.vdurmont.semver4j.Semver;

public enum ReleaseLevel {
  MAJOR(SemVerPart.MAJOR),
  MINOR(SemVerPart.MINOR),
  PATCH(SemVerPart.PATCH);

  private final SemVerPart semVerPart;

  ReleaseLevel(SemVerPart semVerPart) {
    this.semVerPart = semVerPart;
  }

  public Semver toNextVersion(Semver semVer) {
    return semVerPart.toNextVersion(semVer);
  }
}
