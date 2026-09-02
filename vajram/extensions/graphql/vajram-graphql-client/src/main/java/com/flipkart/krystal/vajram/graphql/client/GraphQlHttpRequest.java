package com.flipkart.krystal.vajram.graphql.client;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A <a
 * href="https://graphql.github.io/graphql-over-http/draft/#sec-Request-Parameters">GraphQL-over-HTTP</a>-compliant
 * request. {@code variables} holds the raw variables model instance untouched - the caller
 * serializes it using whatever protocol/mechanism that model itself declares, before sending the
 * HTTP request.
 */
public record GraphQlHttpRequest(
    String query,
    @Nullable String operationName,
    Object variables,
    Map<String, Object> extensions) {}
