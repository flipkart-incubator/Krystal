package com.flipkart.krystal.vajram.graphql.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphQLSchemaRegistry {

  private final Map<String, Field> fieldPathToDefinition;
  private final Map<String, Type> typeNameToType;
  private static volatile GraphQLSchemaRegistry INSTANCE = null;

  private GraphQLSchemaRegistry() {
    this.fieldPathToDefinition = new HashMap<>();
    this.typeNameToType = new HashMap<>();
  }

  public static GraphQLSchemaRegistry getInstance() {
    if (INSTANCE == null) {
      synchronized (GraphQLSchemaRegistry.class) {
        if (INSTANCE == null) {
          INSTANCE = new GraphQLSchemaRegistry();
        }
        return INSTANCE;
      }
    }
    return INSTANCE;
  }

  public Field getFieldDefinitionByPath(String fieldPath) {
    return fieldPathToDefinition.get(fieldPath);
  }

  public void addFieldDefinition(String fieldPath, Field definition) {
    this.fieldPathToDefinition.put(fieldPath, definition);
  }

  public Type getTypeDefinitionByName(String typeName) {
    typeNameToType.putIfAbsent(typeName, new Type(typeName, new ArrayList<>()));
    return typeNameToType.get(typeName);
  }
}
