package com.flipkart.krystal.vajram.graphql.api.execution;

import graphql.language.OperationDefinition.Operation;
import java.util.Map;

public record GraphQLQuery(Operation operation, String query, Map<String, Object> variables) {}
