package com.flipkart.krystal.vajram.graphql.api;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String GRAPHQL_AGGREGATOR_SUFFIX = "_GQlAggr";
  public static final String GRAPHQL_SCHEMA_FILENAME = "Schema.graphqls";

  @UtilityClass
  public static class Directives {
    public static final String ENTITY = "entity";
    public static final String COMPOSED_TYPE = "composedType";
    public static final String DATA_FETCHER = "dataFetcher";
    public static final String ID_FETCHER = "idFetcher";
    public static final String INHERIT_ID_FROM_ARGS = "inferIdFromArgs";
    public static final String INHERIT_ID_FROM_PARENT = "inferIdFromParent";
    public static final String ROOT_PACKAGE = "rootPackage";
    public static final String SUB_PACKAGE = "subPackage";
    public static final String JAVA_TYPE = "javaType";
  }

  @UtilityClass
  public static class DirectiveArgs {
    public static final String IN_ENTITY = "inEntity";
    public static final String SUB_PACKAGE = "subPackage";
    public static final String VAJRAM_ID = "vajramId";
    public static final String ENTITY_ID_FIELD = "entityIdField";
    public static final String NAME = "name";
    public static final String PACKAGE_NAME = "packageName";
    public static final String CLASS_NAME = "className";
  }

  @UtilityClass
  public static class Facets {
    public static final String ENTITY_ID = "graphql_entityId";
    public static final String EXECUTION_CONTEXT = "graphql_executionContext";
    public static final String EXECUTION_STRATEGY = "graphql_executionStrategy";
    public static final String EXECUTION_STRATEGY_PARAMS = "graphql_executionStrategyParams";
  }
}
