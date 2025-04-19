package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;

import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class L1EmailAgent extends ComputeVajramDef<String>
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
  static String output(String customerName) {
    return "Hello "
        + customerName
        + "!"
        + """
          I am an L1 Agent and I have received your email.
          I will get back to you soon!
          """;
  }
}
