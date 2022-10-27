package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

public interface StringMetricPayloadDefinition extends WorkflowPayload {
  String initString();

  Metric metric();
}
