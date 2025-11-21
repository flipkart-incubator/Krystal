package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.squareup.javapoet.AnnotationSpec;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  static final AnnotationSpec IF_ABSENT_FAIL =
      AnnotationSpec.builder(IfAbsent.class)
          .addMember("value", "$T.$L", IfAbsentThen.class, "FAIL")
          .build();

  @UtilityClass
  public static class Directives {
    public static final String ENTITY = "entity";
    public static final String COMPOSED_TYPE = "composedType";
    public static final String DATA_FETCHER = "dataFetcher";
    public static final String ID_FETCHER = "idFetcher";
    public static final String INHERIT_ID_FROM_ARGS = "inferIdFromArgs";
    public static final String INHERIT_ID_FROM_PARENT = "inferIdFromParent";
  }

  @UtilityClass
  public static class DirectiveArgs {
    public static final String IN_ENTITY = "inEntity";
    public static final String SUB_PACKAGE = "subPackage";
    public static final String VAJRAM_ID = "vajramId";
    public static final String ENTITY_ID_FIELD = "entityIdField";
  }

  @UtilityClass
  public static class Facets {
    public static final String ENTITY_ID = "graphql_entityId";
    public static final String EXECUTION_CONTEXT = "graphql_executionContext";
    public static final String EXECUTION_STRATEGY = "graphql_executionStrategy";
    public static final String EXECUTION_STRATEGY_PARAMS = "graphql_executionStrategyParams";
  }
}
