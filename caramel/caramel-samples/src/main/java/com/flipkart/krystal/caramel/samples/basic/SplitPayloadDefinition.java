package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;
import java.util.Collection;

interface SplitPayloadDefinition extends WorkflowPayload {
  String initString();

  ProductUpdateEvent initProductEvent();

  Collection<Metric> metrics();

  Metric metric();
}
