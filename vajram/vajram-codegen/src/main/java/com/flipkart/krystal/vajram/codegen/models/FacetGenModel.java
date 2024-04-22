package com.flipkart.krystal.vajram.codegen.models;

import javax.lang.model.element.VariableElement;

public sealed interface FacetGenModel permits InputModel, DependencyModel {
  int id();

  String name();

  boolean isMandatory();

  VariableElement facetField();
}
