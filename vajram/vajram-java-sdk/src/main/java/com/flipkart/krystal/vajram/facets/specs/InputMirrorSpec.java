package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.InputMirror;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputMirrorSpec<T, CV extends Request> implements InputMirror {
  @Getter private final DataType<T> type;
  @Getter private final Class<CV> ofVajram;
  @Getter private final int id;
  @Getter private final String name;
  @Getter private final String documentation;
  private final Function<Request, @Nullable T> getFromRequest;
  private final BiConsumer<ImmutableRequest.Builder, @Nullable T> setToRequest;

  public InputMirrorSpec(
      int id,
      String name,
      DataType<T> type,
      Class<CV> ofVajram,
      String documentation,
      Function<Request, @Nullable T> getFromRequest,
      BiConsumer<ImmutableRequest.Builder, @Nullable T> setToRequest) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.ofVajram = ofVajram;
    this.documentation = documentation;
    this.getFromRequest = getFromRequest;
    this.setToRequest = setToRequest;
  }

  @Override
  public @Nullable T getFromRequest(Request request) {
    return getFromRequest.apply(request);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setToRequest(ImmutableRequest.Builder request, @Nullable Object value) {
    setToRequest.accept(request, (T) value);
  }

  @Override
  public String toString() {
    return "InputMirror(id=" + id + ", name=" + name + ", doc=" + documentation + ')';
  }
}
