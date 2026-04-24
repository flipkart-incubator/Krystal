package com.flipkart.krystal.visualization.models;

import lombok.Builder;

@Builder
public record Input(String name, String type, boolean isMandatory, String documentation) {}
