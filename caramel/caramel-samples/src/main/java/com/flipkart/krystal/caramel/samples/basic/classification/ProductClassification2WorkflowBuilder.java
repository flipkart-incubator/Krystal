package com.flipkart.krystal.caramel.samples.basic.classification;

import static com.flipkart.krystal.caramel.model.WorkflowMeta.workflow;
import static java.util.function.Function.identity;

import com.flipkart.krystal.caramel.model.TerminatedWorkflow;
import com.flipkart.krystal.caramel.samples.basic.TransformedProduct;
import com.flipkart.krystal.caramel.samples.basic.classification.TransformedProductPayload.TransformedProductPayloadFields;
import java.util.logging.Logger;

public class ProductClassification2WorkflowBuilder {
  private static final Logger log = Logger.getLogger("");

  public static TerminatedWorkflow<TransformedProduct, TransformedProductPayload, String>
      classifyProduct2() {
    return workflow("ProductClassificationSubWorkflow2", TransformedProductPayload.class)
        .startWith(TransformedProductPayloadFields.initialTransformedProduct)
        .compute(
            TransformedProductPayloadFields.x2String,
            Object::toString,
            TransformedProductPayloadFields.initialTransformedProduct)
        .peek(
            TransformedProductPayloadFields.initialTransformedProduct,
            TransformedProductPayloadFields.x2String,
            (initialTransformedProduct, x2String) -> log.info(initialTransformedProduct + x2String))
        .compute(
            TransformedProductPayloadFields.finalTransformedProduct,
            identity(),
            TransformedProductPayloadFields.initialTransformedProduct)
        .terminateWithOutput(TransformedProductPayloadFields.x2String);
  }
}
