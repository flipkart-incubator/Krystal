package com.flipkart.krystal.krystex.inputinjection;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link KryonDecorator} which populates a vajram's {@code INJECTION}-typed facets using a
 * natively-typed {@link Supplier} (typically backed by a compile-time DI framework such as Dagger
 * or Guice) instead of the reflection-based, per-facet lookup done by {@link KryonInputInjector}.
 */
public final class NativeVajramInjector implements KryonDecorator {

  public static final String DECORATOR_TYPE = NativeVajramInjector.class.getName();

  private final @Nullable Function<VajramID, Supplier<FacetValuesBuilder>>
      injectedFacetsSupplierProvider;

  public NativeVajramInjector(
      @Nullable Function<VajramID, Supplier<FacetValuesBuilder>> injectedFacetsSupplierProvider) {
    this.injectedFacetsSupplierProvider = injectedFacetsSupplierProvider;
  }

  @Override
  public String decoratorType() {
    return DECORATOR_TYPE;
  }

  @Override
  public Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> decorateKryon(
      KryonDecorationInput decorationInput) {
    return new NativeInjectingDecoratedKryon(
        decorationInput.kryon(), injectedFacetsSupplierProvider);
  }
}
