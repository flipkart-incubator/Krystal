package com.flipkart.krystal.krystex.caching;

import java.util.Set;

record KryonCachingMetadata(
    boolean isComputeVajram,
    boolean isEligibleForCaching,
    Set<String> entitiesQueried,
    Set<String> entitiesMutated) {}
