package com.flipkart.krystal.visualization.models;

import lombok.Builder;

@Builder
public record Link(
    String source,
    String target,
    String name,
    boolean isMandatory,
    boolean canFanout,
    String documentation) {}
