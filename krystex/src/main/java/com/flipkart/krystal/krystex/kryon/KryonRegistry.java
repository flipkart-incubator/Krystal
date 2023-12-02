package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.model.KryonId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class KryonRegistry<T extends Kryon<? extends KryonCommand, ? extends KryonResponse>> {

  private final Map<KryonId, T> kryons = new LinkedHashMap<>();

  public T get(KryonId kryonId) {
    return tryGet(kryonId)
        .orElseThrow(
            () -> new IllegalArgumentException("No kryon with id %s found".formatted(kryonId)));
  }

  public Optional<T> tryGet(KryonId kryonId) {
    return Optional.ofNullable(kryons.get(kryonId));
  }

  public T createIfAbsent(KryonId kryonId, Function<KryonId, ? extends T> supplier) {
    return kryons.computeIfAbsent(kryonId, supplier);
  }
}
