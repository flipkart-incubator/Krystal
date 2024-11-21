package com.flipkart.krystal.except;

/**
 * A callable which can throw any throwable
 *
 * @param <V>
 */
@FunctionalInterface
public interface ThrowingCallable<V> {

  V call() throws Throwable;
}
