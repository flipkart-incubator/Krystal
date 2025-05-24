package com.flipkart.krystal.lattice.core.headers;

import org.checkerframework.checker.nullness.qual.Nullable;

public record SimpleHeader(String key, String value) implements Header {}
