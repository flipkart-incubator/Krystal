package com.flipkart.krystal.vajram.graphql.codegen;

import com.squareup.javapoet.ClassName;

public record Fetcher(ClassName className, GraphQlFetcherType type) {}
