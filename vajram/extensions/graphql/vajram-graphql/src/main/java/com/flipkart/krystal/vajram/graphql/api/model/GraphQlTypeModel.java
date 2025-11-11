package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.json.Json;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
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

  @Nullable List<GraphQLError> _errors();

  @Nullable Map<String, Object> _extensions();

  @Override
  default byte[] _serialize() throws Exception {
    Map<String, @Nullable Object> map = new HashMap<>(3);
    map.put("data", _data());
    map.put("errors", _errors());
    map.put("extensions", _extensions());
    return Json.OBJECT_WRITER.writeValueAsBytes(map);
  }
}
