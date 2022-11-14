package com.flipkart.krystal.caramel.samples.basic.classification;

import static com.flipkart.krystal.caramel.model.WorkflowMeta.workflow;
import static com.flipkart.krystal.caramel.samples.basic.stringconversion.StringConversionWorkflowBuilder.convertToString;

import com.flipkart.krystal.caramel.model.TerminatedWorkflow;
import com.flipkart.krystal.caramel.samples.basic.TransformedProduct;
import com.flipkart.krystal.caramel.samples.basic.classification.TransformedProductPayload.TransformedProductPayloadFields;
import java.util.logging.Logger;

public class ProductClassificationWorkflowBuilder {

  private static final Logger log = Logger.getLogger("");

  public static TerminatedWorkflow<TransformedProduct, TransformedProductPayload, TransformedProduct>
      classifyProduct() {
    return workflow("ProductClassificationSubWorkflow", TransformedProductPayload.class)
        .startWith(TransformedProductPayloadFields.initialTransformedProduct)
        .compute(
            TransformedProductPayloadFields.x2String,
            convertToString(),
            TransformedProductPayloadFields.initialTransformedProduct)
        .peek(
            TransformedProductPayloadFields.initialTransformedProduct,
            TransformedProductPayloadFields.x2String,
            (itp, x2) -> {
              log.info(String.valueOf(itp));
              log.info(x2);
            })
        .terminateWithOutput(TransformedProductPayloadFields.finalTransformedProduct);
  }
}
