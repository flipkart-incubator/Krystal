package com.flipkart.krystal.data;

/** Represents the results of a depdendency invocation. */
public record Response<R extends Request<T>, T>(R request, Errable<T> response) {}
