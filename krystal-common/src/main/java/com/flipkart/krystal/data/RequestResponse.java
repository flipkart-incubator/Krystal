package com.flipkart.krystal.data;

import org.checkerframework.checker.nullness.qual.NonNull;

/** Represents the results of a depdendency invocation. */
public record RequestResponse<R extends Request<T>, T>(R request, Errable<@NonNull T> response)
    implements One2OneDepResponse<R, T> {}
