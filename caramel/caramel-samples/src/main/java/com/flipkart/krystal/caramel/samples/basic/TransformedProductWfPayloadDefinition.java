package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

public interface TransformedProductWfPayloadDefinition extends WorkflowPayload {
  ProductUpdateEvent productUpdateEvent();

  TransformedProduct transformedProduct();
}
