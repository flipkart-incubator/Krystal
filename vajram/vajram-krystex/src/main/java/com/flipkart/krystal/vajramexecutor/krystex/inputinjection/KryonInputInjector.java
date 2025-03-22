package com.flipkart.krystal.vajramexecutor.krystex.inputinjection;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KryonInputInjector implements KryonDecorator {

  public static final String DECORATOR_TYPE = KryonInputInjector.class.getName();

  @NotOnlyInitialized private final VajramKryonGraph vajramKryonGraph;

  private final @Nullable VajramInjectionProvider inputInjector;

  public KryonInputInjector(
      @UnknownInitialization VajramKryonGraph vajramKryonGraph,
      @Nullable VajramInjectionProvider inputInjector) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.inputInjector = inputInjector;
  }

  @Override
  public String decoratorType() {
    return DECORATOR_TYPE;
  }

  @Override
  public Kryon<KryonCommand, KryonCommandResponse> decorateKryon(
      KryonDecorationInput decorationInput) {
    return new InjectingDecoratedKryon(decorationInput.kryon(), vajramKryonGraph, inputInjector);
  }
}
