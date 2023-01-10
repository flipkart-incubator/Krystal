package com.flipkart.krystal.graphql.test.datafetchers;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.graphql.test.AbstractGraphQLModel;
import com.flipkart.krystal.graphql.test.productpage.ProductPageRequestContext;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DataFetcherUtil {

  public static Object getField(DataFetchingEnvironment dataFetchingEnvironment) {
    AbstractGraphQLModel source = dataFetchingEnvironment.getSource();
    Field field = dataFetchingEnvironment.getField();
    GraphQLType parentType = dataFetchingEnvironment.getParentType();
    String fieldName = field.getName();
    if (source != null) {
      Object currentValue = source.get(fieldName);
      if (currentValue != null) {
        return currentValue;
      }
    }
    Optional<GraphQLAppliedDirective> dataFetcher =
        parentType.getChildren().stream()
            .filter(graphQLSchemaElement -> graphQLSchemaElement instanceof GraphQLFieldDefinition)
            .map(graphQLSchemaElement -> (GraphQLFieldDefinition) graphQLSchemaElement)
            .filter(graphQLFieldDefinition -> fieldName.equals(graphQLFieldDefinition.getName()))
            .map(GraphQLFieldDefinition::getAppliedDirectives)
            .flatMap(Collection::stream)
            .filter(directive -> "DataFetcher".equals(directive.getName()))
            .findAny();
    if (dataFetcher.isPresent()) {
      String type = dataFetcher.get().getArgument("type").getValue();
      String id = dataFetcher.get().getArgument("id").getValue();
      long timeoutMillis =
          dataFetchingEnvironment.getGraphQlContext().getOrDefault("timeout_millis", 10000);
      ApplicationRequestContext applicationRequestContext =
          dataFetchingEnvironment.getGraphQlContext().get("applicationRequestContext");
      if ("vajram".equals(type)) {
        VajramExecutor<ProductPageRequestContext> vajramExecutor =
            dataFetchingEnvironment.getGraphQlContext().get("vajram_executor");
        return vajramExecutor
            .execute(
                vajramID(id),
                rc -> MapperUtil.mapRequest(rc, source, fieldName),
                applicationRequestContext.requestId() + "." + fieldName)
            .thenApply(o -> MapperUtil.mapResponse(source, fieldName, o))
            .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
      } else if ("requestContext".equals(type)) {
        return MapperUtil.mapStatic(applicationRequestContext, source, fieldName);
      }
    }
    throw new UnsupportedOperationException();
  }
}
