package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An adapter interface which allows Krystal to support Dependency Injection. Clients can implement
 * this interface and delegate the injection call to the Dependency injection framework they are
 * using.
 */
@FunctionalInterface
public interface VajramInjectionProvider {

  /**
   * If there is any error in injecting the value, implementors are expected to return an {@link
   * Errable} with an error created using {@link Errable#withError(Throwable)}, for example.
   *
   * <p>If the DI framework supports optional injection and there is not binding for this,
   * implementors are expected to return an {@code nil}/empty {@link Errable} created using {@link
   * Errable#nil()}, for example.
   *
   * @param vajramID The id of the vajram for which injection is being requested
   * @param facetDef The definition of the input for which the injection is being requested
   * @return a success {@link Errable} containing the value provided by a Dependency Injection
   *     framework, or a failure Errable containing the error encountered why injection, or a nil
   *     Errable if the binding is optional.
   */
  <T> Errable<@NonNull T> get(VajramID vajramID, FacetSpec<T, ?> facetDef) throws Exception;
}
