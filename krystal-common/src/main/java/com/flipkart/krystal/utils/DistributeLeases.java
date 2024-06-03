package com.flipkart.krystal.utils;

/**
 * @param maxActiveObjects Prefer to distribute leases equally among this many active objects if
 *     each object already has {@code distributionTriggerThreshold} active leases.
 * @param distributionTriggerThreshold distribute leases equally/create new objects only after all
 *     objects have reached this threshold
 */
public record DistributeLeases(int maxActiveObjects, int distributionTriggerThreshold)
    implements MultiLeasePolicy {}
