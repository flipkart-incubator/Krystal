package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;

/**
 * Command used to notify that all dependent chains starting with {@link #ancestor} chain where the
 * ancestor directly depends on {@link #fromVajramId} must be flushed.
 *
 * <p>When multiple vajrams implement a trait, all of them would have the same incoming dependent
 * chains - {@link #fromVajramId} is used to select which descendents need to be selected from the
 * implementing vajrams.
 *
 * <p>Similarly, when multiple vajrams are invocable from outside the graph, {@link #fromVajramId}
 * is used to specify only those vajrams which were not invoked so that only descendents of that
 * subtree are flushed,
 */
public record FlushCommand(DependentChain ancestor, VajramID fromVajramId)
    implements DecoratorCommand {}
