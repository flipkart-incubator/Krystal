package com.flipkart.krystal.krystalrelease;


import static java.util.function.UnaryOperator.identity;

import com.vdurmont.semver4j.Semver;
import java.util.function.UnaryOperator;

public enum SemVerPart {
  MAJOR(Semver::nextMajor),
  MINOR(Semver::nextMinor),
  PATCH(Semver::nextPatch),
  PRE_RELEASE(identity()),
  BUILD(identity());

  private final UnaryOperator<Semver> versionIncrementer;

  SemVerPart(UnaryOperator<Semver> versionIncrementer) {
    this.versionIncrementer = versionIncrementer;
  }

  public Semver toNextVersion(Semver semVer) {
    return versionIncrementer.apply(semVer);
  }
}
