package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.graphql.api.DefaultErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.json.Json;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelClusterRoot(
    immutableRoot = GraphQlTypeModel_Immut.class,
    builderRoot = GraphQlTypeModel_Immut.Builder.class)
public interface GraphQlTypeModel extends Model, SerializableGQlResponseJsonModel {
  /**
   * Returns the __typename of a graphql type according to the graphql spec or null if it has yet
   * been computed/set.
   */
  @Nullable String __typename();

  ExecutionContext graphql_executionContext();

  VajramExecutionStrategy graphql_executionStrategy();

  ExecutionStrategyParameters graphql_executionStrategyParams();

  @Nullable Map<String, Object> _data();

  /**
   * Collects errors from this model and all nested models using the visitor pattern.
   *
   * @param errorCollector The collector to accumulate errors
   * @param path The current path in the GraphQL response tree
   */
  void _collectErrors(ErrorCollector errorCollector, List<Object> path);

  @Nullable Map<String, Object> _extensions();

  @Override
  default byte[] _serialize() throws Exception {
    Map<String, Object> map = new HashMap<>(3);
    map.put("data", _data());
    
    // Collect errors using the ErrorCollector pattern
    ErrorCollector errorCollector = new DefaultErrorCollector();
    _collectErrors(errorCollector, new ArrayList<>());
    List<GraphQLError> errors = errorCollector.getErrors();
    if (!errors.isEmpty()) {
      map.put("errors", errors);
    }
    
    map.put("extensions", _extensions());
    return Json.OBJECT_WRITER.writeValueAsBytes(map);
  }
}
