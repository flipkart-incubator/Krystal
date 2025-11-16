package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.json.Json;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelClusterRoot(
    immutableRoot = GraphQlTypeModel_Immut.class,
    builderRoot = GraphQlTypeModel_Immut.Builder.class)
public interface GraphQlOpTypeModel extends GraphQlTypeModel {

  @Nullable Map<String, Object> _data();

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
