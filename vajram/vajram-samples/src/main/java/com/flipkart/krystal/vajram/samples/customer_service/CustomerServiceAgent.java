package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.ComputeDelegationMode.NONE;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.traits.UseForDispatch;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.facets.Input;

/**
 * Sample trait demonstrating dynamic predicate dispatch capability of krystal
 *
 * <p>Takes in a customer Communication an responds to it
 */
@Trait
@CallGraphDelegationMode(NONE)
@ExternallyInvocable
public abstract class CustomerServiceAgent implements TraitDef<String> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfNull(FAIL)
    @UseForDispatch
    AgentType agentType;

    @IfNull(FAIL)
    @UseForDispatch
    InitialCommunication initialCommunication;

    @IfNull(FAIL)
    @UseForDispatch
    String customerName;
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
