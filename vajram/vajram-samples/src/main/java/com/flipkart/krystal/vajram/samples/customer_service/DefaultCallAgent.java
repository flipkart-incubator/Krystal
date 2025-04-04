package com.flipkart.krystal.vajram.samples.customer_service;

import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.annos.ConformsToTrait;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.InitialCommunication;

@Vajram
@ConformsToTrait(withDef = CustomerServiceAgent.class)
public abstract class DefaultCallAgent extends ComputeVajramDef<String> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @Input AgentType agentType;
    @Mandatory @Input InitialCommunication initialCommunication;
    @Mandatory @Input String customerName;
  }

  @Output
  static String output(AgentType agentType, String customerName) {

    return "Hello "
        + customerName
        + "! I am a call Agent and I have received your call. "
        + "I will get back to you soon!";
  }
}
