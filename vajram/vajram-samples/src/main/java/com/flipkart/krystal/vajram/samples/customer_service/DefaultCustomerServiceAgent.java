package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;

import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class DefaultCustomerServiceAgent extends ComputeVajramDef<String>
    implements CustomerServiceAgent {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfNull(FAIL)
    AgentType agentType;

    @IfNull(FAIL)
    InitialCommunication initialCommunication;

    @IfNull(FAIL)
    String customerName;
  }

  @Output
  static String output(
      AgentType agentType, InitialCommunication initialCommunication, String customerName) {

    return "Hello "
        + customerName
        + "! I am a Customer Service Agent and I have received your "
        + initialCommunication.getClass().getSimpleName()
        + ". I will get back to you soon!";
  }
}
