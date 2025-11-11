package com.flipkart.krystal.vajram.graphql.api;

import java.util.List;

public record Type(String typeName, List<Field> fields) {}
