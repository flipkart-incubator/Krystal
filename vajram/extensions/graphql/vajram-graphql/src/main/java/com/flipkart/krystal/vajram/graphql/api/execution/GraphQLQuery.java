package com.flipkart.krystal.vajram.graphql.api.execution;

import java.util.Map;

public record GraphQLQuery(String query, Map<String, Object> variables) {}
