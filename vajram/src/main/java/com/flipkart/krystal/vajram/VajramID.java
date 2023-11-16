package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.ObjectType;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import java.util.Collection;
import java.util.Optional;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
public final class VajramID implements DataAccessSpec {

  private @MonotonicNonNull String vajramId;
  private final @MonotonicNonNull String vajramClassName;
  private final DataType<?> responseType;

  private VajramID(
      @Nullable String vajramId, @Nullable String vajramClassName, DataType<?> responseType) {
    this.vajramId = vajramId;
    this.vajramClassName = vajramClassName;
    this.responseType = responseType;
  }

  public static VajramID vajramID(String id) {
    return new VajramID(id, null, ObjectType.object());
  }

  public static VajramID vajramID(String id, DataType<?> responseType) {
    return new VajramID(id, null, responseType);
  }

  public static VajramID fromClass(String vajramClassName, DataType<?> responseType) {
    return new VajramID(null, vajramClassName, responseType);
  }

  public String vajramId() {
    if (vajramId == null) {
      Optional<String> className = className();
      if (className.isPresent()) {
        try {
          //noinspection unchecked
          vajramId =
              getVajramIdString(
                      (Class<? extends Vajram<?>>)
                          Optional.ofNullable(this.getClass().getClassLoader())
                              .orElseThrow()
                              .loadClass(className.get()))
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Couldn't find vajram Id in vajram class %s"
                                  .formatted(className.get())));
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
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VajramID)) {
      return false;
    }
    return this.vajramId().equals(((VajramID) obj).vajramId());
  }

  @Override
  public int hashCode() {
    return vajramId().hashCode();
  }

  @Override
  public <T> T adapt(Collection<T> dataObjects) {
    throw new UnsupportedOperationException("");
  }

  public Optional<String> className() {
    return Optional.ofNullable(vajramClassName);
  }

  public DataType<?> responseType() {
    return responseType;
  }
}
