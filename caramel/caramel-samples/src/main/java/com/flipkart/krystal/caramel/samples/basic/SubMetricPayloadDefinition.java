package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

public interface SubMetricPayloadDefinition extends WorkflowPayload {

  Metric init();

  Metric getMetric();
}
