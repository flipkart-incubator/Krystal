package com.flipkart.krystal.graphql.test;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGraphQLModel {
  protected Map<String, Object> _values = new HashMap<>();

  public <T> T get(String name) {
    //noinspection unchecked
    return (T) _values.get(name);
  }

  public void put(String name, Object value) {
    this._values.put(name, value);
  }

  public Map<String, Object> asMap() {
    return _values;
  }
}
