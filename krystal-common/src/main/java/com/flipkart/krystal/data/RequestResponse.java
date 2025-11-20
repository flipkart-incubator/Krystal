package com.flipkart.krystal.data;

/** Represents the results of a vajram invocation. */
public record RequestResponse<R extends Request<T>, T>(R request, Errable<T> response)
    implements One2OneDepResponse<R, T> {}
