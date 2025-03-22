package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class KryonRegistry<T extends Kryon<? extends KryonCommand, ? extends KryonCommandResponse>> {

  private final Map<VajramID, T> kryons = new LinkedHashMap<>();

  public T get(VajramID vajramID) {
    return tryGet(vajramID)
        .orElseThrow(
            () -> new IllegalArgumentException("No kryon with id %s found".formatted(vajramID)));
  }

  public Optional<T> tryGet(VajramID vajramID) {
    return Optional.ofNullable(kryons.get(vajramID));
  }

  public T createIfAbsent(VajramID vajramID, Function<VajramID, ? extends T> supplier) {
    return kryons.computeIfAbsent(vajramID, supplier);
  }
}
