package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class KryonRegistry<T extends Kryon<? extends KryonCommand, ? extends KryonCommandResponse>> {

  private final Map<String, T> kryons = new HashMap<>();

  public T get(VajramID vajramID) {
    return tryGet(vajramID)
        .orElseThrow(
            () -> new IllegalArgumentException("No kryon with id %s found".formatted(vajramID)));
  }

  public Optional<T> tryGet(VajramID vajramID) {
    return Optional.ofNullable(kryons.get(vajramID.id()));
  }

  public T createIfAbsent(VajramID vajramID, Function<String, ? extends T> supplier) {
    return kryons.computeIfAbsent(vajramID.id(), supplier);
  }
}
