package com.flipkart.krystal.vajram.graphql.codegen;

import com.squareup.javapoet.ClassName;

public record VajramFetcher(ClassName vajramClassName, GraphQlFetcherType type)
    implements Fetcher {}
