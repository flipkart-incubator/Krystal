package com.flipkart.krystal.vajram.facets.specs;

import static com.flipkart.krystal.facets.FacetUtils.computePlatformDefaultValue;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.data.IfNull.IfNullThen;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetUtils;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.Callables;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputMirrorSpec<T, CV extends Request> implements InputMirror {

  @Getter private final VajramID ofVajramID;
  @Getter private final DataType<T> type;
  @Getter private final Class<CV> ofVajram;
  @Getter private final int id;
  @Getter private final String name;
  @Getter private final String documentation;
  private @MonotonicNonNull ElementTags tags;
  private final Function<Request, @Nullable T> getFromRequest;
  private final BiConsumer<ImmutableRequest.Builder, @Nullable T> setToRequest;
  private final Callable<ElementTags> tagsParser;
  private @MonotonicNonNull T platformDefaultValue;

  public InputMirrorSpec(
      int id,
      String name,
      VajramID ofVajramID,
      DataType<T> type,
      Class<CV> ofVajram,
      String documentation,
      Callable<ElementTags> tagsParser,
      Function<Request, @Nullable T> getFromRequest,
      BiConsumer<ImmutableRequest.Builder, @Nullable T> setToRequest) {
    this.id = id;
    this.name = name;
    this.ofVajramID = ofVajramID;
    this.type = type;
    this.ofVajram = ofVajram;
    this.documentation = documentation;
    this.tagsParser = tagsParser;
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

  @SneakyThrows
  @Override
  public ElementTags tags() {
    if (tags == null) {
      try {
        tags = tagsParser.call();
      } catch (Exception e) {
        tags = emptyTags();
      }
    }
    return tags;
  }

  @SuppressWarnings("unchecked")
  public @NonNull T getPlatformDefaultValue() throws UnsupportedOperationException {
    if (platformDefaultValue == null) {
      platformDefaultValue = computePlatformDefaultValue(this, type);
    }
    return platformDefaultValue;
  }
}
