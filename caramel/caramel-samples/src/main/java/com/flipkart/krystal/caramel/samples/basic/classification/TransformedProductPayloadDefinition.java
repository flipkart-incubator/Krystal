package com.flipkart.krystal.caramel.samples.basic.classification;

import com.flipkart.krystal.caramel.model.WorkflowPayload;
import com.flipkart.krystal.caramel.samples.basic.TransformedProduct;

public interface TransformedProductPayloadDefinition extends WorkflowPayload {
  TransformedProduct getInitialTransformedProduct();

  String getX2String();

  TransformedProduct getFinalTransformedProduct();
}
