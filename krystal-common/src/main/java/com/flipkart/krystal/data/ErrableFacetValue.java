package com.flipkart.krystal.data;

import com.flipkart.krystal.data.FacetValue.SingleFacetValue;

public record ErrableFacetValue<T>(Errable<T> errable) implements SingleFacetValue<T> {

  private static final ErrableFacetValue<?> NIL = new ErrableFacetValue<>(Errable.nil());

  @SuppressWarnings("unchecked")
  public static <T> ErrableFacetValue<T> nil() {
    return (ErrableFacetValue<T>) NIL;
  }

  @Override
  public Errable<T> asErrable() {
    return errable;
  }
}
