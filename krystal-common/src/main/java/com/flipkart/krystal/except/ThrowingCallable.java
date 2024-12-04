package com.flipkart.krystal.except;

/**
 * A callable which can throw any throwable
 *
 * @param <V> The type of the value returned by invoking the callable
 */
@FunctionalInterface
public interface ThrowingCallable<V> {

  V call() throws Throwable;
}
