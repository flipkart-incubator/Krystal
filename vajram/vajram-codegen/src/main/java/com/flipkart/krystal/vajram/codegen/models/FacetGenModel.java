package com.flipkart.krystal.vajram.codegen.models;

public sealed interface FacetGenModel permits InputModel, DependencyModel {
  String name();

  boolean isMandatory();
}
