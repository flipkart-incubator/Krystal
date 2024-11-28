package com.flipkart.krystal.pooling;

public interface MultiLeasePoolStats {

  int currentPoolSize();

  int peakPoolSize();

  int peakLeasesOfAnObject();

  double peakAvgActiveLeasesPerObject();
}
