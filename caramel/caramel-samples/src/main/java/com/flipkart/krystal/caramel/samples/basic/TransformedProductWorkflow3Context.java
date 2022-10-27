package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

public interface TransformedProductWorkflow3Context extends WorkflowPayload {
  void setString(String s);

  void set2ndString(String s);

  void setInitProduct(TransformedProduct transformedProduct);

  void setNextProduct(TransformedProduct transformedProduct);
}
