package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KryonInputInjector implements KryonDecorator {

  public static final String DECORATOR_TYPE = KryonInputInjector.class.getName();

  @NotOnlyInitialized private final VajramKryonGraph vajramKryonGraph;

  private final @Nullable InputInjectionProvider inputInjectionProvider;

  private final Map<Kryon<KryonCommand, KryonResponse>, InjectingDecoratedKryon> decoratedKryons =
      new LinkedHashMap<>();

  public KryonInputInjector(
      @UnknownInitialization VajramKryonGraph vajramKryonGraph,
      @Nullable InputInjectionProvider inputInjectionProvider) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.inputInjectionProvider = inputInjectionProvider;
  }

  @Override
  public String decoratorType() {
    return DECORATOR_TYPE;
  }

  @Override
  public Kryon<KryonCommand, KryonResponse> decorateKryon(KryonDecorationInput decorationInput) {
    return new InjectingDecoratedKryon(
        decorationInput.kryon(), vajramKryonGraph, inputInjectionProvider);
  }
}
