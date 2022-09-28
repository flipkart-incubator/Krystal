package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

public interface TransformedProductWorkflow2Context extends WorkflowPayload {
  void setInitProductEvent(TransformedProduct transformedProduct);

  void setString(String s);

  String getString();

  void set2ndString(String s);

  void setFinalTransformedProduct(TransformedProduct transformedProduct);

  TransformedProduct getFinalTransformedProduct();
}
