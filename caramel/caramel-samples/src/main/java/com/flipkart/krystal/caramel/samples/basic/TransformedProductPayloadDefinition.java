package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

public interface TransformedProductPayloadDefinition extends WorkflowPayload {
  TransformedProduct getInitialTransformedProduct();

  String getX2String();

  TransformedProduct getFinalTransformedProduct();
}
