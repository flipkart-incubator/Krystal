package com.flipkart.krystal.caramel.samples.basic.stringconversion;

import com.flipkart.krystal.caramel.model.WorkflowPayload;

interface StringConversionPayloadDefinition extends WorkflowPayload {
  Object input();

  String output();
}
