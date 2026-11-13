package com.flipkart.krystal.vajram.graphql.codegen;

import com.palantir.javapoet.ClassName;

public record VajramFetcher(ClassName vajramClassName, GraphQlFetcherType type)
    implements Fetcher {}
