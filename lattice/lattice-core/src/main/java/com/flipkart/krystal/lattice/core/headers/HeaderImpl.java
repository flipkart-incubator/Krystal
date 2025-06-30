package com.flipkart.krystal.lattice.core.headers;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
sealed class HeaderImpl implements Header permits SingleValueHeader {
  String name;
  ImmutableList<String> values;

  HeaderImpl(String name, List<String> values) {
    this.name = name;
    this.values = ImmutableList.copyOf(values);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof Header that)) return false;
    return Objects.equals(name, that.name()) && Objects.equals(values, that.values());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), values());
  }
}
