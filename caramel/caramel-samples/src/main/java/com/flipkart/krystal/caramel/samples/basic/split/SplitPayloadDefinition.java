package com.flipkart.krystal.caramel.samples.basic.split;

import com.flipkart.krystal.caramel.model.WorkflowPayload;
import com.flipkart.krystal.caramel.samples.basic.Metric;
import com.flipkart.krystal.caramel.samples.basic.ProductUpdateEvent;
import java.util.Collection;

interface SplitPayloadDefinition extends WorkflowPayload {
  String initString();

  ProductUpdateEvent initProductEvent();

  Collection<Metric> metrics();

  Metric metric();
}
