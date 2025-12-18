package com.flipkart.krystal.vajram.inputinjection;

import jakarta.inject.Provider;

/**
 * Note:
 *
 * <ul>
 *   <li>Instances of this class are designed to be disposable and cannot be reused more than once.
 *       Doing so may cause unpredictable behaviour.
 * </ul>
 *
 * @param <T> The type of the object being provided for injection
 */
public interface CloseableProvider<T> extends Provider<T>, AutoCloseable {

  /**
   * Returns the object for injection. This can be called exactly once per instance of this class.
   */
  @Override
  T get();

  /**
   * Closes the provider and destroys any temporary instance created for this injection (for
   * example, as specified by the jakarta CDI spec). Not all dependency injection frameworks need
   * this.
   */
  @Override
  void close();
}
