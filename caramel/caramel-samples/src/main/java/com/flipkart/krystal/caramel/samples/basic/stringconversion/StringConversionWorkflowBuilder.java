package com.flipkart.krystal.caramel.samples.basic.stringconversion;

import static com.flipkart.krystal.caramel.model.WorkflowMeta.workflow;
import static com.flipkart.krystal.caramel.samples.basic.stringconversion.StringConversionPayload.StringConversionFields.input;
import static com.flipkart.krystal.caramel.samples.basic.stringconversion.StringConversionPayload.StringConversionFields.output;

import com.flipkart.krystal.caramel.model.TerminatedWorkflow;

public class StringConversionWorkflowBuilder {

  public static <I> TerminatedWorkflow<I, StringConversionPayload, String> convertToString() {
    //noinspection unchecked
    return (TerminatedWorkflow<I, StringConversionPayload, String>)
        workflow("stringConversionWorkflow", StringConversionPayload.class)
            .startWith(input)
            .compute(output, String::valueOf, input)
            .terminateWithOutput(output);
  }
}
