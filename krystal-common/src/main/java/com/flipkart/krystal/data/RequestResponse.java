package com.flipkart.krystal.data;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents the results of a dependency invocation. */
public record RequestResponse<R extends Request<@Nullable T>, T>(R request, Errable<T> response)
    implements One2OneDepResponse<R, T> {}
