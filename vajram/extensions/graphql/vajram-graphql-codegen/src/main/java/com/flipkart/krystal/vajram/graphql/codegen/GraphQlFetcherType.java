package com.flipkart.krystal.vajram.graphql.codegen;

public enum GraphQlFetcherType {
  UNKNOWN_FETCHER_TYPE,
  SINGLE_FIELD_DATA_FETCHER,
  MULTI_FIELD_DATA_FETCHER,
  ID_FETCHER,
  INHERIT_ID_FROM_ARGS,
  INHERIT_ID_FROM_PARENT,
  TYPE_AGGREGATOR,
}
