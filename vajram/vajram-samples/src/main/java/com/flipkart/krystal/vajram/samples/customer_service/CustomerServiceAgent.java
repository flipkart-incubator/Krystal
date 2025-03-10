package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.vajram.ComputeDelegationMode.NONE;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.traits.UseForDispatch;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;

/**
 * Sample trait demonstrating dynamic predicate dispatch capability of krystal
 *
 * <p>Takes in a customer Communication an responds to it
 */
@Trait
@CallGraphDelegationMode(NONE)
@ExternalInvocation(allow = true)
public abstract class CustomerServiceAgent implements TraitDef<String> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @UseForDispatch @Input AgentType agentType;
    @Mandatory @UseForDispatch @Input InitialCommunication initialCommunication;
    @Mandatory @UseForDispatch @Input String customerName;
  }

  public enum AgentType {
    L1,
    L2,
    L3,
  }

  public sealed interface InitialCommunication {}

  public record Email(String emailContent) implements InitialCommunication {}

  public record Call(String callRecording) implements InitialCommunication {}

  public record Ticket(String ticketSummary) implements InitialCommunication {}
}
