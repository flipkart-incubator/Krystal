package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.graphql.api.DefaultErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.ErrorCollector;
import com.flipkart.krystal.vajram.json.Json;
import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelClusterRoot(
    immutableRoot = GraphQlTypeModel_Immut.class,
    builderRoot = GraphQlTypeModel_Immut.Builder.class)
public interface GraphQlOpTypeModel extends GraphQlTypeModel {

  @Nullable Map<Object, Object> _extensions();

  @Nullable
  default List<GraphQLError> _errors() {
    ErrorCollector errorCollector = new DefaultErrorCollector();
    _collectErrors(errorCollector, new ArrayList<>());
    return errorCollector.getErrors();
  }

  @Override
  default byte[] _serialize() throws Exception {
    Map<String, @Nullable Object> map = new HashMap<>(3);
    map.put("data", _data());
    map.put("errors", _errors());
    map.put("extensions", _extensions());
    return Json.OBJECT_WRITER.writeValueAsBytes(map);
  }
}
