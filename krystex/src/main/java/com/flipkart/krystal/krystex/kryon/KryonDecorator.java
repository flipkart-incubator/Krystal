package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.KryonCommand;

@FunctionalInterface
public interface KryonDecorator {
  Kryon<KryonCommand, KryonResponse> decorateKryon(
      Kryon<KryonCommand, KryonResponse> kryon, KryonExecutor kryonExecutor);
}
