package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.CaramelPayload;
import com.flipkart.krystal.caramel.model.ImplAs;
import com.flipkart.krystal.caramel.model.WorkflowPayload;
import java.util.Collection;
import java.util.List;

@ImplAs("TestPayload")
@CaramelPayload
interface TestPayloadDefinition extends WorkflowPayload {

  ProductUpdateEventsContainer productUpdateEvents();

  String x1String();

  TransformedProduct initialTransformedProduct();

  ProductUpdateEvent initProductEvent();

  List<TransformedProduct> conditionalTransformedProducts();

  String triggerUserId();

  Collection<Metric> metrics();

  Collection<String> metricNames();

  boolean isEnableValidation();

  String string();

  String secondString();

  TransformedProduct nextProduct();
}
