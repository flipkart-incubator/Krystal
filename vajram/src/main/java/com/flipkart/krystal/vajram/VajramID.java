package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import java.util.Collection;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@EqualsAndHashCode
public final class VajramID implements DataAccessSpec {

  private @MonotonicNonNull String vajramId;
  private final @MonotonicNonNull String className;

  private VajramID(String vajramId, String className) {
    this.vajramId = vajramId;
    this.className = className;
  }

  public static VajramID vajramID(String id) {
    return new VajramID(id, null);
  }

  public static VajramID fromClass(String vajramClassName) {
    return new VajramID(null, vajramClassName);
  }

  public String vajramId() {
    Optional<String> className = className();
    if (vajramId == null) {
      if (className.isPresent()) {
        try {
          //noinspection unchecked
          vajramId =
              getVajramIdString(
                      (Class<? extends Vajram>)
                          this.getClass().getClassLoader().loadClass(className.get()))
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Couldn't find vajram Id in vajram class %s"
                                  .formatted(this.className)));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      } else {
        throw new IllegalStateException("Either 'vajramId' or 'className' must be non-null");
      }
    }
    return vajramId;
  }

  @Override
  public <T> T adapt(Collection<T> dataObjects) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public String toString() {
    return "v<%s>".formatted(vajramId());
  }

  private Optional<String> className() {
    return Optional.ofNullable(className);
  }
}
