package com.flipkart.krystal.data;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DividerRequest {
  public interface Option1 {
    int numerator();

    Optional<Integer> denominator();
  }

  public interface Option2 {

    Optional<Integer> numerator();

    Optional<Integer> denominator();
  }

  public interface Option3 {
    int numerator();

    int denominator_OrDefault();
  }

  public interface Option4 {
    @Nullable Integer numerator();

    @Nullable Integer denominator();
  }

  public interface Option5 {
    int numerator();

    @Nullable Integer denominator();
  }
}
