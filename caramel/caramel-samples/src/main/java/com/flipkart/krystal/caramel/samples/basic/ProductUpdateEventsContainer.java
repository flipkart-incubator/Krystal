package com.flipkart.krystal.caramel.samples.basic;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ProductUpdateEventsContainer {
  @Getter private final ImmutableList<ProductUpdateEvent> productUpdateEvents;
}
