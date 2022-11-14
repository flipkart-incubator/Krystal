package com.flipkart.krystal.caramel.samples.basic.split;

import com.flipkart.krystal.caramel.model.WorkflowPayload;
import com.flipkart.krystal.caramel.samples.basic.Metric;

public interface SubMetricPayloadDefinition extends WorkflowPayload {

  Metric init();

  Metric getMetric();
}
