package com.flipkart.krystal.pooling;

import static java.lang.Math.max;

import lombok.Builder;

@Builder(toBuilder = true)
public record MultiLeasePoolStatsImpl(
    int currentPoolSize,
    int peakPoolSize,
    int peakLeasesOfAnObject,
    double peakAvgActiveLeasesPerObject,
    int currentActiveLeases)
    implements MultiLeasePoolStats {

  public static class MultiLeasePoolStatsImplBuilder {

    public void reportNewLease(int numberOfLeasesOfObject) {
      currentActiveLeases++;
      peakLeasesOfAnObject = max(peakLeasesOfAnObject, numberOfLeasesOfObject);
      peakAvgActiveLeasesPerObject =
          max(peakAvgActiveLeasesPerObject, currentActiveLeases * 1.0 / currentPoolSize);
    }

    public void reportNewObject() {
      currentPoolSize++;
      peakPoolSize = max(peakPoolSize, currentPoolSize);
    }

    public void reportLeaseClosed() {
      currentActiveLeases--;
    }

    public void reportObjectDeleted() {
      currentPoolSize--;
    }

    public double getPeakAvgActiveLeasesPerObject() {
      return peakAvgActiveLeasesPerObject;
    }
  }
}
